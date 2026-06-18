package com.example.fitnesstracker.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.ActivityDao
import com.example.fitnesstracker.data.ActivityPoint
import com.example.fitnesstracker.data.ActivityRecord
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.service.TrackingService
import com.example.fitnesstracker.service.TrackingState
import com.example.fitnesstracker.util.InputValidation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityDao: ActivityDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) : ViewModel() {

    // Connect directly to the service's companion StateFlow
    val trackingState: StateFlow<TrackingState> = TrackingService.trackingState

    // Live list of all activities in history
    val allActivities: Flow<List<ActivityRecord>> = activityDao.getAllActivities()

    // Active Profile Flow (if null, auto insert a default profile)
    val userProfile: StateFlow<UserProfile> = activityDao.getUserProfile()
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // UI State for Cloud Syncing
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    init {
        // Initialize default profile if empty
        viewModelScope.launch {
            val current = activityDao.getUserProfileSync()
            if (current == null) {
                activityDao.insertUserProfile(UserProfile())
            }
        }
    }

    // --- Service Control Actions ---
    fun startActivity(activityType: String) {
        val intent = Intent(appContext, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_ACTIVITY_TYPE, activityType)
        }
        appContext.startForegroundService(intent)
    }

    fun pauseActivity() {
        val intent = Intent(appContext, TrackingService::class.java).apply {
            action = TrackingService.ACTION_PAUSE
        }
        appContext.startService(intent)
    }

    fun resumeActivity() {
        val intent = Intent(appContext, TrackingService::class.java).apply {
            action = TrackingService.ACTION_RESUME
        }
        appContext.startService(intent)
    }

    fun stopActivity() {
        val intent = Intent(appContext, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    // --- Database Operations ---
    fun saveCompletedActivity(notes: String, onSuccess: (Long) -> Unit) {
        val state = trackingState.value
        val points = state.routePoints

        if (state.elapsedSeconds <= 0) {
            // Discarding if empty session
            TrackingService.resetState()
            return
        }

        viewModelScope.launch {
            // Calculate final statistics
            val avgSpeed = if (state.elapsedSeconds > 0) state.distanceMeters / state.elapsedSeconds else 0.0
            val avgPace = if (state.distanceMeters > 0) state.elapsedSeconds / (state.distanceMeters / 1000.0) else 0.0

            val record = ActivityRecord(
                activityType = state.activityType,
                startTime = state.startTime,
                endTime = System.currentTimeMillis(),
                durationSeconds = state.elapsedSeconds,
                distanceMeters = state.distanceMeters,
                calories = state.calories,
                avgSpeed = avgSpeed,
                avgPace = avgPace,
                notes = InputValidation.sanitizeNotes(notes, 500),
                isSynced = false
            )

            val dbPoints = state.routePoints.map {
                ActivityPoint(
                    activityId = 0L, // assigned inside the transaction
                    latitude = it.latitude,
                    longitude = it.longitude,
                    timestamp = it.timestamp
                )
            }

            // Atomic: both inserts succeed or both fail — no orphaned activities
            val activityId = activityDao.saveActivityWithPoints(record, dbPoints)

            // Stop service and reset state AFTER DB write succeeds
            stopActivity()
            TrackingService.resetState()

            onSuccess(activityId)
        }
    }

    fun discardActivity() {
        stopActivity()
        TrackingService.resetState()
    }

    fun deleteActivity(activity: ActivityRecord) {
        viewModelScope.launch {
            activityDao.deleteActivity(activity)
        }
    }

    fun updateActivityNotes(activityId: Long, notes: String) {
        viewModelScope.launch {
            activityDao.updateActivityNotes(activityId, InputValidation.sanitizeNotes(notes, 500))
        }
    }

    fun getPointsForActivity(activityId: Long): Flow<List<ActivityPoint>> {
        return activityDao.getPointsForActivity(activityId)
    }

    suspend fun getActivityById(activityId: Long): ActivityRecord? {
        return activityDao.getActivityById(activityId)
    }

    // --- Profile Management ---
    fun updateUserProfile(
        name: String,
        age: Int,
        weightKg: Double,
        heightCm: Double,
        preferredUnits: String
    ) {
        viewModelScope.launch {
            val current = activityDao.getUserProfileSync() ?: UserProfile()
            val sanitizedName = InputValidation.sanitizeName(name, 100)
            val updated = current.copy(
                name = if (sanitizedName.isEmpty()) "Athlete" else sanitizedName,
                age = age.coerceIn(5, 120),
                weightKg = weightKg.coerceIn(20.0, 300.0),
                heightCm = heightCm.coerceIn(50.0, 250.0),
                preferredUnits = preferredUnits.trim().ifEmpty { "Metric" }
            )
            activityDao.insertUserProfile(updated)
        }
    }

    // --- Cloud Sync (Health Connect) ---
    // Syncs unsynced activities and latest weight to Health Connect.
    // No fake delays — shows honest status without simulating a cloud call.
    fun triggerCloudSync() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Syncing with Health Connect..."

            if (!isOnline(appContext)) {
                _syncMessage.value = "Offline. Sync postponed."
                kotlinx.coroutines.delay(1500)
                _isSyncing.value = false
                _syncMessage.value = ""
                return@launch
            }

            try {
                val count = com.example.fitnesstracker.util.HealthConnectManager.syncAll(appContext)
                _syncMessage.value = if (count > 0) {
                    "Synced $count activit${if (count == 1) "y" else "ies"} to Health Connect."
                } else {
                    "Already up to date."
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityViewModel", "Health Connect sync failed", e)
                _syncMessage.value = "Sync failed. Check Health Connect permissions."
            }

            kotlinx.coroutines.delay(2000)
            _isSyncing.value = false
            _syncMessage.value = ""
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        )
    }

    // --- Analytics Queries (SQL-level, no in-memory filtering) ---

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val sub = when (dow) { Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2; Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5; else -> 6 }
        cal.add(Calendar.DAY_OF_YEAR, -sub)
        return cal.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Reactive start-of-week timestamp that updates when the week rolls over
    private val weekStartFlow: Flow<Long> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(getStartOfWeek())
            // Re-emit every 60 seconds so the week boundary is detected promptly
            kotlinx.coroutines.delay(60_000)
        }
    }.distinctUntilChanged()

    private val monthStartFlow: Flow<Long> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(getStartOfMonth())
            kotlinx.coroutines.delay(60_000)
        }
    }.distinctUntilChanged()

    private val dayStartFlow: Flow<Long> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(getStartOfDay())
            kotlinx.coroutines.delay(60_000)
        }
    }.distinctUntilChanged()

    // Weekly Summary — SQL aggregates
    val weeklyDistanceKm: Flow<Double> = weekStartFlow.flatMapLatest { since ->
        activityDao.getTotalDistanceMetersSince(since).map { it / 1000.0 }
    }

    val weeklyWorkoutsCount: Flow<Int> = weekStartFlow.flatMapLatest { since ->
        activityDao.getActivityCountSince(since)
    }

    val weeklyDurationHours: Flow<Double> = weekStartFlow.flatMapLatest { since ->
        activityDao.getTotalDurationSecondsSince(since).map { it / 3600.0 }
    }

    val weeklyCaloriesBurned: Flow<Double> = weekStartFlow.flatMapLatest { since ->
        activityDao.getTotalCaloriesSince(since)
    }

    // Monthly Summary — SQL aggregates
    val monthlyDistanceKm: Flow<Double> = monthStartFlow.flatMapLatest { since ->
        activityDao.getTotalDistanceMetersSince(since).map { it / 1000.0 }
    }

    val monthlyActiveDaysCount: Flow<Int> = monthStartFlow.flatMapLatest { since ->
        activityDao.getActiveDayCountSince(since)
    }

    val monthlyCaloriesBurned: Flow<Double> = monthStartFlow.flatMapLatest { since ->
        activityDao.getTotalCaloriesSince(since)
    }

    // Today's calories burned (for net calorie integration on Nutrition screen)
    val todayCaloriesBurned: Flow<Double> = dayStartFlow.flatMapLatest { startOfDay ->
        activityDao.getTodayCaloriesBurned(startOfDay)
    }
}

class ActivityViewModelFactory(
    private val activityDao: ActivityDao,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(activityDao, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

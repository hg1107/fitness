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
                notes = notes.trim(),
                isSynced = false
            )

            // Save Activity and associated points in a transaction flow
            val activityId = activityDao.insertActivity(record)
            
            if (points.isNotEmpty()) {
                val dbPoints = points.map {
                    ActivityPoint(
                        activityId = activityId,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        timestamp = it.timestamp
                    )
                }
                activityDao.insertActivityPoints(dbPoints)
            }

            // Stop service and reset state
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
            activityDao.updateActivityNotes(activityId, notes.trim())
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
            val updated = current.copy(
                name = name.trim().ifEmpty { "Athlete" },
                age = if (age > 0) age else 30,
                weightKg = if (weightKg > 0.0) weightKg else 70.0,
                heightCm = if (heightCm > 0.0) heightCm else 175.0,
                preferredUnits = preferredUnits.trim().ifEmpty { "Metric" }
            )
            activityDao.insertUserProfile(updated)
        }
    }

    // --- Cloud Sync Simulator ---
    fun triggerCloudSync() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Checking connection..."
            kotlinx.coroutines.delay(800)

            if (!isOnline(appContext)) {
                _syncMessage.value = "Offline. Sync postponed."
                kotlinx.coroutines.delay(1500)
                _isSyncing.value = false
                _syncMessage.value = ""
                return@launch
            }

            // Retrieve all activities from database that are unsynced
            val list = allActivities.first().filter { !it.isSynced }
            if (list.isEmpty()) {
                _syncMessage.value = "Already up to date."
                kotlinx.coroutines.delay(1200)
                _isSyncing.value = false
                _syncMessage.value = ""
                return@launch
            }

            _syncMessage.value = "Uploading ${list.size} activities..."
            kotlinx.coroutines.delay(2000) // Simulate upload latency

            // Mark as synced in DB
            activityDao.markActivitiesAsSynced(list.map { it.id })
            _syncMessage.value = "Cloud Sync Complete!"
            kotlinx.coroutines.delay(1500)
            
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

    // --- Analytics Queries ---
    // Start of the current week (Monday)
    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return cal.timeInMillis
    }

    // Start of the current month
    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    // Weekly Summary Aggregation
    val weeklyDistanceKm: Flow<Double> = allActivities.map { list ->
        val since = getStartOfWeek()
        list.filter { it.startTime >= since }.sumOf { it.distanceMeters } / 1000.0
    }

    val weeklyWorkoutsCount: Flow<Int> = allActivities.map { list ->
        val since = getStartOfWeek()
        list.count { it.startTime >= since }
    }

    val weeklyDurationHours: Flow<Double> = allActivities.map { list ->
        val since = getStartOfWeek()
        list.filter { it.startTime >= since }.sumOf { it.durationSeconds } / 3600.0
    }

    val weeklyCaloriesBurned: Flow<Double> = allActivities.map { list ->
        val since = getStartOfWeek()
        list.filter { it.startTime >= since }.sumOf { it.calories }
    }

    // Monthly Summary Aggregation
    val monthlyDistanceKm: Flow<Double> = allActivities.map { list ->
        val since = getStartOfMonth()
        list.filter { it.startTime >= since }.sumOf { it.distanceMeters } / 1000.0
    }

    val monthlyActiveDaysCount: Flow<Int> = allActivities.map { list ->
        val since = getStartOfMonth()
        val calendar = Calendar.getInstance()
        list.filter { it.startTime >= since }
            .map {
                calendar.timeInMillis = it.startTime
                calendar.get(Calendar.DAY_OF_YEAR)
            }
            .distinct()
            .size
    }

    val monthlyCaloriesBurned: Flow<Double> = allActivities.map { list ->
        val since = getStartOfMonth()
        list.filter { it.startTime >= since }.sumOf { it.calories }
    }

    // Today's calories burned (for net calorie integration on Nutrition screen)
    val todayCaloriesBurned: Flow<Double> = allActivities.map { list ->
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        list.filter { it.startTime >= startOfDay }.sumOf { it.calories }
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

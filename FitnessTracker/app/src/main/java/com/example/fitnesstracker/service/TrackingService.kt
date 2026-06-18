package com.example.fitnesstracker.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.data.WorkoutDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.*

data class LocationPoint(val latitude: Double, val longitude: Double, val timestamp: Long)

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val activityType: String = "Running", // "Running", "Walking", "Cycling"
    val startTime: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val calories: Double = 0.0,
    val currentSpeedMps: Double = 0.0,
    val avgSpeedMps: Double = 0.0,
    val currentPaceSecondsPerKm: Double = 0.0,
    val routePoints: List<LocationPoint> = emptyList(),
    val gpsStatus: String = "No Signal" // "No Signal", "Weak GPS", "Strong GPS"
)

class TrackingService : Service() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var timerJob: Job? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private var weightKg: Double = 70.0 // Default fallback weight
    // Fix #8: cache preferred units so the notification shows the right distance unit
    private var preferredUnits: String = "Metric"

    // Set on resume so the distance traveled while paused is not added to the workout
    private var skipNextDistanceSegment = false

    // Throttle notification updates to avoid Android rate limiting and battery drain
    private var lastNotificationUpdateMs = 0L

    // Auto-pause: elapsedRealtime of the last detected movement
    private var lastMovementElapsedMs = 0L

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tracking_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_ACTIVITY_TYPE = "EXTRA_ACTIVITY_TYPE"

        private val _trackingState = MutableStateFlow(TrackingState())
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        // Helper to reset state externally if needed
        fun resetState() {
            _trackingState.value = TrackingState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        loadUserWeight()
        // Fix #4: Reset static state on cold start so stale isTracking=true
        // from a previous process death cannot cause ghost tracking
        if (!_trackingState.value.isTracking) {
            _trackingState.value = TrackingState()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val activityType = intent?.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "Running"

        when (action) {
            ACTION_START -> {
                startTracking(activityType)
            }
            ACTION_PAUSE -> {
                pauseTracking()
            }
            ACTION_RESUME -> {
                resumeTracking()
            }
            ACTION_STOP -> {
                stopTrackingService()
            }
            else -> {
                // STICKY restart after process death: the intent is null. We must call
                // startForeground() promptly to avoid ForegroundServiceDidNotStartInTimeException.
                startForeground(NOTIFICATION_ID, getNotification(_trackingState.value))
                if (_trackingState.value.isTracking) {
                    // In-memory state survived (rare): re-attach location updates and timer
                    requestLocationUpdates()
                    startTimer()
                } else {
                    // State was lost with the process; stop gracefully instead of lingering
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live progress of your outdoor activity tracking"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(state: TrackingState): Notification {
        val title = "${state.activityType} Session"
        val timeFormatted = formatDuration(state.elapsedSeconds)
        // Fix #8: Show distance in the user's preferred unit (km or miles)
        val distFormatted = if (preferredUnits == "Imperial") {
            String.format(java.util.Locale.US, "%.2f mi", state.distanceMeters * 0.000621371)
        } else {
            String.format(java.util.Locale.US, "%.2f km", state.distanceMeters / 1000.0)
        }
        val text = "$timeFormatted | $distFormatted | ${state.gpsStatus}"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        // Android throttles notification updates; refreshing every second also wastes battery.
        val now = SystemClock.elapsedRealtime()
        if (now - lastNotificationUpdateMs < 3000L) return
        lastNotificationUpdateMs = now
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, getNotification(_trackingState.value))
    }

    private fun loadUserWeight() {
        serviceScope.launch {
            try {
                val db = WorkoutDatabase.getDatabase(applicationContext)
                val profile = db.activityDao().getUserProfileSync()
                if (profile != null) {
                    weightKg = profile.weightKg
                    // Fix #8: also cache preferred units
                    preferredUnits = profile.preferredUnits
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTracking(activityType: String) {
        loadUserWeight() // Make sure weight is loaded

        // Acquire WakeLock
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "FitnessTracker:TrackingWakeLock").apply {
                acquire(10 * 60 * 60 * 1000L) // 10 hours max safety timeout
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lastMovementElapsedMs = SystemClock.elapsedRealtime()

        _trackingState.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                activityType = activityType,
                startTime = System.currentTimeMillis(),
                elapsedSeconds = 0L,
                distanceMeters = 0.0,
                calories = 0.0,
                currentSpeedMps = 0.0,
                avgSpeedMps = 0.0,
                currentPaceSecondsPerKm = 0.0,
                routePoints = emptyList(),
                gpsStatus = "Searching GPS..."
            )
        }

        // Start Foreground Notification
        startForeground(NOTIFICATION_ID, getNotification(_trackingState.value))

        // Start Location Updates
        requestLocationUpdates()

        // Start Timer
        startTimer()
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _trackingState.update { it.copy(gpsStatus = "No Permissions") }
            return
        }

        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(2000L)
                .setMinUpdateDistanceMeters(0f)
                .build()
            fusedLocationClient?.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            _trackingState.update { it.copy(gpsStatus = "GPS Connected") }
        } catch (e: Exception) {
            e.printStackTrace()
            _trackingState.update { it.copy(gpsStatus = "GPS Error") }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (_trackingState.value.isPaused || !_trackingState.value.isTracking) return
            result.lastLocation?.let { handleLocationUpdate(it) }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) {
                _trackingState.update { it.copy(gpsStatus = "Searching GPS...") }
            }
        }
    }

    private fun handleLocationUpdate(location: Location) {
        // 1. Hard reject: accuracy worse than 40m is too unreliable to record
        if (location.accuracy > 40.0) {
            _trackingState.update { it.copy(gpsStatus = "Weak GPS (${location.accuracy.toInt()}m)") }
            return
        }

        // Accuracy between 20–40m is usable but worth flagging to the user
        val gpsQuality = if (location.accuracy > 20.0) "Weak GPS (${location.accuracy.toInt()}m)" else "GPS Lock"

        val timestamp = System.currentTimeMillis()
        val currentPoint = LocationPoint(location.latitude, location.longitude, timestamp)
        val state = _trackingState.value
        val points = state.routePoints

        // First update after resume: record the point but skip the distance segment,
        // otherwise the distance traveled while paused would be added to the workout.
        if (skipNextDistanceSegment && points.isNotEmpty()) {
            skipNextDistanceSegment = false
            _trackingState.update {
                it.copy(routePoints = it.routePoints + currentPoint, gpsStatus = gpsQuality)
            }
            return
        }
        skipNextDistanceSegment = false

        var additionalDistance = 0.0
        var newSpeed = location.speed.toDouble() // m/s from GPS

        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            
            // Calculate distance using Haversine formula
            additionalDistance = com.example.fitnesstracker.util.FitnessMath.calculateHaversine(
                lastPoint.latitude, lastPoint.longitude,
                currentPoint.latitude, currentPoint.longitude
            )

            // 2. Ignore negligible movements to prevent spiderwebbing/GPS drift
            if (additionalDistance < 1.5) {
                return
            }

            // 3. Filter impossible jumps (speed > limit)
            val timeDiffSec = (timestamp - lastPoint.timestamp) / 1000.0
            if (timeDiffSec > 0) {
                val calcSpeed = additionalDistance / timeDiffSec
                val speedLimit = when (state.activityType) {
                    "Walking" -> 6.0    // ~21.6 km/h
                    "Running" -> 12.0   // ~43.2 km/h
                    "Cycling" -> 30.0   // ~108 km/h
                    else -> 15.0
                }
                if (calcSpeed > speedLimit) {
                    // Ignore location jump
                    _trackingState.update { it.copy(gpsStatus = "GPS Jump Discarded") }
                    return
                }

                // If GPS doesn't report speed, use our calculated speed
                if (newSpeed == 0.0) {
                    newSpeed = calcSpeed
                }
            }
        }

        if (additionalDistance > 0) {
            lastMovementElapsedMs = SystemClock.elapsedRealtime()
        }

        val newDistance = state.distanceMeters + additionalDistance
        val newPoints = points + currentPoint

        // Calculate average speed
        val activeSeconds = state.elapsedSeconds
        val avgSpeed = if (activeSeconds > 0) newDistance / activeSeconds else 0.0

        // Calculate current pace (seconds per km)
        val currentPace = if (newSpeed > 0.1) {
            1000.0 / newSpeed
        } else {
            0.0
        }

        _trackingState.update {
            it.copy(
                distanceMeters = newDistance,
                routePoints = newPoints,
                currentSpeedMps = newSpeed,
                avgSpeedMps = avgSpeed,
                currentPaceSecondsPerKm = currentPace,
                gpsStatus = gpsQuality
            )
        }

        updateNotification()
    }



    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!_trackingState.value.isPaused && _trackingState.value.isTracking) {
                    // Auto-pause: stop accumulating time/calories after 15s without
                    // movement (e.g. waiting at traffic lights); resumes automatically.
                    val noMovementMs = SystemClock.elapsedRealtime() - lastMovementElapsedMs
                    val autoPaused = _trackingState.value.routePoints.isNotEmpty() && noMovementMs > 15_000
                    if (autoPaused) {
                        if (_trackingState.value.gpsStatus != "Auto-Paused") {
                            _trackingState.update {
                                it.copy(
                                    gpsStatus = "Auto-Paused",
                                    currentSpeedMps = 0.0,
                                    currentPaceSecondsPerKm = 0.0
                                )
                            }
                            updateNotification()
                        }
                    } else {
                        _trackingState.update { state ->
                            val newElapsed = state.elapsedSeconds + 1
                            val calories = com.example.fitnesstracker.util.FitnessMath.calculateCaloriesBurned(state.activityType, weightKg, newElapsed)
                            state.copy(
                                elapsedSeconds = newElapsed,
                                calories = calories
                            )
                        }
                        updateNotification()
                    }
                }
            }
        }
    }



    private fun pauseTracking() {
        _trackingState.update { it.copy(isPaused = true, currentSpeedMps = 0.0, currentPaceSecondsPerKm = 0.0) }
        updateNotification()
    }

    private fun resumeTracking() {
        skipNextDistanceSegment = true
        _trackingState.update { it.copy(isPaused = false, gpsStatus = "GPS Resumed") }
        updateNotification()
    }

    private fun stopTrackingService() {
        // Release WakeLock safely
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null

        timerJob?.cancel()
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        
        _trackingState.update {
            it.copy(
                isTracking = false,
                currentSpeedMps = 0.0,
                currentPaceSecondsPerKm = 0.0
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        // Release WakeLock safely
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        wakeLock = null

        timerJob?.cancel()
        serviceJob.cancel()
        fusedLocationClient?.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", m, s)
        }
    }
}

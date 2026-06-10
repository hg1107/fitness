package com.example.fitnesstracker.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

    private var locationManager: LocationManager? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var timerJob: Job? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private var weightKg: Double = 70.0 // Default fallback weight

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
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        loadUserWeight()
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
        val distFormatted = String.format("%.2f km", state.distanceMeters / 1000.0)
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
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                3000L, // 3 seconds
                0f,    // 0 meters
                locationListener
            )
            _trackingState.update { it.copy(gpsStatus = "GPS Connected") }
        } catch (e: Exception) {
            e.printStackTrace()
            _trackingState.update { it.copy(gpsStatus = "GPS Error") }
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (_trackingState.value.isPaused || !_trackingState.value.isTracking) return
            handleLocationUpdate(location)
        }
        override fun onProviderEnabled(provider: String) {
            _trackingState.update { it.copy(gpsStatus = "GPS Enabled") }
        }
        override fun onProviderDisabled(provider: String) {
            _trackingState.update { it.copy(gpsStatus = "GPS Disabled") }
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

        var additionalDistance = 0.0
        var newSpeed = location.speed.toDouble() // m/s from GPS

        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            
            // Calculate distance using Haversine formula
            additionalDistance = calculateHaversine(
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

    private fun calculateHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!_trackingState.value.isPaused && _trackingState.value.isTracking) {
                    _trackingState.update { state ->
                        val newElapsed = state.elapsedSeconds + 1
                        val calories = calculateCalories(state.activityType, weightKg, newElapsed)
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

    private fun calculateCalories(activityType: String, weight: Double, durationSec: Long): Double {
        val met = when (activityType) {
            "Walking" -> 3.8
            "Running" -> 8.0
            "Cycling" -> 7.5
            else -> 6.0
        }
        val hours = durationSec / 3600.0
        return met * weight * hours
    }

    private fun pauseTracking() {
        _trackingState.update { it.copy(isPaused = true, currentSpeedMps = 0.0, currentPaceSecondsPerKm = 0.0) }
        updateNotification()
    }

    private fun resumeTracking() {
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
        locationManager?.removeUpdates(locationListener)
        
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
        locationManager?.removeUpdates(locationListener)
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
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}

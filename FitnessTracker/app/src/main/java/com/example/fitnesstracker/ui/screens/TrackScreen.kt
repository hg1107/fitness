package com.example.fitnesstracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.service.LocationPoint
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.screens.track.LiveTrackingDashboard
import com.example.fitnesstracker.ui.screens.track.PreTrackingDashboard
import com.example.fitnesstracker.ui.screens.track.SettingsConfigDialog
import com.google.android.gms.location.LocationServices

// Brand Colors mapped to theme
val StravaOrange = com.example.fitnesstracker.theme.StravaOrange
val DarkBackground = com.example.fitnesstracker.theme.Black
val SurfaceCard = com.example.fitnesstracker.theme.CardGray
val OutlinedBorder = com.example.fitnesstracker.theme.BorderGray
val MutedText = com.example.fitnesstracker.theme.MediumGray
val BrightText = com.example.fitnesstracker.theme.White

@Composable
fun TrackScreen(
    viewModel: ActivityViewModel,
    onActivitySaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackingState by viewModel.trackingState.collectAsState()
    val userProfileState by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedActivityType by remember { mutableStateOf("Running") }
    var isDarkMode by remember { mutableStateOf(true) }

    var preTrackingLocation by remember { mutableStateOf<LocationPoint?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val userProfile = userProfileState

    fun requestSingleLocationUpdate() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    1000L
                ).setMaxUpdates(1).build()
                
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            result.lastLocation?.let { loc ->
                                preTrackingLocation = LocationPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
                            }
                        }
                    },
                    android.os.Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    val mapPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        preTrackingLocation = LocationPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
                    } else {
                        requestSingleLocationUpdate()
                    }
                }.addOnFailureListener {
                    requestSingleLocationUpdate()
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasFineLocation, hasCoarseLocation) {
        if (hasFineLocation || hasCoarseLocation) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        preTrackingLocation = LocationPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
                    } else {
                        requestSingleLocationUpdate()
                    }
                }.addOnFailureListener {
                    requestSingleLocationUpdate()
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    // Launcher for location and notification permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.startActivity(selectedActivityType)
        }
    }

    fun startTrackingClick() {
        val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (finePerm != PackageManager.PERMISSION_GRANTED) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarsePerm != PackageManager.PERMISSION_GRANTED) permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notifPerm != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isEmpty()) {
            viewModel.startActivity(selectedActivityType)
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (!trackingState.isTracking) {
            val allActivities by viewModel.allActivities.collectAsState(initial = emptyList())
            PreTrackingDashboard(
                userProfile = userProfile,
                selectedActivityType = selectedActivityType,
                onActivityTypeSelected = { selectedActivityType = it },
                preTrackingLocation = preTrackingLocation,
                isDarkMode = isDarkMode,
                hasLocationPermission = hasFineLocation || hasCoarseLocation,
                onRefreshLocation = { requestSingleLocationUpdate() },
                onRequestPermission = {
                    mapPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onStartTracking = { startTrackingClick() },
                onSettingsClicked = { showSettingsDialog = true },
                allActivities = allActivities,
                onActivitySaved = onActivitySaved,
                onDeleteActivity = { viewModel.deleteActivity(it) }
            )
        } else {
            LiveTrackingDashboard(
                trackingState = trackingState,
                userProfile = userProfile,
                isDarkMode = isDarkMode,
                onToggleMapTheme = { isDarkMode = !isDarkMode },
                onPause = { viewModel.pauseActivity() },
                onResume = { viewModel.resumeActivity() },
                onFinish = {
                    viewModel.saveCompletedActivity("") { activityId ->
                        onActivitySaved(activityId)
                    }
                },
                onDiscard = { viewModel.discardActivity() }
            )
        }
    }

    if (showSettingsDialog) {
        SettingsConfigDialog(
            userProfile = userProfile,
            onDismiss = { showSettingsDialog = false },
            onSaveProfile = { name, age, weightKg, heightCm, preferredUnits ->
                viewModel.updateUserProfile(
                    name = name,
                    age = age,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    preferredUnits = preferredUnits
                )
                showSettingsDialog = false
            }
        )
    }
}

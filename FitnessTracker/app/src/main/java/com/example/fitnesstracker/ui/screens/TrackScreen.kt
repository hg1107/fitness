package com.example.fitnesstracker.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.gms.location.LocationServices
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import java.text.SimpleDateFormat
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.fitnesstracker.service.LocationPoint
import com.example.fitnesstracker.service.TrackingState
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.data.ActivityRecord
import com.example.fitnesstracker.util.formatDuration
import com.example.fitnesstracker.util.formatPace
import java.util.Locale

// Brand Colors
val StravaOrange = Color(0xFFFC4C02)
val DarkBackground = Color(0xFF000000)
val SurfaceCard = Color(0xFF1C1C1E)
val OutlinedBorder = Color(0xFF2C2C2E)
val MutedText = Color(0xFF8E8E93)
val BrightText = Color(0xFFFFFFFF)

@Composable
fun TrackScreen(
    viewModel: ActivityViewModel,
    onActivitySaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackingState by viewModel.trackingState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedActivityType by remember { mutableStateOf("Running") }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(true) }

    var preTrackingLocation by remember { mutableStateOf<LocationPoint?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

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
            var activityToDelete by remember { mutableStateOf<ActivityRecord?>(null) }

            // Pre-Activity Dashboard Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Record Activity",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightText
                    )
                    Text(
                        text = "Select type and begin tracking",
                        fontSize = 14.sp,
                        color = MutedText
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Activity Type selector
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Activity Type",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrightText,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Running", "Walking", "Cycling").forEach { type ->
                            val isSelected = selectedActivityType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) StravaOrange else SurfaceCard)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) StravaOrange else OutlinedBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedActivityType = type }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) DarkBackground else BrightText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // GPS Map View
                Text(
                    text = "GPS Map",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrightText,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, OutlinedBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (hasFineLocation || hasCoarseLocation) {
                            ActivityMapView(
                                routePoints = emptyList(),
                                currentLocation = preTrackingLocation,
                                isDarkMode = isDarkMode,
                                fitRouteBounds = false,
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Re-center button on top right of the map
                            FloatingActionButton(
                                onClick = { requestSingleLocationUpdate() },
                                containerColor = SurfaceCard,
                                contentColor = BrightText,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(38.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Location",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            // Request permission overlay
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkBackground.copy(alpha = 0.6f))
                                    .clickable {
                                        mapPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Location Needed",
                                    tint = StravaOrange,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Location Permission Needed",
                                    color = BrightText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap here to allow location access and display your location on the map.",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Configuration cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile & Settings Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, OutlinedBorder, RoundedCornerShape(12.dp))
                            .clickable { showSettingsDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Profile & Settings", fontSize = 13.sp, color = MutedText)
                            val isImperial = userProfile.preferredUnits == "Imperial"
                            val displayWeight = if (isImperial) {
                                "${String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.kgToLbs(userProfile.weightKg))} lbs"
                            } else {
                                "${userProfile.weightKg} kg"
                            }
                            Text("${userProfile.name} • $displayWeight • ${userProfile.preferredUnits}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrightText)
                        }
                        Text("Edit", color = StravaOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Big Start Button
                Button(
                    onClick = { startTrackingClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = DarkBackground),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(110.dp)
                        .border(4.dp, BrightText.copy(alpha = 0.2f), CircleShape),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "START",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Recent Activities section
                if (allActivities.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Recent Sessions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        allActivities.take(3).forEach { activity ->
                            val isImperial = userProfile.preferredUnits == "Imperial"
                            val date = java.util.Date(activity.startTime)
                            val dateFormatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            val dateString = dateFormatter.format(date)
                            val durationString = formatDuration(activity.durationSeconds)
                            val distanceKm = activity.distanceMeters / 1000.0
                            val displayDistance = if (isImperial) activity.distanceMeters * 0.000621371 else distanceKm
                            val distanceUnit = if (isImperial) "mi" else "km"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .border(1.dp, OutlinedBorder, RoundedCornerShape(12.dp))
                                    .clickable { 
                                        onActivitySaved(activity.id)
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.activityType,
                                        color = StravaOrange,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateString,
                                        color = MutedText,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2f %s", displayDistance, distanceUnit),
                                            color = BrightText,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = durationString,
                                            color = MutedText,
                                            fontSize = 12.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { activityToDelete = activity },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Log",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Delete confirmation dialog
            activityToDelete?.let { activity ->
                Dialog(onDismissRequest = { activityToDelete = null }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceCard,
                        border = BorderStroke(1.dp, OutlinedBorder),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Delete Activity?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Delete this ${activity.activityType} on ${
                                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(java.util.Date(activity.startTime))
                            }? This cannot be undone.", fontSize = 14.sp, color = MutedText)
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { activityToDelete = null }) {
                                    Text("Cancel", color = BrightText)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.deleteActivity(activity)
                                        activityToDelete = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C), contentColor = BrightText)
                                ) {
                                    Text("Delete", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Live Tracking Dashboard Layout
            val elapsedFormatted = formatDuration(trackingState.elapsedSeconds)
            val distanceKm = trackingState.distanceMeters / 1000.0
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Stats Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trackingState.activityType,
                        color = StravaOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            trackingState.gpsStatus.contains("Lock") || trackingState.gpsStatus.contains("Connected") -> Color(0xFF2ECC71)
                            trackingState.gpsStatus.contains("Weak") -> Color(0xFFF1C40F)
                            else -> Color(0xFFE74C3C)
                        },
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = trackingState.gpsStatus.uppercase(),
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Active Stats Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(horizontal = 16.dp).padding(bottom = 16.dp)
                ) {
                    // Large Time Counter
                    Text(
                        text = elapsedFormatted,
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Light,
                        color = BrightText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isImperial = userProfile.preferredUnits == "Imperial"

                        // Distance Metric
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            val displayDistance = if (isImperial) {
                                com.example.fitnesstracker.util.UnitConverter.metersToMiles(trackingState.distanceMeters)
                            } else {
                                com.example.fitnesstracker.util.UnitConverter.metersToKm(trackingState.distanceMeters)
                            }
                            Text(
                                text = String.format(Locale.US, "%.2f", displayDistance),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightText
                            )
                            Text(if (isImperial) "DISTANCE (MI)" else "DISTANCE (KM)", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                        }

                        // Speed / Pace Metric
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            val metricValue: String
                            val metricLabel: String
                            
                            if (trackingState.activityType == "Cycling") {
                                val speedVal = if (isImperial) {
                                    com.example.fitnesstracker.util.UnitConverter.mpsToMph(trackingState.currentSpeedMps)
                                } else {
                                    com.example.fitnesstracker.util.UnitConverter.mpsToKmh(trackingState.currentSpeedMps)
                                }
                                metricValue = String.format(Locale.US, "%.1f", speedVal)
                                metricLabel = if (isImperial) "SPEED (MPH)" else "SPEED (KM/H)"
                            } else {
                                val paceVal = if (isImperial) {
                                    com.example.fitnesstracker.util.UnitConverter.paceKmToMile(trackingState.currentPaceSecondsPerKm)
                                } else {
                                    trackingState.currentPaceSecondsPerKm
                                }
                                metricValue = formatPace(paceVal)
                                metricLabel = if (isImperial) "PACE (/MI)" else "PACE (/KM)"
                            }
                            
                            Text(
                                text = metricValue,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightText
                            )
                            Text(metricLabel, fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                        }

                        // Calories Metric
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = trackingState.calories.toInt().toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrightText
                            )
                            Text("CALORIES (KCAL)", fontSize = 11.sp, color = MutedText, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Map View Area (occupies weight)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.DarkGray)
                ) {
                    ActivityMapView(
                        routePoints = trackingState.routePoints,
                        currentLocation = trackingState.routePoints.lastOrNull(),
                        isDarkMode = isDarkMode,
                        fitRouteBounds = false,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Map theme toggle button
                    FloatingActionButton(
                        onClick = { isDarkMode = !isDarkMode },
                        containerColor = SurfaceCard,
                        contentColor = BrightText,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(44.dp),
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (isDarkMode) "☀️" else "🌙",
                            fontSize = 18.sp
                        )
                    }
                }

                // Live Tracking Control Dashboard
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .padding(vertical = 20.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (trackingState.isPaused) {
                        // Resuming and Stopping options
                        Button(
                            onClick = { showCancelConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C), contentColor = BrightText),
                            shape = CircleShape,
                            modifier = Modifier.size(70.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("DISCARD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.resumeActivity() },
                            colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = DarkBackground),
                            shape = CircleShape,
                            modifier = Modifier.size(90.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(40.dp))
                        }

                        Button(
                            onClick = {
                                viewModel.saveCompletedActivity("") { activityId ->
                                    onActivitySaved(activityId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71), contentColor = DarkBackground),
                            shape = CircleShape,
                            modifier = Modifier.size(70.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("FINISH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Single Pause button
                        Button(
                            onClick = { viewModel.pauseActivity() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightText, contentColor = DarkBackground),
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("PAUSE", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    // Profile & Settings Dialog
    if (showSettingsDialog) {
        var nameInput by remember { mutableStateOf(userProfile.name) }
        var ageInput by remember { mutableStateOf(userProfile.age.toString()) }
        var weightInput by remember { mutableStateOf(
            if (userProfile.preferredUnits == "Imperial") {
                String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.kgToLbs(userProfile.weightKg))
            } else {
                userProfile.weightKg.toString()
            }
        ) }
        var heightInput by remember { mutableStateOf(
            if (userProfile.preferredUnits == "Imperial") {
                String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.cmToInches(userProfile.heightCm))
            } else {
                userProfile.heightCm.toString()
            }
        ) }
        var unitInput by remember { mutableStateOf(userProfile.preferredUnits) }

        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            // Fix #11: Constrain dialog height so keyboard doesn't push content off screen
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlinedBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Profile & Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                    
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name", color = MutedText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrightText,
                            unfocusedTextColor = BrightText,
                            focusedBorderColor = StravaOrange,
                            unfocusedBorderColor = OutlinedBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Age", color = MutedText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightText,
                                unfocusedTextColor = BrightText,
                                focusedBorderColor = StravaOrange,
                                unfocusedBorderColor = OutlinedBorder
                            ),
                            modifier = Modifier.weight(1.5f)
                        )

                        // Preferred units selector
                        Column(modifier = Modifier.weight(2f)) {
                            Text("Units", fontSize = 11.sp, color = MutedText, modifier = Modifier.padding(start = 4.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBackground)
                                    .border(1.dp, OutlinedBorder, RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            ) {
                                listOf("Metric", "Imperial").forEach { unit ->
                                    val isSelected = unitInput == unit
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) StravaOrange else Color.Transparent)
                                            .clickable { 
                                                val oldUnit = unitInput
                                                if (oldUnit != unit) {
                                                    unitInput = unit
                                                    val weightVal = weightInput.toDoubleOrNull() ?: 0.0
                                                    val heightVal = heightInput.toDoubleOrNull() ?: 0.0
                                                    if (unit == "Imperial") {
                                                        weightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.kgToLbs(weightVal))
                                                        heightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.cmToInches(heightVal))
                                                    } else {
                                                        weightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.lbsToKg(weightVal))
                                                        heightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.inchesToCm(heightVal))
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unit,
                                            color = if (isSelected) DarkBackground else BrightText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val weightLabel = if (unitInput == "Imperial") "Weight (lbs)" else "Weight (kg)"
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text(weightLabel, color = MutedText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightText,
                                unfocusedTextColor = BrightText,
                                focusedBorderColor = StravaOrange,
                                unfocusedBorderColor = OutlinedBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        val heightLabel = if (unitInput == "Imperial") "Height (in)" else "Height (cm)"
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text(heightLabel, color = MutedText) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrightText,
                                unfocusedTextColor = BrightText,
                                focusedBorderColor = StravaOrange,
                                unfocusedBorderColor = OutlinedBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }



                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel", color = BrightText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val finalWeight = weightInput.toDoubleOrNull() ?: 70.0
                                val finalHeight = heightInput.toDoubleOrNull() ?: 175.0
                                val savedWeight = if (unitInput == "Imperial") com.example.fitnesstracker.util.UnitConverter.lbsToKg(finalWeight) else finalWeight
                                val savedHeight = if (unitInput == "Imperial") com.example.fitnesstracker.util.UnitConverter.inchesToCm(finalHeight) else finalHeight
                                
                                viewModel.updateUserProfile(
                                    name = nameInput,
                                    age = ageInput.toIntOrNull() ?: 30,
                                    weightKg = savedWeight,
                                    heightCm = savedHeight,
                                    preferredUnits = unitInput
                                )
                                showSettingsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = DarkBackground)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Stop and Discard activity confirmation dialog
    if (showCancelConfirmDialog) {
        Dialog(onDismissRequest = { showCancelConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlinedBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Discard Workout?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("This activity details and GPS coordinates will be permanently deleted and cannot be recovered.", fontSize = 14.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCancelConfirmDialog = false }) {
                            Text("Keep Tracking", color = BrightText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.discardActivity()
                                showCancelConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C), contentColor = BrightText)
                        ) {
                            Text("Discard", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityMapView(
    routePoints: List<LocationPoint>,
    currentLocation: LocationPoint?,
    isDarkMode: Boolean,
    fitRouteBounds: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isMapInitialized by remember { mutableStateOf(false) }
    // Fix #21: track how many points were already sent to avoid re-sending the full route
    var lastSentPointCount by remember { mutableStateOf(0) }

    // Fix #21: Build only the new incremental points (delta) instead of the full array
    val newPointsJson = remember(routePoints) {
        if (routePoints.size <= lastSentPointCount) return@remember "[]"
        val newPoints = routePoints.drop(lastSentPointCount)
        val sb = StringBuilder()
        sb.append("[")
        newPoints.forEachIndexed { index, p ->
            sb.append("{\"latitude\":${p.latitude},\"longitude\":${p.longitude}}")
            if (index < newPoints.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    // For initial load we still need the full route
    val fullPointsJson = remember(routePoints) {
        val sb = StringBuilder()
        sb.append("[")
        routePoints.forEachIndexed { index, p ->
            sb.append("{\"latitude\":${p.latitude},\"longitude\":${p.longitude}}")
            if (index < routePoints.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.toString()
    }

    LaunchedEffect(isDarkMode, isMapInitialized) {
        if (isMapInitialized) {
            val themeStr = if (isDarkMode) "dark" else "light"
            webViewRef?.evaluateJavascript("setMapTheme('$themeStr')", null)
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                // Fix #15: Keep allowFileAccess=true for android_asset/ URLs (map.html),
                // but disable cross-origin file:// reads to limit attack surface.
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true           // needed for android_asset/
                @Suppress("DEPRECATION")
                settings.allowFileAccessFromFileURLs = true // allows JS to load local files
                @Suppress("DEPRECATION")
                settings.allowUniversalAccessFromFileURLs = true // allows loading network tiles from file:// origin
                
                // Allow loading tiles over HTTPS from local file:// origin
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // Set standard browser User-Agent to prevent tile CDNs from blocking WebView
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        android.util.Log.d("MapWebView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        isMapInitialized = true
                        
                        val startLat = currentLocation?.latitude ?: 0.0
                        val startLon = currentLocation?.longitude ?: 0.0
                        val themeStr = if (isDarkMode) "dark" else "light"
                        
                        evaluateJavascript("initMap($startLat, $startLon, '$themeStr')", null)
                        
                        if (routePoints.isNotEmpty()) {
                            evaluateJavascript("setRoute('$fullPointsJson', $fitRouteBounds)", null)
                            lastSentPointCount = routePoints.size
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        android.util.Log.e("MapWebView", "Network Error: ${error?.description} for URL: ${request?.url}")
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        errorResponse: android.webkit.WebResourceResponse?
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        android.util.Log.e("MapWebView", "HTTP Error: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase} for URL: ${request?.url}")
                    }
                }
                loadUrl("file:///android_asset/map.html")
            }
        },
        update = { webView ->
            if (isMapInitialized) {
                if (currentLocation != null) {
                    webView.evaluateJavascript("updateCurrentLocation(${currentLocation.latitude}, ${currentLocation.longitude})", null)
                }
                // Fix #21: Send only the delta (new points) not the full route each update
                if (routePoints.size > lastSentPointCount && newPointsJson != "[]") {
                    webView.evaluateJavascript("appendRoute('$newPointsJson', $fitRouteBounds)", null)
                    lastSentPointCount = routePoints.size
                } else if (routePoints.isEmpty()) {
                    webView.evaluateJavascript("clearRoute()", null)
                    lastSentPointCount = 0
                }
            }
        },
        modifier = modifier
    )
}

// Note: formatDuration() and formatPace() are imported from com.example.fitnesstracker.util.FormatUtils
// Fix #28: Moved to shared FormatUtils.kt so they can be used across screens

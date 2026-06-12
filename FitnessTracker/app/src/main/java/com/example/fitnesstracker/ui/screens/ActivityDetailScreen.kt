package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitnesstracker.data.ActivityPoint
import com.example.fitnesstracker.data.ActivityRecord
import com.example.fitnesstracker.service.LocationPoint
import com.example.fitnesstracker.ui.ActivityViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.fitnesstracker.util.formatDuration
import com.example.fitnesstracker.util.formatPace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    viewModel: ActivityViewModel,
    onNavigateBack: () -> Unit
) {
    var activity by remember { mutableStateOf<ActivityRecord?>(null) }
    var points by remember { mutableStateOf<List<ActivityPoint>>(emptyList()) }
    val userProfile by viewModel.userProfile.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(true) }

    LaunchedEffect(activityId) {
        activity = viewModel.getActivityById(activityId)
        viewModel.getPointsForActivity(activityId).first().let {
            points = it
        }
    }

    if (activity == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = StravaOrange)
        }
        // Note: return is only safe here because this is a full composable tree replacement
        // (we render a complete Box and return, not an early return mid-composition).
        return
    }

    val record = activity!!
    val isImperial = userProfile.preferredUnits == "Imperial"
    val displayDistance = if (isImperial) com.example.fitnesstracker.util.UnitConverter.metersToMiles(record.distanceMeters) else com.example.fitnesstracker.util.UnitConverter.metersToKm(record.distanceMeters)
    val distanceUnit = if (isImperial) "mi" else "km"
    val mapPoints = remember(points) {
        points.map { LocationPoint(it.latitude, it.longitude, it.timestamp) }
    }

    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(record.startTime))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = record.activityType,
                        fontWeight = FontWeight.Bold,
                        color = BrightText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrightText
                        )
                    }
                },
                actions = {
                    val gpxScope = rememberCoroutineScope()
                    val gpxContext = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = {
                        gpxScope.launch {
                            com.example.fitnesstracker.util.DataExporter.shareActivityGpx(gpxContext, activityId)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export route as GPX",
                            tint = BrightText
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Activity",
                            tint = Color(0xFFE74C3C)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceCard,
                    titleContentColor = BrightText
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Date subtitle
            Text(
                text = formattedDate,
                fontSize = 14.sp,
                color = MutedText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Map Route
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, OutlinedBorder, RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
            ) {
                if (mapPoints.isNotEmpty()) {
                    ActivityMapView(
                        mapboxToken = userProfile.mapboxToken,
                        routePoints = mapPoints,
                        currentLocation = null,
                        isDarkMode = isDarkMode,
                        fitRouteBounds = true,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Map theme toggle button
                    IconButton(
                        onClick = { isDarkMode = !isDarkMode },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .background(SurfaceCard, RoundedCornerShape(50.dp))
                            .border(1.dp, OutlinedBorder, RoundedCornerShape(50.dp))
                    ) {
                        Text(
                            text = if (isDarkMode) "☀️" else "🌙",
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No route data recorded", color = MutedText, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, OutlinedBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workout Stats",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StravaOrange
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Distance", fontSize = 11.sp, color = MutedText)
                            Text(String.format(java.util.Locale.US, "%.2f %s", displayDistance, distanceUnit), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrightText)
                        }

                        Column {
                            Text("Duration", fontSize = 11.sp, color = MutedText)
                            Text(formatDuration(record.durationSeconds), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrightText)
                        }

                        Column {
                            Text("Calories", fontSize = 11.sp, color = MutedText)
                            Text("${record.calories.toInt()} kcal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrightText)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = OutlinedBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                        Text("Average Speed", fontSize = 11.sp, color = MutedText)
                            val speedVal = if (isImperial) com.example.fitnesstracker.util.UnitConverter.mpsToMph(record.avgSpeed) else com.example.fitnesstracker.util.UnitConverter.mpsToKmh(record.avgSpeed)
                            Text(String.format(java.util.Locale.US, "%.1f %s", speedVal, if (isImperial) "mph" else "km/h"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                        }

                        Column {
                            Text("Average Pace", fontSize = 11.sp, color = MutedText)
                            val paceVal = if (isImperial) com.example.fitnesstracker.util.UnitConverter.paceKmToMile(record.avgPace) else record.avgPace
                            Text("${formatPace(paceVal)} /${if (isImperial) "mi" else "km"}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                        }
                        
                        Spacer(modifier = Modifier.width(30.dp))
                    }
                }
            }

            // Custom Notes display
            if (record.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Notes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrightText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, OutlinedBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = record.notes,
                            fontSize = 14.sp,
                            color = BrightText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlinedBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Delete Activity?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Are you sure you want to delete this workout? This action cannot be undone.", fontSize = 14.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel", color = BrightText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteActivity(record)
                                showDeleteConfirm = false
                                onNavigateBack()
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
}

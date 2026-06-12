package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitnesstracker.data.ActivityPoint
import com.example.fitnesstracker.data.ActivityRecord
import com.example.fitnesstracker.service.LocationPoint
import com.example.fitnesstracker.ui.ActivityViewModel
import kotlinx.coroutines.flow.first

@Composable
fun ActivitySummaryScreen(
    activityId: Long,
    viewModel: ActivityViewModel,
    onSaveOrDiscard: () -> Unit
) {
    var activity by remember { mutableStateOf<ActivityRecord?>(null) }
    var points by remember { mutableStateOf<List<ActivityPoint>>(emptyList()) }
    var notesText by remember { mutableStateOf("") }
    val userProfile by viewModel.userProfile.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
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
        return
    }

    val record = activity!!
    val isImperial = userProfile.preferredUnits == "Imperial"
    val displayDistance = if (isImperial) com.example.fitnesstracker.util.UnitConverter.metersToMiles(record.distanceMeters) else com.example.fitnesstracker.util.UnitConverter.metersToKm(record.distanceMeters)
    val distanceUnit = if (isImperial) "mi" else "km"
    val durationFormatted = formatDuration(record.durationSeconds)

    // Convert ActivityPoints to LocationPoints for mapping
    val mapPoints = remember(points) {
        points.map { LocationPoint(it.latitude, it.longitude, it.timestamp) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Activity Summary",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BrightText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Text(
            text = "Nice job! Here are your stats.",
            fontSize = 14.sp,
            color = MutedText,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Embedded route map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, OutlinedBorder, RoundedCornerShape(16.dp))
                .background(SurfaceCard)
        ) {
            if (mapPoints.isNotEmpty()) {
                ActivityMapView(
                    mapboxToken = userProfile.mapboxToken,
                    routePoints = mapPoints,
                    currentLocation = null, // don't show real-time GPS pulse dot
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
                    Text("No GPS route data recorded", color = MutedText, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Stats Grid
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, OutlinedBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${record.activityType} workout metrics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
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
                        Text(durationFormatted, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrightText)
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

        Spacer(modifier = Modifier.height(20.dp))

        // Workout Notes Box
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "How did it feel?",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrightText
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                placeholder = { Text("Write workout details, shoes worn, weather conditions...", color = MutedText) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BrightText,
                    unfocusedTextColor = BrightText,
                    focusedBorderColor = StravaOrange,
                    unfocusedBorderColor = OutlinedBorder,
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save / Discard Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showDiscardDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE74C3C)),
                border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Discard Activity", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.updateActivityNotes(record.id, notesText)
                    onSaveOrDiscard()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = DarkBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
            ) {
                Text("Save Activity", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showDiscardDialog) {
        Dialog(onDismissRequest = { showDiscardDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceCard,
                border = BorderStroke(1.dp, OutlinedBorder),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Delete this Activity?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("This will permanently remove the activity from your device.", fontSize = 14.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDiscardDialog = false }) {
                            Text("Cancel", color = BrightText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.deleteActivity(record)
                                showDiscardDialog = false
                                onSaveOrDiscard()
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

package com.example.fitnesstracker.ui.screens.track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.service.TrackingState
import com.example.fitnesstracker.util.formatDuration
import com.example.fitnesstracker.util.formatPace
import java.util.Locale

@Composable
fun LiveTrackingDashboard(
    trackingState: TrackingState,
    userProfile: UserProfile,
    isDarkMode: Boolean,
    onToggleMapTheme: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local theme references to match TrackScreen styling
    val StravaOrange = com.example.fitnesstracker.theme.StravaOrange
    val DarkBackground = com.example.fitnesstracker.theme.Black
    val SurfaceCard = com.example.fitnesstracker.theme.CardGray
    val OutlinedBorder = com.example.fitnesstracker.theme.BorderGray
    val MutedText = com.example.fitnesstracker.theme.MediumGray
    val BrightText = com.example.fitnesstracker.theme.White

    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    val elapsedFormatted = formatDuration(trackingState.elapsedSeconds)

    Column(
        modifier = modifier.fillMaxSize()
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
                onClick = onToggleMapTheme,
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
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = DarkBackground),
                    shape = CircleShape,
                    modifier = Modifier.size(90.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(40.dp))
                }

                Button(
                    onClick = onFinish,
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
                    onClick = onPause,
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
                                onDiscard()
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

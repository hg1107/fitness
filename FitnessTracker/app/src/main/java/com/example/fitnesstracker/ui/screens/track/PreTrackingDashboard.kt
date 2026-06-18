package com.example.fitnesstracker.ui.screens.track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import com.example.fitnesstracker.data.ActivityRecord
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.service.LocationPoint
import com.example.fitnesstracker.util.formatDuration
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PreTrackingDashboard(
    userProfile: UserProfile,
    selectedActivityType: String,
    onActivityTypeSelected: (String) -> Unit,
    preTrackingLocation: LocationPoint?,
    isDarkMode: Boolean,
    hasLocationPermission: Boolean,
    onRefreshLocation: () -> Unit,
    onRequestPermission: () -> Unit,
    onStartTracking: () -> Unit,
    onSettingsClicked: () -> Unit,
    allActivities: List<ActivityRecord>,
    onActivitySaved: (Long) -> Unit,
    onDeleteActivity: (ActivityRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local theme references to match TrackScreen styling
    val StravaOrange = com.example.fitnesstracker.theme.StravaOrange
    val DarkBackground = com.example.fitnesstracker.theme.Black
    val SurfaceCard = com.example.fitnesstracker.theme.CardGray
    val OutlinedBorder = com.example.fitnesstracker.theme.BorderGray
    val MutedText = com.example.fitnesstracker.theme.MediumGray
    val BrightText = com.example.fitnesstracker.theme.White

    var activityToDelete by remember { mutableStateOf<ActivityRecord?>(null) }

    Column(
        modifier = modifier
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
                            .clickable { onActivityTypeSelected(type) }
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
                if (hasLocationPermission) {
                    ActivityMapView(
                        routePoints = emptyList(),
                        currentLocation = preTrackingLocation,
                        isDarkMode = isDarkMode,
                        fitRouteBounds = false,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Re-center button on top right of the map
                    FloatingActionButton(
                        onClick = onRefreshLocation,
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
                            .clickable { onRequestPermission() }
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
                    .clickable { onSettingsClicked() }
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
            onClick = onStartTracking,
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
                            .clickable { onActivitySaved(activity.id) }
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
                                    text = String.format(Locale.US, "%.2f %s", displayDistance, distanceUnit),
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
                                onDeleteActivity(activity)
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
}

package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.data.ActivityRecord
import com.example.fitnesstracker.data.SessionWithSets
import com.example.fitnesstracker.data.WorkoutSet
import com.example.fitnesstracker.theme.Black
import com.example.fitnesstracker.theme.BorderGray
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.LightGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.ui.WorkoutViewModel
import com.example.fitnesstracker.ui.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    workoutViewModel: WorkoutViewModel,
    activityViewModel: ActivityViewModel,
    onViewActivityDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Gym Logs, 1 = GPS Tracking
    val userProfile by activityViewModel.userProfile.collectAsState()
    val isImperial = userProfile.preferredUnits == "Imperial"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "History",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "View your logs and recorded workouts",
            fontSize = 14.sp,
            color = MediumGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dual Segmented Tab Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardGray)
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedTab == 0) White else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Gym Logs",
                    color = if (selectedTab == 0) Black else LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedTab == 1) StravaOrange else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GPS Tracking",
                    color = if (selectedTab == 1) DarkBackground else LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            GymLogsSection(workoutViewModel, isImperial)
        } else {
            GpsTrackingSection(activityViewModel, isImperial, onViewActivityDetail)
        }
    }
}

@Composable
fun GymLogsSection(viewModel: WorkoutViewModel, isImperial: Boolean) {
    val allSessions by viewModel.allSessions.collectAsState(initial = emptyList())
    val weeklyVolume by viewModel.weeklyVolume.collectAsState(initial = 0.0)
    val weeklySetCount by viewModel.weeklySetCount.collectAsState(initial = 0)
    val weeklySessionCount by viewModel.weeklySessionCount.collectAsState(initial = 0)
    val activeDays by viewModel.weeklyActiveDays.collectAsState(initial = List(7) { false })
    var sessionToDelete by remember { mutableStateOf<SessionWithSets?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSessions = allSessions.filter {
        it.session.exerciseName.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "This Week",
            fontSize = 13.sp,
            color = LightGray,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        WeeklyConsistencyTracker(activeDays = activeDays)

        Spacer(modifier = Modifier.height(12.dp))

        // Gym Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayVolume = if (isImperial) com.example.fitnesstracker.util.UnitConverter.kgToLbs(weeklyVolume) else weeklyVolume
            val formattedVolume = displayVolume.let {
                if (it >= 1000) {
                    val k = it / 1000.0
                    if (k % 1.0 == 0.0) "${k.toInt()}k" else "%.1fk".format(k)
                } else {
                    if (it % 1.0 == 0.0) "${it.toInt()}" else "$it"
                }
            }
            
            val volumeUnit = if (isImperial) "lbs" else "kg"
            StatCard(
                label = "Volume",
                value = "${formattedVolume} ${volumeUnit}",
                modifier = Modifier.weight(1f)
            )
            
            StatCard(
                label = "Sets",
                value = "$weeklySetCount",
                modifier = Modifier.weight(1f)
            )

            StatCard(
                label = "Workouts",
                value = "$weeklySessionCount",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workout Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            if (allSessions.isNotEmpty()) {
                Text(
                    text = "${filteredSessions.size} found",
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (allSessions.isNotEmpty()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises...", color = MediumGray, fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = BorderGray,
                    unfocusedBorderColor = BorderGray,
                    focusedContainerColor = CardGray,
                    unfocusedContainerColor = CardGray
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            if (allSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No workouts logged yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Go to Plan and tap 'Log' on an exercise",
                            fontSize = 14.sp,
                            color = MediumGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No matching workouts",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different exercise name",
                            fontSize = 13.sp,
                            color = MediumGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSessions, key = { it.session.id }) { sessionWithSets ->
                        ExpandableHistoryRow(
                            sessionWithSets = sessionWithSets,
                            isImperial = isImperial,
                            onDelete = { sessionToDelete = sessionWithSets }
                        )
                    }
                }
            }
        }
    }

    sessionToDelete?.let { session ->
        GymLogsConfirmDeleteDialog(
            title = "Delete Workout",
            message = "Delete this ${session.session.exerciseName} session? This cannot be undone.",
            onConfirm = {
                viewModel.deleteSession(session)
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsTrackingSection(
    viewModel: ActivityViewModel,
    isImperial: Boolean,
    onViewActivityDetail: (Long) -> Unit
) {
    val allActivities by viewModel.allActivities.collectAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var activityToDelete by remember { mutableStateOf<ActivityRecord?>(null) }

    // Weekly summary aggregates
    val weeklyDistanceKm by viewModel.weeklyDistanceKm.collectAsState(initial = 0.0)
    val weeklyWorkoutsCount by viewModel.weeklyWorkoutsCount.collectAsState(initial = 0)
    val weeklyDurationHours by viewModel.weeklyDurationHours.collectAsState(initial = 0.0)
    val weeklyCaloriesBurned by viewModel.weeklyCaloriesBurned.collectAsState(initial = 0.0)

    // Monthly aggregates
    val monthlyDistanceKm by viewModel.monthlyDistanceKm.collectAsState(initial = 0.0)
    val monthlyActiveDaysCount by viewModel.monthlyActiveDaysCount.collectAsState(initial = 0)
    val monthlyCaloriesBurned by viewModel.monthlyCaloriesBurned.collectAsState(initial = 0.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Analytics Summary Cards Accordion
        Text(
            text = "GPS Summary",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = StravaOrange
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Summaries Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayWeeklyDistance = if (isImperial) weeklyDistanceKm * 0.621371 else weeklyDistanceKm
            val distanceLabel = if (isImperial) "mi" else "km"
            StatCard(
                label = "Weekly $distanceLabel",
                value = String.format(java.util.Locale.US, "%.1f", displayWeeklyDistance),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Weekly Hrs",
                value = String.format(java.util.Locale.US, "%.1f", weeklyDurationHours),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Weekly Kcal",
                value = weeklyCaloriesBurned.toInt().toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayMonthlyDistance = if (isImperial) monthlyDistanceKm * 0.621371 else monthlyDistanceKm
            val distanceLabel = if (isImperial) "mi" else "km"
            StatCard(
                label = "Monthly $distanceLabel",
                value = String.format(java.util.Locale.US, "%.1f", displayMonthlyDistance),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Monthly Days",
                value = monthlyActiveDaysCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Monthly Kcal",
                value = monthlyCaloriesBurned.toInt().toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cloud Sync Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, OutlinedBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = StravaOrange,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(syncMessage, fontSize = 12.sp, color = BrightText)
                } else {
                    val unsyncedCount = allActivities.count { !it.isSynced }
                    val statusText = if (unsyncedCount > 0) "$unsyncedCount unsynced workouts" else "All workouts synced to cloud"
                    Text(statusText, fontSize = 12.sp, color = MutedText)
                }
            }

            if (!isSyncing) {
                IconButton(
                    onClick = { viewModel.triggerCloudSync() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Cloud",
                        tint = StravaOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GPS Activities",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (allActivities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No activities recorded",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Navigate to Track, select activity, and press Start!",
                            fontSize = 13.sp,
                            color = MediumGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(allActivities, key = { it.id }) { activity ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    activityToDelete = activity
                                }
                                false // don't auto-dismiss; we show a confirmation dialog
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val targetAlpha = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f
                                val alpha by animateFloatAsState(targetValue = targetAlpha, label = "swipeAlpha")
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE74C3C).copy(alpha = alpha)),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = BrightText,
                                        modifier = Modifier.padding(end = 20.dp)
                                    )
                                }
                            }
                        ) {
                            ActivityRowItem(
                                activity = activity,
                                isImperial = isImperial,
                                onClick = { onViewActivityDetail(activity.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Swipe-delete confirmation dialog
    activityToDelete?.let { activity ->
        GymLogsConfirmDeleteDialog(
            title = "Delete Activity?",
            message = "Delete this ${activity.activityType} on ${
                java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(activity.startTime))
            }? This cannot be undone.",
            onConfirm = {
                viewModel.deleteActivity(activity)
                activityToDelete = null
            },
            onDismiss = { activityToDelete = null }
        )
    }
}

@Composable
fun ActivityRowItem(
    activity: ActivityRecord,
    isImperial: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = Date(activity.startTime)
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    val dateString = dateFormatter.format(date)
    val durationString = formatDuration(activity.durationSeconds)
    val distanceKm = activity.distanceMeters / 1000.0
    val displayDistance = if (isImperial) activity.distanceMeters * 0.000621371 else distanceKm
    val distanceUnit = if (isImperial) "mi" else "km"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, OutlinedBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = activity.activityType,
                    color = StravaOrange,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (activity.isSynced) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Synced",
                        tint = Color(0xFF2ECC71),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = dateString,
                color = MutedText,
                fontSize = 12.sp
            )

            if (activity.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.notes,
                    color = LightGray,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
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

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Details",
                tint = MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ExpandableHistoryRow(
    sessionWithSets: SessionWithSets,
    isImperial: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val date = Date(sessionWithSets.session.timestamp)
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateString = dateFormatter.format(date)
    val timeString = timeFormatter.format(date)

    val setSize = sessionWithSets.sets.size
    val totalVolume = sessionWithSets.sets.sumOf { it.weight * it.reps }
    val displayVolume = if (isImperial) com.example.fitnesstracker.util.UnitConverter.kgToLbs(totalVolume) else totalVolume
    val volumeUnit = if (isImperial) "lbs" else "kg"
    val formattedVolume = displayVolume.let {
        if (it % 1.0 == 0.0) "${it.toInt()}$volumeUnit" else String.format(java.util.Locale.US, "%.1f%s", it, volumeUnit)
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sessionWithSets.session.exerciseName,
                    color = White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$dateString  $timeString",
                    color = MediumGray,
                    fontSize = 12.sp
                )
                if (sessionWithSets.session.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sessionWithSets.session.notes,
                        color = MediumGray,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedVolume,
                        color = White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$setSize sets",
                        color = MediumGray,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "›",
                    color = MediumGray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(chevronRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
            ) {
                HorizontalDivider(color = BorderGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Set", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text(if (isImperial) "Weight (lbs)" else "Weight (kg)", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Reps", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Volume", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                Spacer(modifier = Modifier.height(6.dp))

                sessionWithSets.sets.sortedBy { it.setIndex }.forEachIndexed { idx, set ->
                    SetDetailRow(set = set, isImperial = isImperial, idx = idx + 1)
                    if (idx < sessionWithSets.sets.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                if (sessionWithSets.session.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Notes",
                        color = MediumGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sessionWithSets.session.notes,
                        color = LightGray,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MediumGray)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Session",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun SetDetailRow(
    set: WorkoutSet,
    isImperial: Boolean,
    idx: Int,
    modifier: Modifier = Modifier
) {
    val displayWeight = if (isImperial) com.example.fitnesstracker.util.UnitConverter.kgToLbs(set.weight) else set.weight
    val displayVolume = if (isImperial) com.example.fitnesstracker.util.UnitConverter.kgToLbs(set.weight * set.reps) else set.weight * set.reps
    val weightUnit = if (isImperial) "lbs" else "kg"
    val volumeUnit = if (isImperial) "lbs" else "kg"
    val weight = displayWeight.let { if (it % 1.0 == 0.0) "${it.toInt()}$weightUnit" else String.format(java.util.Locale.US, "%.1f%s", it, weightUnit) }
    val volume = displayVolume.let { if (it % 1.0 == 0.0) "${it.toInt()}$volumeUnit" else String.format(java.util.Locale.US, "%.1f%s", it, volumeUnit) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$idx",
            color = MediumGray,
            fontSize = 13.sp,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = weight,
            color = LightGray,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${set.reps}",
            color = LightGray,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = volume,
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun WeeklyConsistencyTracker(
    activeDays: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekdays.forEachIndexed { index, day ->
            val isActive = activeDays.getOrElse(index) { false }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isActive) White else Color.Transparent)
                        .border(1.dp, if (isActive) White else BorderGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Text(
                            text = "✓",
                            color = Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    text = day,
                    color = if (isActive) White else MediumGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MediumGray,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
    }
}

@Composable
private fun GymLogsConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = White, fontWeight = FontWeight.Bold) },
        text = { Text(text = message, color = LightGray) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LightGray)
            }
        },
        containerColor = CardGray,
        textContentColor = LightGray
    )
}


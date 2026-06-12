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
import com.example.fitnesstracker.ui.NutritionViewModel
import com.example.fitnesstracker.data.FoodLog
import com.example.fitnesstracker.data.WaterLog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.fitnesstracker.util.formatDuration

@Composable
fun HistoryScreen(
    workoutViewModel: WorkoutViewModel,
    activityViewModel: ActivityViewModel,
    nutritionViewModel: NutritionViewModel,
    onViewActivityDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Workouts, 1 = Activities, 2 = Analysis
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

        // Fix #7/#14: Use consistent StravaOrange indicator for all three tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardGray)
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                .padding(3.dp)
        ) {
            listOf("Workouts", "Activities", "Analysis").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedTab == index) StravaOrange else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedTab == index) DarkBackground else LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> GymLogsSection(workoutViewModel, isImperial)
            1 -> GpsTrackingSection(activityViewModel, isImperial, onViewActivityDetail)
            2 -> WeeklyAnalysisSection(workoutViewModel, activityViewModel, nutritionViewModel, isImperial)
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
                            allSessionsForExercise = filteredSessions.filter {
                                it.session.exerciseName == sessionWithSets.session.exerciseName
                            },
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
                                onClick = { onViewActivityDetail(activity.id) },
                                onDelete = { activityToDelete = activity }
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
    onDelete: () -> Unit,
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

            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Log",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
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
    allSessionsForExercise: List<SessionWithSets>,
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
    // PR: this session holds the single highest-weight set ever done for this exercise
    val thisSessionMaxWeight = sessionWithSets.sets.maxOfOrNull { it.weight } ?: 0.0
    val hasPersonalBest = thisSessionMaxWeight > 0.0 && allSessionsForExercise
        .filter { it.session.id != sessionWithSets.session.id }
        .flatMap { it.sets }
        .maxOfOrNull { it.weight }
        ?.let { thisSessionMaxWeight > it } ?: true


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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = sessionWithSets.session.exerciseName,
                        color = White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasPersonalBest) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                            modifier = Modifier
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "🏆 PR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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

@Composable
fun WeeklyAnalysisSection(
    workoutViewModel: WorkoutViewModel,
    activityViewModel: ActivityViewModel,
    nutritionViewModel: NutritionViewModel,
    isImperial: Boolean
) {
    val userProfileState by nutritionViewModel.userProfile.collectAsState(initial = null)
    
    // Gym weekly states
    val weeklyVolume by workoutViewModel.weeklyVolume.collectAsState(initial = 0.0)
    val weeklySetCount by workoutViewModel.weeklySetCount.collectAsState(initial = 0)
    val weeklySessionCount by workoutViewModel.weeklySessionCount.collectAsState(initial = 0)
    val activeDays by workoutViewModel.weeklyActiveDays.collectAsState(initial = List(7) { false })

    // Activity weekly states
    val weeklyDistanceKm by activityViewModel.weeklyDistanceKm.collectAsState(initial = 0.0)
    val weeklyDurationHours by activityViewModel.weeklyDurationHours.collectAsState(initial = 0.0)
    val weeklyCaloriesBurned by activityViewModel.weeklyCaloriesBurned.collectAsState(initial = 0.0)
    val weeklyWorkoutsCount by activityViewModel.weeklyWorkoutsCount.collectAsState(initial = 0)

    // Nutrition weekly states
    val weeklyFoodLogs by nutritionViewModel.weeklyFoodLogs.collectAsState(initial = emptyList())
    val weeklyWaterLogs by nutritionViewModel.weeklyWaterLogs.collectAsState(initial = emptyList())

    val profile = userProfileState ?: return

    val goal = nutritionViewModel.calculateGoal(
        gender = profile.gender,
        age = profile.age,
        weightKg = profile.weightKg,
        heightCm = profile.heightCm,
        activityLevel = profile.activityLevel,
        fitnessGoal = profile.fitnessGoal
    )

    // Compute averages
    val logsByDate = weeklyFoodLogs.groupBy { it.date }
    val loggedDaysCount = logsByDate.size.coerceAtLeast(1)

    val totalCalories = weeklyFoodLogs.sumOf { it.calories * it.quantity }
    val totalProtein = weeklyFoodLogs.sumOf { it.protein * it.quantity }
    val totalCarbs = weeklyFoodLogs.sumOf { it.carbs * it.quantity }
    val totalFat = weeklyFoodLogs.sumOf { it.fat * it.quantity }

    val avgCalories = totalCalories / loggedDaysCount
    val avgProtein = totalProtein / loggedDaysCount
    val avgCarbs = totalCarbs / loggedDaysCount
    val avgFat = totalFat / loggedDaysCount

    // Goal Compliance
    val compliantDays = logsByDate.filter { (_, logs) ->
        val dailyCals = logs.sumOf { it.calories * it.quantity }
        if (profile.fitnessGoal == "Fat Loss") {
            dailyCals <= goal.calories + 100
        } else {
            dailyCals in (goal.calories - 150)..(goal.calories + 250)
        }
    }.size
    val compliancePct = if (logsByDate.isNotEmpty()) (compliantDays * 100) / logsByDate.size else 0

    // Water average
    val waterByDate = weeklyWaterLogs.groupBy { it.date }
    val waterDaysCount = waterByDate.size.coerceAtLeast(1)
    val totalWater = weeklyWaterLogs.sumOf { it.amountMl }
    val avgWaterL = (totalWater / waterDaysCount) / 1000.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "This Week's Progress Report",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
        Text(
            text = "Summary of gym workouts, tracking, and diet goals",
            fontSize = 12.sp,
            color = MediumGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday Consistency
        Text("Workout Consistency", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightGray)
        Spacer(modifier = Modifier.height(8.dp))
        WeeklyConsistencyTracker(activeDays = activeDays)

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: Gym Workouts Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Strength Workouts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StravaOrange)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val volumeVal = if (isImperial) com.example.fitnesstracker.util.UnitConverter.kgToLbs(weeklyVolume) else weeklyVolume
                    val volUnit = if (isImperial) "lbs" else "kg"
                    Column {
                        Text("Volume", fontSize = 11.sp, color = MediumGray)
                        Text("${volumeVal.toInt()} $volUnit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                    Column {
                        Text("Sets Completed", fontSize = 11.sp, color = MediumGray)
                        Text("$weeklySetCount sets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                    Column {
                        Text("Sessions", fontSize = 11.sp, color = MediumGray)
                        Text("$weeklySessionCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: GPS Activities Progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cardio & GPS Tracking", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val distVal = if (isImperial) weeklyDistanceKm * 0.621371 else weeklyDistanceKm
                    val distUnit = if (isImperial) "mi" else "km"
                    Column {
                        Text("Distance", fontSize = 11.sp, color = MediumGray)
                        Text(String.format(Locale.US, "%.1f %s", distVal, distUnit), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                    Column {
                        Text("Duration", fontSize = 11.sp, color = MediumGray)
                        Text(String.format(Locale.US, "%.1f hrs", weeklyDurationHours), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                    Column {
                        Text("Energy Burned", fontSize = 11.sp, color = MediumGray)
                        Text("${weeklyCaloriesBurned.toInt()} kcal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 3: Nutrition & Hydration Averages
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Diet & Nutrition (Daily Averages)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Calories Intake", fontSize = 11.sp, color = MediumGray)
                        Text("${avgCalories.toInt()} kcal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                        Text("Target: ${goal.calories.toInt()}", fontSize = 10.sp, color = MediumGray)
                    }
                    Column {
                        Text("Avg Protein", fontSize = 11.sp, color = MediumGray)
                        Text("${avgProtein.toInt()}g", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        Text("Target: ${goal.protein.toInt()}g", fontSize = 10.sp, color = MediumGray)
                    }
                    Column {
                        Text("Goal Compliance", fontSize = 11.sp, color = MediumGray)
                        Text("$compliancePct%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White)
                        Text("Logged: ${logsByDate.size} days", fontSize = 10.sp, color = MediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderGray)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Average Hydration", fontSize = 11.sp, color = MediumGray)
                        Text(String.format(Locale.US, "%.1f Liters", avgWaterL), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Carbs", fontSize = 10.sp, color = MediumGray)
                            Text("${avgCarbs.toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Fat", fontSize = 10.sp, color = MediumGray)
                            Text("${avgFat.toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Insights & Recommendations
        Text("Weekly Fitness Insights", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = White)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val volumeVal = if (isImperial) com.example.fitnesstracker.util.UnitConverter.kgToLbs(weeklyVolume) else weeklyVolume
                val volUnit = if (isImperial) "lbs" else "kg"
                
                val gymInsight = if (weeklySessionCount > 0) {
                    "Strength: Logged $weeklySessionCount sessions with a total volume of ${volumeVal.toInt()} $volUnit. Keep up the progressive overload!"
                } else {
                    "Strength: No gym sessions logged this week. Try scheduling at least 2 strength workouts to maintain muscle mass."
                }
                Text("• $gymInsight", fontSize = 13.sp, color = LightGray)

                val distVal = if (isImperial) weeklyDistanceKm * 0.621371 else weeklyDistanceKm
                val distUnit = if (isImperial) "mi" else "km"
                
                val cardioInsight = if (weeklyWorkoutsCount > 0) {
                    "Cardio: Completed $weeklyWorkoutsCount tracked activities covering ${"%.1f".format(distVal)} $distUnit and burning ${weeklyCaloriesBurned.toInt()} kcal. Excellent energy expenditure!"
                } else {
                    "Cardio: No outdoor cardio tracked. Try walking or running to improve your cardiovascular fitness."
                }
                Text("• $cardioInsight", fontSize = 13.sp, color = LightGray)

                val nutritionInsight = if (logsByDate.isNotEmpty()) {
                    val proteinConsistencyText = if (avgProtein >= goal.protein - 10.0) {
                        "met protein targets consistently"
                    } else {
                        "need to increase your protein intake (avg ${avgProtein.toInt()}g vs goal ${goal.protein.toInt()}g)"
                    }
                    "Nutrition: Logged diet for ${logsByDate.size} days. Average daily intake is ${avgCalories.toInt()} kcal. You $proteinConsistencyText to match your ${profile.fitnessGoal} goal."
                } else {
                    "Nutrition: No meals logged in the database. Log what you eat on the Nutrition tab to get custom target analysis!"
                }
                Text("• $nutritionInsight", fontSize = 13.sp, color = LightGray)
            }
        }
    }
}


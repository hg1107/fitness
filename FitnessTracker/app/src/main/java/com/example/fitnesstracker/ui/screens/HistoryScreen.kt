package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.data.SessionWithSets
import com.example.fitnesstracker.data.WorkoutSet
import com.example.fitnesstracker.theme.Black
import com.example.fitnesstracker.theme.BorderGray
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.LightGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.ui.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val allSessions by viewModel.allSessions.collectAsState(initial = emptyList())
    val weeklyVolume by viewModel.weeklyVolume.collectAsState(initial = 0.0)
    val weeklySetCount by viewModel.weeklySetCount.collectAsState(initial = 0)
    val activeDays by viewModel.weeklyActiveDays.collectAsState(initial = List(7) { false })

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
            text = "Your weekly overview & logs",
            fontSize = 14.sp,
            color = MediumGray
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Consistency tracker (7 circles)
        Text(
            text = "This Week",
            fontSize = 13.sp,
            color = LightGray,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        WeeklyConsistencyTracker(activeDays = activeDays)

        Spacer(modifier = Modifier.height(12.dp))

        // Numeric aggregation summaries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val formattedVolume = weeklyVolume.let {
                if (it % 1.0 == 0.0) "${it.toInt()} kg" else "${it} kg"
            }
            
            StatCard(
                label = "Weekly Volume",
                value = formattedVolume,
                modifier = Modifier.weight(1f)
            )
            
            StatCard(
                label = "Sets Logged",
                value = "$weeklySetCount sets",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History Log Section
        Text(
            text = "Workout Logs",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(10.dp))

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
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(allSessions) { sessionWithSets ->
                        ExpandableHistoryRow(sessionWithSets = sessionWithSets)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableHistoryRow(
    sessionWithSets: SessionWithSets,
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
    val formattedVolume = totalVolume.let {
        if (it % 1.0 == 0.0) "${it.toInt()}kg" else "${it}kg"
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
        // Main row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: date + exercise
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
                // Show notes snippet if present
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

            // Right: stats + chevron
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

        // Expandable set details
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

                // Set detail header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Set", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text("Weight", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Reps", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Volume", color = MediumGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }

                Spacer(modifier = Modifier.height(6.dp))

                sessionWithSets.sets.sortedBy { it.setIndex }.forEachIndexed { idx, set ->
                    SetDetailRow(set = set, idx = idx + 1)
                    if (idx < sessionWithSets.sets.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun SetDetailRow(
    set: WorkoutSet,
    idx: Int,
    modifier: Modifier = Modifier
) {
    val weight = set.weight.let { if (it % 1.0 == 0.0) "${it.toInt()}kg" else "${it}kg" }
    val volume = (set.weight * set.reps).let { if (it % 1.0 == 0.0) "${it.toInt()}kg" else "${it}kg" }

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
                        // Small dot to denote completion
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Black)
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
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MediumGray,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
    }
}

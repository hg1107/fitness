package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.data.SessionWithSets
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
            text = "Workout History",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Consistency tracker (7 circles)
        Text(
            text = "This Week's Activity",
            fontSize = 13.sp,
            color = LightGray,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        WeeklyConsistencyTracker(activeDays = activeDays)

        Spacer(modifier = Modifier.height(16.dp))

        // Numeric aggregation summaries
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val formattedVolume = weeklyVolume.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
            
            StatCard(
                label = "Weekly Volume",
                value = "${formattedVolume} kg",
                modifier = Modifier.weight(1f)
            )
            
            StatCard(
                label = "Sets Logged",
                value = "$weeklySetCount sets",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History Log Section
        Text(
            text = "Workout Logs",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Table Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Date", color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
            Text("Exercise", color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Sets", color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Text("Volume", color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }

        Box(modifier = Modifier.weight(1f)) {
            if (allSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workouts logged yet.",
                        fontSize = 14.sp,
                        color = MediumGray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(allSessions) { sessionWithSets ->
                        HistoryRow(sessionWithSets = sessionWithSets)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyConsistencyTracker(
    activeDays: List<Boolean>,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

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
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isActive) White else Color.Transparent)
                        .border(1.dp, if (isActive) White else BorderGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        // Small dot or check to denote completion
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

@Composable
fun HistoryRow(
    sessionWithSets: SessionWithSets,
    modifier: Modifier = Modifier
) {
    val date = Date(sessionWithSets.session.timestamp)
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val dateString = formatter.format(date)
    
    val setSize = sessionWithSets.sets.size
    val totalVolume = sessionWithSets.sets.sumOf { it.weight * it.reps }
    val formattedVolume = totalVolume.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            text = dateString,
            color = LightGray,
            fontSize = 13.sp,
            modifier = Modifier.width(65.dp)
        )

        // Exercise Name
        Text(
            text = sessionWithSets.session.exerciseName,
            color = White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        // Set Count
        Text(
            text = "$setSize",
            color = LightGray,
            fontSize = 14.sp,
            modifier = Modifier.width(55.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        // Volume
        Text(
            text = "${formattedVolume}kg",
            color = White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

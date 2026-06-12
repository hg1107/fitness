package com.example.fitnesstracker.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import com.example.fitnesstracker.data.PlannedExercise
import com.example.fitnesstracker.theme.Black
import com.example.fitnesstracker.theme.BorderGray
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.LightGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.ui.WorkoutViewModel

@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    onLogExercise: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDay by viewModel.selectedDay.collectAsState(initial = 1)
    val exercises by viewModel.plannedExercises.collectAsState(initial = emptyList())
    val daysWithPlannedExercises by viewModel.daysWithPlannedExercises.collectAsState(initial = emptySet())
    val exerciseSuggestions by viewModel.uniqueExerciseNames.collectAsState(initial = emptyList())
    val workoutStreak by viewModel.workoutStreak.collectAsState(initial = 0)
    val todayLoggedExerciseNames by viewModel.todayLoggedExerciseNames.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<PlannedExercise?>(null) }
    val today = WorkoutViewModel.getCurrentDayOfWeek()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Workout Plan",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Select a day and log your sets",
            fontSize = 14.sp,
            color = MediumGray
        )

        // Streak banner
        if (workoutStreak > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔥", fontSize = 18.sp)
                Column {
                    Text(
                        text = "$workoutStreak-day streak!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "Keep it up — consistency is everything.",
                        fontSize = 11.sp,
                        color = MediumGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Weekday selector: Mon (1) to Sun (7)
        DayOfWeekSelector(
            selectedDay = selectedDay,
            todayDay = today,
            daysWithPlannedExercises = daysWithPlannedExercises,
            onDaySelected = { viewModel.selectDay(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = getDayName(selectedDay),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = LightGray
            )
            if (selectedDay == today) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = White
                ) {
                    Text(
                        text = "TODAY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Rest day",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+' to add exercises for ${getDayShort(selectedDay)}",
                            fontSize = 14.sp,
                            color = MediumGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(exercises) { exercise ->
                        ExercisePlanCard(
                            exercise = exercise,
                            isLoggedToday = todayLoggedExerciseNames.any {
                                it.equals(exercise.exerciseName, ignoreCase = true)
                            },
                            onLog = { onLogExercise(exercise.exerciseName) },
                            onDelete = { exerciseToDelete = exercise }
                        )
                    }
                }
            }

            // Fix #12: Increase bottom padding to clear the bottom navigation bar (~72dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 88.dp, end = 8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = White,
                    contentColor = Black,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            suggestions = exerciseSuggestions,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, muscle ->
                viewModel.addPlannedExercise(name, muscle)
                showAddDialog = false
            }
        )
    }

    // Delete confirmation dialog
    exerciseToDelete?.let { exercise ->
        ConfirmDeleteDialog(
            title = "Remove Exercise",
            message = "Remove \"${exercise.exerciseName}\" from ${getDayShort(selectedDay)}?",
            onConfirm = {
                viewModel.deletePlannedExercise(exercise)
                exerciseToDelete = null
            },
            onDismiss = { exerciseToDelete = null }
        )
    }
}

@Composable
fun DayOfWeekSelector(
    selectedDay: Int,
    todayDay: Int,
    daysWithPlannedExercises: Set<Int>,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Unique 2-letter abbreviations for each day
    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNum = index + 1
            val isSelected = selectedDay == dayNum
            val isToday = todayDay == dayNum
            val hasPlanned = daysWithPlannedExercises.contains(dayNum)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) White else CardGray)
                        .border(
                            width = if (isToday && !isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) White else if (isToday) LightGray else BorderGray,
                            shape = CircleShape
                        )
                        .clickable { onDaySelected(dayNum) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Black else LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Small dot under the day column: White if today, MediumGray if has planned exercises, Transparent otherwise
                val indicatorColor = if (isToday) {
                    White
                } else if (hasPlanned) {
                    MediumGray
                } else {
                    Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
        }
    }
}

@Composable
fun ExercisePlanCard(
    exercise: PlannedExercise,
    isLoggedToday: Boolean,
    onLog: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLoggedToday) Color(0xFF0A2A1A) else CardGray)
            .border(
                1.dp,
                if (isLoggedToday) Color(0xFF00E676).copy(alpha = 0.5f) else BorderGray,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = exercise.exerciseName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                if (isLoggedToday) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Logged today",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLoggedToday) "✓ Logged today  •  ${exercise.targetMuscle}" else exercise.targetMuscle,
                fontSize = 13.sp,
                color = if (isLoggedToday) Color(0xFF00E676).copy(alpha = 0.8f) else MediumGray
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Log", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(contentColor = MediumGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Planned Exercise",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddExerciseDialog(
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var targetMuscle by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardGray,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Add Exercise",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name", color = MediumGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = White,
                        unfocusedBorderColor = BorderGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Suggestion chips
                val filteredSuggestions = suggestions.filter {
                    it.contains(exerciseName, ignoreCase = true) && !it.equals(exerciseName, ignoreCase = true)
                }.take(3)
                if (filteredSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredSuggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BorderGray)
                                    .clickable { exerciseName = suggestion }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetMuscle,
                    onValueChange = { targetMuscle = it },
                    label = { Text("Target Muscle (e.g. Chest)", color = MediumGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = White,
                        unfocusedBorderColor = BorderGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = LightGray)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (exerciseName.isNotBlank()) {
                                onConfirm(exerciseName, targetMuscle)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = White,
                            contentColor = Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getDayName(day: Int): String {
    return when (day) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> ""
    }
}

private fun getDayShort(day: Int): String {
    return when (day) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardGray,
            border = BorderStroke(1.dp, BorderGray),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = LightGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = LightGray)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = White,
                            contentColor = Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

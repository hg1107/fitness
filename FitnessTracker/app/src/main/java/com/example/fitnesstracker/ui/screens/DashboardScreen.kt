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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Routine Plan",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // Weekday selector: Mon (1) to Sun (7)
        DayOfWeekSelector(
            selectedDay = selectedDay,
            onDaySelected = { viewModel.selectDay(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = getDayName(selectedDay),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LightGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exercises planned for today.\nTap '+' to add one.",
                        fontSize = 15.sp,
                        color = MediumGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(exercises) { exercise ->
                        ExercisePlanCard(
                            exercise = exercise,
                            onLog = { onLogExercise(exercise.exerciseName) },
                            onDelete = { viewModel.deletePlannedExercise(exercise) }
                        )
                    }
                }
            }

            // Minimalist floating add button inside the screen
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 8.dp)
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
            onDismiss = { showAddDialog = false },
            onConfirm = { name, muscle ->
                viewModel.addPlannedExercise(name, muscle)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DayOfWeekSelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayLabels.forEachIndexed { index, label ->
            val dayNum = index + 1
            val isSelected = selectedDay == dayNum
            
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) White else CardGray)
                    .border(1.dp, if (isSelected) White else BorderGray, CircleShape)
                    .clickable { onDaySelected(dayNum) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Black else LightGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExercisePlanCard(
    exercise: PlannedExercise,
    onLog: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.exerciseName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.targetMuscle,
                fontSize = 13.sp,
                color = MediumGray
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
        1 -> "Monday Workout"
        2 -> "Tuesday Workout"
        3 -> "Wednesday Workout"
        4 -> "Thursday Workout"
        5 -> "Friday Workout"
        6 -> "Saturday Workout"
        7 -> "Sunday Workout"
        else -> ""
    }
}

package com.example.fitnesstracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitnesstracker.data.WorkoutProgram
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.WorkoutViewModel
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val programs by viewModel.workoutPrograms.collectAsState(initial = emptyList())
    var selectedProgramToApply by remember { mutableStateOf<WorkoutProgram?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Workout Programs", fontWeight = FontWeight.Bold, color = White, fontSize = 18.sp)
                        Text("Select a pre-built split to load your routine", fontSize = 11.sp, color = MediumGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Black,
                    titleContentColor = White
                )
            )
        },
        containerColor = Black,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (programs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StravaOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp)
            ) {
                items(programs) { program ->
                    ProgramCard(
                        program = program,
                        onApplyClick = { selectedProgramToApply = program }
                    )
                }
            }
        }
    }

    selectedProgramToApply?.let { program ->
        ConfirmApplyDialog(
            programName = program.programName,
            onConfirm = {
                viewModel.applyWorkoutProgram(program)
                Toast.makeText(context, "${program.programName} routine applied!", Toast.LENGTH_SHORT).show()
                selectedProgramToApply = null
                onNavigateBack()
            },
            onDismiss = { selectedProgramToApply = null }
        )
    }
}

@Composable
fun ProgramCard(
    program: WorkoutProgram,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = program.programName,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = program.description,
                    color = LightGray,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = BorderGray)

            // Render program days preview
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val parsedDays = remember(program.daysJson) { parseDaysJson(program.daysJson) }
                parsedDays.forEach { day ->
                    Column {
                        Text(
                            text = getDayNameShort(day.dayOfWeek),
                            color = StravaOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.exercises.joinToString(", ") { it.name },
                            color = LightGray,
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onApplyClick,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
            ) {
                Text("Apply This Split", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ConfirmApplyDialog(
    programName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Overwrite Routine?", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text(
                "This will completely overwrite your current weekly planned workout routine with the \"$programName\" template. Are you sure?",
                color = LightGray,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Yes, Overwrite", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = LightGray)) {
                Text("Cancel")
            }
        },
        containerColor = CardGray
    )
}

// Helpers
private data class ParsedDay(val dayOfWeek: Int, val exercises: List<ParsedExercise>)
private data class ParsedExercise(val name: String, val muscle: String)

private fun parseDaysJson(daysJson: String): List<ParsedDay> {
    val list = mutableListOf<ParsedDay>()
    try {
        val array = JSONArray(daysJson)
        for (i in 0 until array.length()) {
            val dayObj = array.getJSONObject(i)
            val dayOfWeek = dayObj.getInt("dayOfWeek")
            val exercisesArr = dayObj.getJSONArray("exercises")
            val exercisesList = mutableListOf<ParsedExercise>()
            for (j in 0 until exercisesArr.length()) {
                val exObj = exercisesArr.getJSONObject(j)
                exercisesList.add(
                    ParsedExercise(
                        name = exObj.getString("name"),
                        muscle = exObj.getString("muscle")
                    )
                )
            }
            list.add(ParsedDay(dayOfWeek, exercisesList))
        }
    } catch (e: Exception) {
        // Safe empty list fallback
    }
    return list
}

private fun getDayNameShort(day: Int): String {
    return when (day) {
        1 -> "Monday Split"
        2 -> "Tuesday Split"
        3 -> "Wednesday Split"
        4 -> "Thursday Split"
        5 -> "Friday Split"
        6 -> "Saturday Split"
        7 -> "Sunday Split"
        else -> ""
    }
}

package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.theme.BorderGray
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.LightGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.util.ExerciseInfo
import java.util.Locale

/** Shows target muscles and form cues for the current exercise. */
@Composable
fun ExerciseInfoDialog(
    exerciseName: String,
    onDismiss: () -> Unit
) {
    val info = remember(exerciseName) { ExerciseInfo.lookup(exerciseName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardGray,
        title = {
            Text(exerciseName, color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target muscles", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(info.muscles, color = LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Form cues", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                info.cues.forEach { cue ->
                    Text("\u2022 $cue", color = LightGray, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/** Barbell plate calculator: shows which plates to load per side for a target weight. */
@Composable
fun PlateCalculatorDialog(
    isImperial: Boolean,
    onDismiss: () -> Unit
) {
    val unit = if (isImperial) "lbs" else "kg"
    val barOptions = if (isImperial) listOf(45.0, 35.0, 15.0) else listOf(20.0, 15.0, 10.0)
    val plateSizes = if (isImperial) {
        listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    } else {
        listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    }

    var targetText by remember { mutableStateOf("") }
    var barWeight by remember { mutableStateOf(barOptions.first()) }

    fun fmt(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

    val target = targetText.toDoubleOrNull() ?: 0.0
    val perSide = ((target - barWeight) / 2.0).coerceAtLeast(0.0)
    val result = remember(targetText, barWeight) {
        var remaining = ((target - barWeight) / 2.0).coerceAtLeast(0.0)
        val plates = mutableListOf<Double>()
        plateSizes.forEach { plate ->
            while (remaining >= plate - 0.001) {
                plates.add(plate)
                remaining -= plate
            }
        }
        plates.toList() to remaining
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardGray,
        title = {
            Text("Plate Calculator", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> targetText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Target weight ($unit)", fontSize = 12.sp, color = MediumGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = White,
                        unfocusedBorderColor = BorderGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Bar weight", fontSize = 12.sp, color = MediumGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    barOptions.forEach { option ->
                        FilterChip(
                            selected = barWeight == option,
                            onClick = { barWeight = option },
                            label = { Text("${fmt(option)} $unit", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = White,
                                selectedLabelColor = Color.Black,
                                labelColor = LightGray
                            )
                        )
                    }
                }

                HorizontalDivider(color = BorderGray, thickness = 1.dp)

                when {
                    target <= 0.0 -> Text(
                        "Enter a target weight to see the plate breakdown.",
                        color = MediumGray, fontSize = 13.sp
                    )
                    target < barWeight -> Text(
                        "Target is lighter than the bar (${fmt(barWeight)} $unit).",
                        color = Color(0xFFEF5350), fontSize = 13.sp
                    )
                    result.first.isEmpty() -> Text(
                        "Empty bar - no plates needed.",
                        color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Per side (${fmt(perSide)} $unit):",
                            color = MediumGray, fontSize = 12.sp
                        )
                        Text(
                            result.first.joinToString("  +  ") { fmt(it) },
                            color = Color(0xFF00E676), fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        if (result.second > 0.01) {
                            Text(
                                "${fmt(result.second)} $unit per side cannot be loaded with standard plates.",
                                color = Color(0xFFFFB74D), fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

package com.example.fitnesstracker.ui.screens.track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitnesstracker.data.UserProfile
import java.util.Locale

@Composable
fun SettingsConfigDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, age: Int, weightKg: Double, heightCm: Double, preferredUnits: String) -> Unit
) {
    // Local theme references to match TrackScreen styling
    val StravaOrange = com.example.fitnesstracker.theme.StravaOrange
    val DarkBackground = com.example.fitnesstracker.theme.Black
    val SurfaceCard = com.example.fitnesstracker.theme.CardGray
    val OutlinedBorder = com.example.fitnesstracker.theme.BorderGray
    val MutedText = com.example.fitnesstracker.theme.MediumGray
    val BrightText = com.example.fitnesstracker.theme.White

    var nameInput by remember { mutableStateOf(userProfile.name) }
    var ageInput by remember { mutableStateOf(userProfile.age.toString()) }
    var weightInput by remember { mutableStateOf(
        if (userProfile.preferredUnits == "Imperial") {
            String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.kgToLbs(userProfile.weightKg))
        } else {
            userProfile.weightKg.toString()
        }
    ) }
    var heightInput by remember { mutableStateOf(
        if (userProfile.preferredUnits == "Imperial") {
            String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.cmToInches(userProfile.heightCm))
        } else {
            userProfile.heightCm.toString()
        }
    ) }
    var unitInput by remember { mutableStateOf(userProfile.preferredUnits) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, OutlinedBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Profile & Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightText)
                
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name", color = MutedText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BrightText,
                        unfocusedTextColor = BrightText,
                        focusedBorderColor = StravaOrange,
                        unfocusedBorderColor = OutlinedBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Age", color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrightText,
                            unfocusedTextColor = BrightText,
                            focusedBorderColor = StravaOrange,
                            unfocusedBorderColor = OutlinedBorder
                        ),
                        modifier = Modifier.weight(1.5f)
                    )

                    Column(modifier = Modifier.weight(2f)) {
                        Text("Units", fontSize = 11.sp, color = MutedText, modifier = Modifier.padding(start = 4.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .border(1.dp, OutlinedBorder, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            listOf("Metric", "Imperial").forEach { unit ->
                                val isSelected = unitInput == unit
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) StravaOrange else Color.Transparent)
                                        .clickable { 
                                            val oldUnit = unitInput
                                            if (oldUnit != unit) {
                                                unitInput = unit
                                                val weightVal = weightInput.toDoubleOrNull() ?: 0.0
                                                val heightVal = heightInput.toDoubleOrNull() ?: 0.0
                                                if (unit == "Imperial") {
                                                    weightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.kgToLbs(weightVal))
                                                    heightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.cmToInches(heightVal))
                                                } else {
                                                    weightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.lbsToKg(weightVal))
                                                    heightInput = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.inchesToCm(heightVal))
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unit,
                                        color = if (isSelected) DarkBackground else BrightText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val weightLabel = if (unitInput == "Imperial") "Weight (lbs)" else "Weight (kg)"
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text(weightLabel, color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrightText,
                            unfocusedTextColor = BrightText,
                            focusedBorderColor = StravaOrange,
                            unfocusedBorderColor = OutlinedBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    val heightLabel = if (unitInput == "Imperial") "Height (in)" else "Height (cm)"
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text(heightLabel, color = MutedText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrightText,
                            unfocusedTextColor = BrightText,
                            focusedBorderColor = StravaOrange,
                            unfocusedBorderColor = OutlinedBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BrightText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalWeight = weightInput.toDoubleOrNull() ?: 70.0
                            val finalHeight = heightInput.toDoubleOrNull() ?: 175.0
                            val savedWeight = if (unitInput == "Imperial") com.example.fitnesstracker.util.UnitConverter.lbsToKg(finalWeight) else finalWeight
                            val savedHeight = if (unitInput == "Imperial") com.example.fitnesstracker.util.UnitConverter.inchesToCm(finalHeight) else finalHeight
                            
                            onSaveProfile(
                                nameInput,
                                ageInput.toIntOrNull() ?: 30,
                                savedWeight,
                                savedHeight,
                                unitInput
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StravaOrange, contentColor = DarkBackground)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

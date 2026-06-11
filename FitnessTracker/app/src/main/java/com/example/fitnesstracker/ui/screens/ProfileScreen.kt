package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.NutritionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: NutritionViewModel,
    onNavigateBack: () -> Unit
) {
    val userProfileState by viewModel.userProfile.collectAsState(initial = null)
    val profile = userProfileState ?: return

    // Editable state – pre-populated from profile
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var age by remember(profile.id) { mutableStateOf(profile.age.toString()) }
    var gender by remember(profile.id) { mutableStateOf(profile.gender) }
    var weightKg by remember(profile.id) { mutableStateOf(profile.weightKg.toString()) }
    var heightCm by remember(profile.id) { mutableStateOf(profile.heightCm.toString()) }
    var fitnessGoal by remember(profile.id) { mutableStateOf(profile.fitnessGoal) }
    var activityLevel by remember(profile.id) { mutableStateOf(profile.activityLevel) }
    var dietaryPreference by remember(profile.id) { mutableStateOf(profile.dietaryPreference) }
    var preferredUnits by remember(profile.id) { mutableStateOf(profile.preferredUnits) }
    var foodLikes by remember(profile.id) { mutableStateOf(profile.foodLikes) }
    var foodDislikes by remember(profile.id) { mutableStateOf(profile.foodDislikes) }
    var foodAllergies by remember(profile.id) { mutableStateOf(profile.foodAllergies) }
    var waterTargetMl by remember(profile.id) { mutableStateOf(profile.waterTargetMl.toString()) }

    var saveSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Profile & Settings", fontWeight = FontWeight.Bold, color = White, fontSize = 18.sp)
                        Text("Edit your personal details", fontSize = 11.sp, color = MediumGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateUserProfile(
                                name = name,
                                age = age.toIntOrNull() ?: profile.age,
                                gender = gender,
                                weightKg = weightKg.toDoubleOrNull() ?: profile.weightKg,
                                heightCm = heightCm.toDoubleOrNull() ?: profile.heightCm,
                                fitnessGoal = fitnessGoal,
                                activityLevel = activityLevel,
                                dietaryPreference = dietaryPreference,
                                preferredUnits = preferredUnits,
                                foodLikes = foodLikes,
                                foodDislikes = foodDislikes,
                                foodAllergies = foodAllergies,
                                waterTargetMl = waterTargetMl.toIntOrNull() ?: 3000
                            )
                            saveSuccess = true
                        }
                    ) {
                        Text("Save", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Black,
                    titleContentColor = White
                )
            )
        },
        containerColor = Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Section
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = CardGray,
                        modifier = Modifier.size(80.dp),
                        border = BorderStroke(2.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (saveSuccess) {
                        Text(
                            "✓ Profile saved",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Section: Personal Info
            ProfileSection(title = "Personal Info", icon = Icons.Default.Person) {
                ProfileTextField(label = "Name", value = name, onValueChange = { name = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileTextField(
                        label = "Age",
                        value = age,
                        onValueChange = { age = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileTextField(
                        label = "Weight (kg)",
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileTextField(
                        label = "Height (cm)",
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Gender selector
                Text("Gender", fontSize = 12.sp, color = MediumGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        ProfileChip(
                            label = g,
                            isSelected = gender == g,
                            onClick = { gender = g }
                        )
                    }
                }
            }

            // Section: Fitness Goals
            ProfileSection(title = "Fitness Goals", icon = Icons.Default.Star) {
                Text("Fitness Goal", fontSize = 12.sp, color = MediumGray)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Fat Loss", "Recomposition", "Weight Maintenance", "Lean Bulk", "Muscle Gain").forEach { goal ->
                        ProfileChip(
                            label = goal,
                            isSelected = fitnessGoal == goal,
                            onClick = { fitnessGoal = goal },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Activity Level", fontSize = 12.sp, color = MediumGray)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Athlete").forEach { level ->
                        ProfileChip(
                            label = level,
                            isSelected = activityLevel == level,
                            onClick = { activityLevel = level },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section: Diet & Nutrition Preferences
            ProfileSection(title = "Diet Preferences", icon = Icons.Default.Favorite) {
                Text("Dietary Preference", fontSize = 12.sp, color = MediumGray)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Non-Vegetarian", "Vegetarian", "Vegan",
                        "Eggetarian", "Pescatarian", "Jain Vegetarian"
                    ).forEach { pref ->
                        ProfileChip(
                            label = pref,
                            isSelected = dietaryPreference == pref,
                            onClick = { dietaryPreference = pref },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ProfileTextField(
                    label = "Food Likes (comma-separated)",
                    value = foodLikes,
                    onValueChange = { foodLikes = it }
                )
                ProfileTextField(
                    label = "Food Dislikes (comma-separated)",
                    value = foodDislikes,
                    onValueChange = { foodDislikes = it }
                )
                ProfileTextField(
                    label = "Food Allergies (comma-separated)",
                    value = foodAllergies,
                    onValueChange = { foodAllergies = it }
                )
            }

            // Section: App Settings
            ProfileSection(title = "App Settings", icon = Icons.Default.Settings) {
                Text("Preferred Units", fontSize = 12.sp, color = MediumGray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Metric", "Imperial").forEach { unit ->
                        ProfileChip(
                            label = unit,
                            isSelected = preferredUnits == unit,
                            onClick = { preferredUnits = unit }
                        )
                    }
                }

                ProfileTextField(
                    label = "Daily Water Target (ml)",
                    value = waterTargetMl,
                    onValueChange = { waterTargetMl = it },
                    keyboardType = KeyboardType.Number
                )
            }

            // Full-width save button at bottom
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    viewModel.updateUserProfile(
                        name = name,
                        age = age.toIntOrNull() ?: profile.age,
                        gender = gender,
                        weightKg = weightKg.toDoubleOrNull() ?: profile.weightKg,
                        heightCm = heightCm.toDoubleOrNull() ?: profile.heightCm,
                        fitnessGoal = fitnessGoal,
                        activityLevel = activityLevel,
                        dietaryPreference = dietaryPreference,
                        preferredUnits = preferredUnits,
                        foodLikes = foodLikes,
                        foodDislikes = foodDislikes,
                        foodAllergies = foodAllergies,
                        waterTargetMl = waterTargetMl.toIntOrNull() ?: 3000
                    )
                    saveSuccess = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
            HorizontalDivider(color = BorderGray, thickness = 1.dp)
            content()
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, color = MediumGray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = Color(0xFF00E676).copy(alpha = 0.7f),
            unfocusedBorderColor = BorderGray,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = modifier,
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun ProfileChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF00E676) else BorderGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF00E676) else LightGray
        )
    }
}

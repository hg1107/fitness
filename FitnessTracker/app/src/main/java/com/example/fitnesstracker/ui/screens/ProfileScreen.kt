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
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.NutritionViewModel
import com.example.fitnesstracker.util.BackupManager
import com.example.fitnesstracker.util.DataExporter
import com.example.fitnesstracker.util.HealthConnectManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: NutritionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
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
        modifier = modifier,
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

                // Live BMI indicator based on the values being edited
                val bmi = weightKg.toDoubleOrNull()?.let { w ->
                    heightCm.toDoubleOrNull()?.takeIf { it > 0 }?.let { h ->
                        w / ((h / 100.0) * (h / 100.0))
                    }
                }
                if (bmi != null) {
                    val category = when {
                        bmi < 18.5 -> "Underweight"
                        bmi < 25.0 -> "Healthy"
                        bmi < 30.0 -> "Overweight"
                        else -> "Obese"
                    }
                    Text(
                        text = String.format(java.util.Locale.US, "BMI: %.1f · %s", bmi, category),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
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

            // Section: Data Export
            val exportContext = LocalContext.current
            val exportScope = rememberCoroutineScope()
            ProfileSection(title = "Your Data", icon = Icons.Default.Share) {
                Text(
                    "Export your logs as CSV to back them up or analyze them in a spreadsheet.",
                    fontSize = 12.sp,
                    color = MediumGray
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { exportScope.launch { DataExporter.shareWorkoutsCsv(exportContext) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) { Text("Workouts", fontSize = 12.sp, color = White) }
                    OutlinedButton(
                        onClick = { exportScope.launch { DataExporter.shareNutritionCsv(exportContext) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) { Text("Nutrition", fontSize = 12.sp, color = White) }
                    OutlinedButton(
                        onClick = { exportScope.launch { DataExporter.shareActivitiesCsv(exportContext) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) { Text("Cardio", fontSize = 12.sp, color = White) }
                }

                HorizontalDivider(color = BorderGray, thickness = 1.dp)
                Text(
                    "Full backup: save or restore the entire database (all history).",
                    fontSize = 12.sp,
                    color = MediumGray
                )
                var dataMessage by remember { mutableStateOf("") }
                var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
                val backupLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/octet-stream")
                ) { uri ->
                    if (uri != null) {
                        exportScope.launch {
                            dataMessage = if (BackupManager.backupTo(exportContext, uri)) {
                                "Backup saved successfully."
                            } else {
                                "Backup failed."
                            }
                        }
                    }
                }
                val restoreLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) pendingRestoreUri = uri
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { backupLauncher.launch("fitnesstracker-backup.db") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) { Text("Backup", fontSize = 12.sp, color = White) }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGray)
                    ) { Text("Restore", fontSize = 12.sp, color = White) }
                }
                if (dataMessage.isNotEmpty()) {
                    Text(dataMessage, fontSize = 12.sp, color = Color(0xFF00E676))
                }
                if (pendingRestoreUri != null) {
                    AlertDialog(
                        onDismissRequest = { pendingRestoreUri = null },
                        containerColor = CardGray,
                        title = { Text("Restore backup?", color = White, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "This replaces ALL current data with the backup and restarts the app. This cannot be undone.",
                                color = LightGray,
                                fontSize = 13.sp
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val uri = pendingRestoreUri!!
                                pendingRestoreUri = null
                                exportScope.launch {
                                    if (BackupManager.restoreFrom(exportContext, uri)) {
                                        BackupManager.restartApp(exportContext)
                                    } else {
                                        dataMessage = "Restore failed: invalid backup file."
                                    }
                                }
                            }) { Text("Restore", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingRestoreUri = null }) {
                                Text("Cancel", color = MediumGray)
                            }
                        }
                    )
                }
            }

            // Section: Health Connect
            ProfileSection(title = "Health Connect", icon = Icons.Default.Favorite) {
                if (HealthConnectManager.isAvailable(exportContext)) {
                    var hcMessage by remember { mutableStateOf("") }
                    val hcPermissionLauncher = rememberLauncherForActivityResult(
                        PermissionController.createRequestPermissionResultContract()
                    ) { granted ->
                        hcMessage = if (granted.containsAll(HealthConnectManager.PERMISSIONS)) {
                            "Permissions granted. Tap Sync again."
                        } else {
                            "Health Connect permissions not granted."
                        }
                    }
                    Text(
                        "Push your recorded activities and latest weight to Health Connect so other health apps can see them.",
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                    Button(
                        onClick = {
                            exportScope.launch {
                                if (HealthConnectManager.hasAllPermissions(exportContext)) {
                                    val count = HealthConnectManager.syncAll(exportContext)
                                    hcMessage = "Synced $count new activit${if (count == 1) "y" else "ies"} + latest weight."
                                } else {
                                    hcPermissionLauncher.launch(HealthConnectManager.PERMISSIONS)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Sync to Health Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    if (hcMessage.isNotEmpty()) {
                        Text(hcMessage, fontSize = 12.sp, color = Color(0xFF00E676))
                    }
                } else {
                    Text(
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            "Health Connect requires Android 8.0 or newer."
                        } else {
                            "Health Connect is not available on this device."
                        },
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }
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

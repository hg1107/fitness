package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.theme.Black
import com.example.fitnesstracker.theme.BorderGray
import com.example.fitnesstracker.theme.CardGray
import com.example.fitnesstracker.theme.LightGray
import com.example.fitnesstracker.theme.MediumGray
import com.example.fitnesstracker.theme.White
import com.example.fitnesstracker.ui.NutritionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: NutritionViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val totalSteps = 7

    // Form state
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var weightStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var preferredUnits by remember { mutableStateOf("Metric") }
    var fitnessGoal by remember { mutableStateOf("Weight Maintenance") }
    var activityLevel by remember { mutableStateOf("Moderately Active") }
    var dietaryPreference by remember { mutableStateOf("Non-Vegetarian") }
    
    var foodLikes by remember { mutableStateOf("") }
    var foodDislikes by remember { mutableStateOf("") }
    var foodAllergies by remember { mutableStateOf("") }

    val isStepValid = when (step) {
        1 -> name.trim().isNotEmpty()
        2 -> age.toIntOrNull() != null && age.toInt() in 5..120
        3 -> {
            val w = weightStr.toDoubleOrNull()
            val h = heightStr.toDoubleOrNull()
            if (w == null || h == null) {
                false
            } else {
                if (preferredUnits == "Imperial") {
                    w in 44.0..660.0 && h in 20.0..100.0
                } else {
                    w in 20.0..300.0 && h in 50.0..250.0
                }
            }
        }
        4 -> true
        5 -> true
        6 -> true
        7 -> true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Fitness Tracker", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (step > 1) {
                        IconButton(onClick = { step-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F0C20),
                    Color(0xFF0D0A16)
                )
            )
        )
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // Progress indicators
            LinearProgressIndicator(
                progress = { step.toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = StravaOrange,
                trackColor = BorderGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Step $step of $totalSteps",
                color = MediumGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step contents (with transition animations)
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "OnboardingStepTransition"
                ) { targetStep ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top
                    ) {
                        when (targetStep) {
                            1 -> StepName(name = name, onNameChange = { name = it })
                            2 -> StepAgeGender(
                                age = age,
                                onAgeChange = { age = it },
                                selectedGender = gender,
                                onGenderChange = { gender = it }
                            )
                            3 -> StepStats(
                                weight = weightStr,
                                onWeightChange = { weightStr = it },
                                height = heightStr,
                                onHeightChange = { heightStr = it },
                                preferredUnits = preferredUnits,
                                onUnitsChange = { unit ->
                                    val w = weightStr.toDoubleOrNull()
                                    val h = heightStr.toDoubleOrNull()
                                    if (unit == "Imperial" && preferredUnits == "Metric") {
                                        if (w != null) weightStr = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.kgToLbs(w))
                                        if (h != null) heightStr = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.cmToInches(h))
                                    } else if (unit == "Metric" && preferredUnits == "Imperial") {
                                        if (w != null) weightStr = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.lbsToKg(w))
                                        if (h != null) heightStr = String.format(Locale.US, "%.1f", com.example.fitnesstracker.util.UnitConverter.inchesToCm(h))
                                    }
                                    preferredUnits = unit
                                }
                            )
                            4 -> StepGoal(
                                selectedGoal = fitnessGoal,
                                onGoalChange = { fitnessGoal = it }
                            )
                            5 -> StepActivity(
                                selectedActivity = activityLevel,
                                onActivityChange = { activityLevel = it }
                            )
                            6 -> StepDiet(
                                selectedDiet = dietaryPreference,
                                onDietChange = { dietaryPreference = it }
                            )
                            7 -> StepFoodPreferences(
                                likes = foodLikes,
                                onLikesChange = { foodLikes = it },
                                dislikes = foodDislikes,
                                onDislikesChange = { foodDislikes = it },
                                allergies = foodAllergies,
                                onAllergiesChange = { foodAllergies = it }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    if (step < totalSteps) {
                        step++
                    } else {
                        val savedWeight = if (preferredUnits == "Imperial") {
                            com.example.fitnesstracker.util.UnitConverter.lbsToKg(weightStr.toDouble())
                        } else {
                            weightStr.toDouble()
                        }
                        val savedHeight = if (preferredUnits == "Imperial") {
                            com.example.fitnesstracker.util.UnitConverter.inchesToCm(heightStr.toDouble())
                        } else {
                            heightStr.toDouble()
                        }
                        viewModel.saveUserProfile(
                            name = name.trim(),
                            age = age.toInt(),
                            gender = gender,
                            weightKg = savedWeight,
                            heightCm = savedHeight,
                            fitnessGoal = fitnessGoal,
                            activityLevel = activityLevel,
                            dietaryPreference = dietaryPreference,
                            foodLikes = foodLikes,
                            foodDislikes = foodDislikes,
                            foodAllergies = foodAllergies,
                            preferredUnits = preferredUnits
                        )
                        onComplete()
                    }
                },
                enabled = isStepValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StravaOrange,
                    contentColor = White,
                    disabledContainerColor = CardGray,
                    disabledContentColor = MediumGray
                )
            ) {
                Text(
                    text = if (step == totalSteps) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StepName(name: String, onNameChange: (String) -> Unit) {
    Text("Let's get started", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("What should we call you?", fontSize = 16.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Your Name", color = MediumGray) },
        placeholder = { Text("Enter your name", color = MediumGray) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )
}

@Composable
fun StepAgeGender(
    age: String,
    onAgeChange: (String) -> Unit,
    selectedGender: String,
    onGenderChange: (String) -> Unit
) {
    Text("Tell us about yourself", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Age and gender help us calculate your BMR accurately.", fontSize = 16.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = age,
        onValueChange = onAgeChange,
        label = { Text("Your Age", color = MediumGray) },
        placeholder = { Text("Enter age (e.g. 25)", color = MediumGray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text("Gender", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val genders = listOf("Male", "Female")
        genders.forEach { gender ->
            val isSelected = gender == selectedGender
            Card(
                onClick = { onGenderChange(gender) },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isSelected) StravaOrange else BorderGray),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CardGray else Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender,
                        color = if (isSelected) StravaOrange else MediumGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StepStats(
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    preferredUnits: String,
    onUnitsChange: (String) -> Unit
) {
    Text("What are your metrics?", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Height and weight will determine your calorie limits.", fontSize = 16.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(24.dp))

    // Unit selector
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardGray)
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .padding(2.dp)
    ) {
        listOf("Metric", "Imperial").forEach { unit ->
            val isSelected = preferredUnits == unit
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) StravaOrange else Color.Transparent)
                    .clickable { onUnitsChange(unit) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unit,
                    color = if (isSelected) Black else LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    val isImperial = preferredUnits == "Imperial"
    val weightLabel = if (isImperial) "Weight (lbs)" else "Weight (kg)"
    val weightPlaceholder = if (isImperial) "e.g. 160.0" else "e.g. 72.5"
    OutlinedTextField(
        value = weight,
        onValueChange = onWeightChange,
        label = { Text(weightLabel, color = MediumGray) },
        placeholder = { Text(weightPlaceholder, color = MediumGray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )

    Spacer(modifier = Modifier.height(20.dp))

    val heightLabel = if (isImperial) "Height (in)" else "Height (cm)"
    val heightPlaceholder = if (isImperial) "e.g. 70" else "e.g. 178"
    OutlinedTextField(
        value = height,
        onValueChange = onHeightChange,
        label = { Text(heightLabel, color = MediumGray) },
        placeholder = { Text(heightPlaceholder, color = MediumGray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )
}

@Composable
fun StepGoal(selectedGoal: String, onGoalChange: (String) -> Unit) {
    Text("What is your fitness goal?", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("We will adapt macro distributions accordingly.", fontSize = 15.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(20.dp))

    val goals = listOf(
        Triple("Fat Loss", "🎯 Fat Loss", "Caloric deficit focused on reducing body fat."),
        Triple("Weight Maintenance", "⚖️ Weight Maintenance", "Caloric balance to keep current weight."),
        Triple("Lean Bulk", "🪨 Lean Bulk", "Mild surplus for clean muscle gains."),
        Triple("Muscle Gain", "💪 Muscle Gain", "Caloric surplus for maximum muscle growth."),
        Triple("Recomposition", "🔄 Recomposition", "Focuses on building muscle while losing fat.")
    )

    goals.forEach { (goalKey, goalName, description) ->
        val isSelected = goalKey == selectedGoal
        Card(
            onClick = { onGoalChange(goalKey) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isSelected) StravaOrange else BorderGray),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CardGray else Color.Transparent
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goalName,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) StravaOrange else LightGray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = StravaOrange,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StepActivity(selectedActivity: String, onActivityChange: (String) -> Unit) {
    Text("What is your activity level?", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Helps accurately determine Total Daily Energy Expenditure (TDEE).", fontSize = 15.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(20.dp))

    val activities = listOf(
        Triple("Sedentary", "🛋️ Sedentary", "Little to no exercise, desk job."),
        Triple("Lightly Active", "🚶 Lightly Active", "Light exercise 1-3 days/week."),
        Triple("Moderately Active", "🏃 Moderately Active", "Moderate exercise 3-5 days/week."),
        Triple("Very Active", "🏋️ Very Active", "Hard exercise 6-7 days/week."),
        Triple("Athlete", "🏅 Athlete", "Twice daily training, heavy physical job.")
    )

    activities.forEach { (actKey, actName, description) ->
        val isSelected = actKey == selectedActivity
        Card(
            onClick = { onActivityChange(actKey) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isSelected) StravaOrange else BorderGray),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CardGray else Color.Transparent
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = actName,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) StravaOrange else LightGray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = StravaOrange,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StepDiet(selectedDiet: String, onDietChange: (String) -> Unit) {
    Text("Your dietary preference", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("We will filter out chicken/meat/dairy recommendations.", fontSize = 15.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(20.dp))

    val diets = listOf(
        Triple("Non-Vegetarian", "🍗 Non-Vegetarian", "Eat all kinds of foods including meats."),
        Triple("Vegetarian", "🥦 Vegetarian", "No meat or fish, egg and dairy are fine."),
        Triple("Vegan", "🌱 Vegan", "Strictly plant-based. No dairy, meat or egg."),
        Triple("Eggetarian", "🥚 Eggetarian", "No meat, but eggs and dairy are allowed."),
        Triple("Pescatarian", "🐟 Pescatarian", "Vegetarian diet but including seafood."),
        Triple("Jain Vegetarian", "📿 Jain Vegetarian", "Vegetarian diet strictly excluding root vegetables.")
    )

    diets.forEach { (dietKey, dietName, description) ->
        val isSelected = dietKey == selectedDiet
        Card(
            onClick = { onDietChange(dietKey) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isSelected) StravaOrange else BorderGray),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CardGray else Color.Transparent
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dietName,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) StravaOrange else LightGray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = StravaOrange,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StepFoodPreferences(
    likes: String,
    onLikesChange: (String) -> Unit,
    dislikes: String,
    onDislikesChange: (String) -> Unit,
    allergies: String,
    onAllergiesChange: (String) -> Unit
) {
    Text("Food preferences", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("We won't recommend foods you dislike or are allergic to.", fontSize = 16.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = likes,
        onValueChange = onLikesChange,
        label = { Text("Foods you like", color = MediumGray) },
        placeholder = { Text("e.g. Paneer, Eggs (comma separated)", color = MediumGray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = dislikes,
        onValueChange = onDislikesChange,
        label = { Text("Foods you dislike", color = MediumGray) },
        placeholder = { Text("e.g. Fish, Tofu", color = MediumGray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = allergies,
        onValueChange = onAllergiesChange,
        label = { Text("Allergies", color = MediumGray) },
        placeholder = { Text("e.g. Peanuts, Dairy", color = MediumGray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = StravaOrange,
            unfocusedBorderColor = BorderGray
        )
    )
}

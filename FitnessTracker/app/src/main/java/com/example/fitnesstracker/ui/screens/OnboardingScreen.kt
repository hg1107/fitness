package com.example.fitnesstracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    var fitnessGoal by remember { mutableStateOf("Weight Maintenance") }
    var activityLevel by remember { mutableStateOf("Moderately Active") }
    var dietaryPreference by remember { mutableStateOf("Non-Vegetarian") }
    
    var foodLikes by remember { mutableStateOf("") }
    var foodDislikes by remember { mutableStateOf("") }
    var foodAllergies by remember { mutableStateOf("") }

    val isStepValid = when (step) {
        1 -> name.trim().isNotEmpty()
        2 -> age.toIntOrNull() != null && age.toInt() in 5..120
        3 -> weightStr.toDoubleOrNull() != null && heightStr.toDoubleOrNull() != null && 
             weightStr.toDouble() in 20.0..300.0 && heightStr.toDouble() in 50.0..250.0
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
                    containerColor = Black,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = Black
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
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = White,
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
                                onHeightChange = { heightStr = it }
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
                        viewModel.saveUserProfile(
                            name = name.trim(),
                            age = age.toInt(),
                            gender = gender,
                            weightKg = weightStr.toDouble(),
                            heightCm = heightStr.toDouble(),
                            fitnessGoal = fitnessGoal,
                            activityLevel = activityLevel,
                            dietaryPreference = dietaryPreference,
                            foodLikes = foodLikes,
                            foodDislikes = foodDislikes,
                            foodAllergies = foodAllergies
                        )
                        onComplete()
                    }
                },
                enabled = isStepValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Black,
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
    Text("Let's get started", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = White)
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
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
    Text("Tell us about yourself", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = White)
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
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
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) White else BorderGray),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CardGray else Black
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender,
                        color = if (isSelected) White else MediumGray,
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
    onHeightChange: (String) -> Unit
) {
    Text("What are your metrics?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Height and weight will determine your calorie limits.", fontSize = 16.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = weight,
        onValueChange = onWeightChange,
        label = { Text("Weight (kg)", color = MediumGray) },
        placeholder = { Text("e.g. 72.5", color = MediumGray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
            unfocusedBorderColor = BorderGray
        )
    )

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedTextField(
        value = height,
        onValueChange = onHeightChange,
        label = { Text("Height (cm)", color = MediumGray) },
        placeholder = { Text("e.g. 178", color = MediumGray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
            unfocusedBorderColor = BorderGray
        )
    )
}

@Composable
fun StepGoal(selectedGoal: String, onGoalChange: (String) -> Unit) {
    Text("What is your fitness goal?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("We will adapt macro distributions accordingly.", fontSize = 15.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(20.dp))

    val goals = listOf(
        "Fat Loss" to "Caloric deficit focused on reducing body fat.",
        "Weight Maintenance" to "Caloric balance to keep current weight.",
        "Lean Bulk" to "Mild surplus for clean muscle gains.",
        "Muscle Gain" to "Caloric surplus for maximum muscle growth.",
        "Recomposition" to "Focuses on building muscle while losing fat."
    )

    goals.forEach { (goalName, description) ->
        val isSelected = goalName == selectedGoal
        Card(
            onClick = { onGoalChange(goalName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isSelected) White else BorderGray),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CardGray else Black
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
                        color = if (isSelected) White else LightGray,
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
                        tint = White,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StepActivity(selectedActivity: String, onActivityChange: (String) -> Unit) {
    Text("What is your activity level?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Helps accurately determine Total Daily Energy Expenditure (TDEE).", fontSize = 15.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(20.dp))

    val activities = listOf(
        "Sedentary" to "Little to no exercise, desk job.",
        "Lightly Active" to "Light exercise 1-3 days/week.",
        "Moderately Active" to "Moderate exercise 3-5 days/week.",
        "Very Active" to "Hard exercise 6-7 days/week.",
        "Athlete" to "Twice daily training, heavy physical job."
    )

    activities.forEach { (actName, description) ->
        val isSelected = actName == selectedActivity
        Card(
            onClick = { onActivityChange(actName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isSelected) White else BorderGray),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CardGray else Black
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
                        color = if (isSelected) White else LightGray,
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
                        tint = White,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StepDiet(selectedDiet: String, onDietChange: (String) -> Unit) {
    Text("Your dietary preference", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("We will filter out chicken/meat/dairy recommendations.", fontSize = 15.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(20.dp))

    val diets = listOf(
        "Non-Vegetarian" to "Eat all kinds of foods including meats.",
        "Vegetarian" to "No meat or fish, egg and dairy are fine.",
        "Vegan" to "Strictly plant-based. No dairy, meat or egg.",
        "Eggetarian" to "No meat, but eggs and dairy are allowed.",
        "Pescatarian" to "Vegetarian diet but including seafood.",
        "Jain Vegetarian" to "Vegetarian diet strictly excluding root vegetables."
    )

    diets.forEach { (dietName, description) ->
        val isSelected = dietName == selectedDiet
        Card(
            onClick = { onDietChange(dietName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isSelected) White else BorderGray),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) CardGray else Black
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
                        color = if (isSelected) White else LightGray,
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
                        tint = White,
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
    Text("Food preferences", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = White)
    Spacer(modifier = Modifier.height(8.dp))
    Text("We won't recommend foods you dislike or are allergic to.", fontSize = 16.sp, color = MediumGray)
    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = likes,
        onValueChange = onLikesChange,
        label = { Text("Foods you like", color = MediumGray) },
        placeholder = { Text("e.g. Paneer, Eggs (comma separated)", color = MediumGray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
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
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedBorderColor = White,
            unfocusedBorderColor = BorderGray
        )
    )
}

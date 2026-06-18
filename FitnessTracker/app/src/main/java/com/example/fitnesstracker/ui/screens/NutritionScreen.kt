package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.example.fitnesstracker.data.FoodLog
import com.example.fitnesstracker.data.SavedMeal
import com.example.fitnesstracker.data.WeightLog
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.ActivityViewModel
import com.example.fitnesstracker.ui.NutritionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel,
    activityViewModel: ActivityViewModel,
    onNavigateToSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val userProfileState by viewModel.userProfile.collectAsState(initial = null)
    val currentDate by viewModel.currentDate.collectAsState()
    val dateOffsetDays by viewModel.dateOffsetDays.collectAsState()
    val foodLogs by viewModel.todayFoodLogs.collectAsState(initial = emptyList())
    val waterAmount by viewModel.todayWaterAmount.collectAsState(initial = 0)
    val weightLogs by viewModel.allWeightLogs.collectAsState(initial = emptyList())
    val savedMeals by viewModel.allSavedMeals.collectAsState(initial = emptyList())
    val recommendations by viewModel.foodRecommendations.collectAsState(initial = emptyList())
    val todayCaloriesBurned by activityViewModel.todayCaloriesBurned.collectAsState(initial = 0.0)

    val profile = userProfileState ?: return

    val goal = viewModel.calculateGoal(
        gender = profile.gender,
        age = profile.age,
        weightKg = profile.weightKg,
        heightCm = profile.heightCm,
        activityLevel = profile.activityLevel,
        fitnessGoal = profile.fitnessGoal,
        customCalories = profile.customCalories,
        customProtein = profile.customProtein,
        customCarbs = profile.customCarbs,
        customFat = profile.customFat
    )

    // Logged totals
    val consumedCalories = foodLogs.sumOf { it.calories * it.quantity }
    val consumedProtein = foodLogs.sumOf { it.protein * it.quantity }
    val consumedCarbs = foodLogs.sumOf { it.carbs * it.quantity }
    val consumedFat = foodLogs.sumOf { it.fat * it.quantity }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header with date navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Diet & Nutrition",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Text(
                    text = "Fuel your body for performance",
                    fontSize = 14.sp,
                    color = MediumGray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardGray)
                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.goToPreviousDay() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Previous Day",
                    tint = White, modifier = Modifier.size(20.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { if (dateOffsetDays < 0) viewModel.goToToday() }
            ) {
                val displayDate = if (dateOffsetDays == 0) "Today" else if (dateOffsetDays == -1) "Yesterday" else {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentDate) ?: java.util.Date()
                    )
                }
                Text(displayDate, color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (dateOffsetDays < 0) {
                    Text("Tap to jump back to today", color = MediumGray, fontSize = 10.sp)
                }
            }
            IconButton(
                onClick = { viewModel.goToNextDay() },
                enabled = dateOffsetDays < 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Next Day",
                    tint = if (dateOffsetDays < 0) White else MediumGray,
                    modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress ring + linear macro bars row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGray),
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Calorie Progress Ring
                Box(
                    modifier = Modifier.weight(1.2f),
                    contentAlignment = Alignment.Center
                ) {
                    val progress = if (goal.calories > 0) (consumedCalories / goal.calories).toFloat() else 0f
                    val remaining = (goal.calories - consumedCalories).coerceAtLeast(0.0)
                    
                    Canvas(modifier = Modifier.size(130.dp)) {
                        drawArc(
                            color = BorderGray,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = Color(0xFF00E676),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${consumedCalories.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                        Text(
                            text = "of ${goal.calories.toInt()} kcal",
                            fontSize = 11.sp,
                            color = MediumGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (remaining > 0) "${remaining.toInt()} left" else "Met!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining > 0) Color(0xFF00E676).copy(alpha = 0.8f) else Color(0xFF4CAF50)
                        )
                    }
                }

                // Macro Progress Bars
                Column(
                    modifier = Modifier.weight(1.8f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroProgressBar(
                        label = "Protein",
                        consumed = consumedProtein,
                        goal = goal.protein,
                        color = Color(0xFFFF9800)
                    )
                    MacroProgressBar(
                        label = "Carbs",
                        consumed = consumedCarbs,
                        goal = goal.carbs,
                        color = Color(0xFF2196F3)
                    )
                    MacroProgressBar(
                        label = "Fat",
                        consumed = consumedFat,
                        goal = goal.fat,
                        color = Color(0xFFE91E63)
                    )
                    val consumedFiber = foodLogs.sumOf { it.fiber * it.quantity }
                    MacroProgressBar(
                        label = "Fiber",
                        consumed = consumedFiber,
                        goal = 25.0,
                        color = Color(0xFF8BC34A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Net Calorie Balance Card (only shown if user has burned calories today)
        if (todayCaloriesBurned > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2B1A)),
                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Net Calories", fontSize = 12.sp, color = MediumGray)
                        val netCals = consumedCalories - todayCaloriesBurned
                        Text(
                            text = "${netCals.toInt()} kcal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netCals < goal.calories) Color(0xFF00E676) else Color(0xFFEF5350)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Consumed", fontSize = 10.sp, color = MediumGray)
                        Text("${consumedCalories.toInt()} kcal", fontSize = 12.sp, color = White)
                        Text("Burned", fontSize = 10.sp, color = MediumGray)
                        Text("-${todayCaloriesBurned.toInt()} kcal", fontSize = 12.sp, color = Color(0xFFFF9800))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }



        Spacer(modifier = Modifier.height(20.dp))

        // Food Diary Timelines
        val meals = listOf("Breakfast", "Lunch", "Snack", "Dinner")
        meals.forEach { meal ->
            val mealLogs = foodLogs.filter { it.mealType.equals(meal, ignoreCase = true) }
            MealSection(
                mealName = meal,
                logs = mealLogs,
                savedMeals = savedMeals,
                onAddClick = { onNavigateToSearch(meal) },
                onDeleteLog = { viewModel.deleteFoodLog(it) },
                onSaveMeal = { name -> viewModel.saveAsCustomMeal(name, mealLogs) },
                onLoadSavedMeal = { savedMeal -> viewModel.loadSavedMeal(savedMeal, meal) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Recommendations Section
        if (recommendations.isNotEmpty()) {
            Text(
                text = "Recommended Foods",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations) { item ->
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGray),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = item.food.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                                Text(
                                    text = "${item.food.calories.toInt()} kcal per ${item.food.servingSizeG.toInt()}${item.food.servingUnit}",
                                    fontSize = 11.sp,
                                    color = MediumGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("P: ${item.food.protein.toInt()}g", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("C: ${item.food.carbs.toInt()}g", color = Color(0xFF2196F3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("F: ${item.food.fat.toInt()}g", color = Color(0xFFE91E63), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = item.reason,
                                fontSize = 10.sp,
                                color = LightGray,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Water Log Section
        val waterTarget = profile.waterTargetMl
        WaterLogSection(
            waterAmount = waterAmount,
            waterTargetMl = waterTarget,
            onLogWater = { viewModel.logWater(it) },
            onUndoWater = { viewModel.undoLastWaterLog() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Weight tracker section
        WeightLogSection(weightLogs = weightLogs, onSaveWeight = { viewModel.logWeight(it) })
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    consumed: Double,
    goal: Double,
    color: Color
) {
    val remaining = (goal - consumed).coerceAtLeast(0.0)
    val progress = if (goal > 0) (consumed / goal).coerceIn(0.0, 1.0) else 0.0

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "${consumed.toInt()}g / ${goal.toInt()}g",
                color = color.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = BorderGray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (remaining > 0) "${remaining.toInt()}g left" else "Met!",
            color = if (remaining > 0) color.copy(alpha = 0.7f) else Color(0xFF4CAF50),
            fontSize = 9.sp
        )
    }
}

@Composable
fun MealSection(
    mealName: String,
    logs: List<FoodLog>,
    savedMeals: List<SavedMeal>,
    onAddClick: () -> Unit,
    onDeleteLog: (FoodLog) -> Unit,
    onSaveMeal: (String) -> Unit,
    onLoadSavedMeal: (SavedMeal) -> Unit
) {
    var showSaveMealDialog by remember { mutableStateOf(false) }
    var showLoadMealSheet by remember { mutableStateOf(false) }
    var newMealName by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mealName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val totalCalories = logs.sumOf { it.calories * it.quantity }
                    if (totalCalories > 0) {
                        Text(
                            text = "${totalCalories.toInt()} kcal",
                            fontSize = 12.sp,
                            color = MediumGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = { showSaveMealDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Save meal", tint = MediumGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    // Load saved meal button
                    if (savedMeals.isNotEmpty()) {
                        IconButton(
                            onClick = { showLoadMealSheet = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Load saved meal", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add food", tint = White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "No food logged yet.",
                    color = MediumGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    logs.forEach { log ->
                        val qtyText = if (log.servingUnit.lowercase() == "g" || log.servingUnit.lowercase() == "ml") {
                            "${(log.quantity * 100).toInt()}${log.servingUnit}"
                        } else {
                            "${log.quantity} ${log.servingUnit}"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.foodName,
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("$qtyText • ")
                                        withStyle(SpanStyle(color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)) {
                                            append("P: ${(log.protein * log.quantity).toInt()}g")
                                        }
                                        append("  ")
                                        withStyle(SpanStyle(color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)) {
                                            append("C: ${(log.carbs * log.quantity).toInt()}g")
                                        }
                                        append("  ")
                                        withStyle(SpanStyle(color = Color(0xFFE91E63), fontWeight = FontWeight.Bold)) {
                                            append("F: ${(log.fat * log.quantity).toInt()}g")
                                        }
                                    },
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${(log.calories * log.quantity).toInt()} kcal",
                                    color = Color(0xFF00E676),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                IconButton(
                                    onClick = { onDeleteLog(log) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete entry",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveMealDialog) {
        AlertDialog(
            onDismissRequest = { showSaveMealDialog = false },
            title = { Text("Save as custom meal") },
            text = {
                OutlinedTextField(
                    value = newMealName,
                    onValueChange = { newMealName = it },
                    label = { Text("Meal Name") },
                    placeholder = { Text("e.g. Mass Gainer Breakfast") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveMeal(newMealName)
                        newMealName = ""
                        showSaveMealDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveMealDialog = false }) {
                    Text("Cancel", color = White)
                }
            },
            containerColor = CardGray
        )
    }

    // Saved Meals Loader Sheet
    if (showLoadMealSheet) {
        AlertDialog(
            onDismissRequest = { showLoadMealSheet = false },
            title = { Text("Load Saved Meal into $mealName", color = White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedMeals.forEach { meal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BorderGray)
                                .clickable {
                                    onLoadSavedMeal(meal)
                                    showLoadMealSheet = false
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(meal.mealName, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Load",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLoadMealSheet = false }) {
                    Text("Cancel", color = LightGray)
                }
            },
            containerColor = CardGray
        )
    }
}


@Composable
fun WaterLogSection(
    waterAmount: Int,
    waterTargetMl: Int,
    onLogWater: (Int) -> Unit,
    onUndoWater: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Water Tracking",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(modifier = Modifier.height(4.dp))
            val targetL = waterTargetMl / 1000.0
            Text(
                text = "Keep hydrated throughout the day. Target: ${String.format(Locale.getDefault(), "%.1f", targetL)} L.",
                fontSize = 12.sp,
                color = MediumGray
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Progress bar
            val waterProgress = (waterAmount.toFloat() / waterTargetMl).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { waterProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF2196F3),
                trackColor = BorderGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f L", waterAmount / 1000.0),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text("Logged Today", fontSize = 11.sp, color = MediumGray)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (waterAmount > 0) {
                        OutlinedButton(
                            onClick = onUndoWater,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MediumGray),
                            border = BorderStroke(1.dp, BorderGray),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { onLogWater(250) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A), contentColor = White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+250ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onLogWater(500) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8), contentColor = White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+500ml", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WeightLogSection(weightLogs: List<WeightLog>, onSaveWeight: (Double) -> Unit) {
    var weightInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Body Weight Logs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    placeholder = { Text("e.g. 71.2 kg", color = MediumGray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedBorderColor = White,
                        unfocusedBorderColor = BorderGray
                    )
                )

                Button(
                    onClick = {
                        val w = weightInput.toDoubleOrNull()
                        if (w != null && w > 0.0) {
                            onSaveWeight(w)
                            weightInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log", fontWeight = FontWeight.Bold)
                }
            }

            if (weightLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderGray)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Recent Logs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(weightLogs.take(10)) { log ->
                        val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(log.timestamp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Black),
                            border = BorderStroke(1.dp, BorderGray),
                            modifier = Modifier.width(80.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(dateStr, color = MediumGray, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${log.weightKg}kg", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.fitnesstracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.data.FoodItem
import com.example.fitnesstracker.data.SavedMeal
import com.example.fitnesstracker.theme.*
import com.example.fitnesstracker.ui.NutritionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    mealType: String,
    viewModel: NutritionViewModel,
    onNavigateBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Search DB, 1 = Recents, 2 = Saved Meals

    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())
    val recentFoods by viewModel.recentFoods.collectAsState(initial = emptyList())
    val savedMeals by viewModel.allSavedMeals.collectAsState(initial = emptyList())
    val foodLimit by viewModel.foodLimit.collectAsState()

    var foodToLog by remember { mutableStateOf<FoodItem?>(null) }
    var quantityInput by remember { mutableStateOf("100") }

    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        viewModel.updateSearchQuery(query)
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onDismiss = { showScanner = false },
            onFoodFound = { item ->
                viewModel.addScannedFood(item)
                showScanner = false
                selectedTab = 0
                query = item.name
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to $mealType", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search foods (rice, oats, paneer...)", color = MediumGray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MediumGray) },
                trailingIcon = {
                    IconButton(onClick = { showScanner = true }) {
                        Text("\uD83D\uDCF7", fontSize = 18.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = White,
                    unfocusedBorderColor = BorderGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs Header
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Black,
                contentColor = White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = White
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Search DB", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recents", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Saved Meals", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        if (searchResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No foods found.", color = MediumGray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(searchResults) { food ->
                                    FoodItemCard(
                                        food = food,
                                        onClick = {
                                            foodToLog = food
                                            quantityInput = if (food.servingUnit.lowercase() == "g" || food.servingUnit.lowercase() == "ml") {
                                                food.servingSizeG.toInt().toString()
                                            } else {
                                                "1"
                                            }
                                        }
                                    )
                                }
                                if (searchResults.size >= foodLimit) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TextButton(onClick = { viewModel.loadMoreFood() }) {
                                                Text("Load More", color = White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        if (recentFoods.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No recently logged foods.", color = MediumGray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(recentFoods) { foodName ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                // Quick log recent with a default search/lookup and log
                                                // We lookup the food and log 1 serving size or 100g
                                                viewModel.updateSearchQuery(foodName)
                                                selectedTab = 0
                                                query = foodName
                                            },
                                        colors = CardDefaults.cardColors(containerColor = CardGray),
                                        border = BorderStroke(1.dp, BorderGray)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(foodName, color = White, fontWeight = FontWeight.Bold)
                                            Text("Tap to edit & log", color = MediumGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        if (savedMeals.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No saved custom meals.", color = MediumGray)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(savedMeals) { meal ->
                                    SavedMealCard(
                                        meal = meal,
                                        onLogClick = {
                                            viewModel.logSavedMeal(meal, mealType)
                                            onNavigateBack()
                                        },
                                        onDeleteClick = {
                                            viewModel.deleteSavedMeal(meal)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Log food portion sizing dialog
    foodToLog?.let { food ->
        val qty = quantityInput.toDoubleOrNull() ?: 1.0
        val multiplier = if (food.servingUnit.lowercase() == "g" || food.servingUnit.lowercase() == "ml") {
            qty / food.servingSizeG
        } else {
            qty
        }

        AlertDialog(
            onDismissRequest = { foodToLog = null },
            title = { Text("Log portion size for ${food.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Quantity (${food.servingUnit})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )

                    // Live macros preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Black),
                        border = BorderStroke(1.dp, BorderGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Logged Nutrition Preview:", fontSize = 12.sp, color = MediumGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Calories: ${(food.calories * multiplier).toInt()} kcal", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("P: ${(food.protein * multiplier).toInt()}g", color = Color(0xFFFF9800), fontSize = 12.sp)
                                Text("C: ${(food.carbs * multiplier).toInt()}g", color = Color(0xFF2196F3), fontSize = 12.sp)
                                Text("F: ${(food.fat * multiplier).toInt()}g", color = Color(0xFFE91E63), fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = quantityInput.toDoubleOrNull()
                        if (q != null && q > 0.0) {
                            viewModel.logFood(mealType, food, multiplier)
                            foodToLog = null
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black)
                ) {
                    Text("Add Food")
                }
            },
            dismissButton = {
                TextButton(onClick = { foodToLog = null }) {
                    Text("Cancel", color = White)
                }
            },
            containerColor = CardGray
        )
    }
}

@Composable
fun FoodItemCard(food: FoodItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(food.name, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${food.calories.toInt()} kcal",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Serving: ${food.servingSizeG.toInt()}${food.servingUnit}",
                color = MediumGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Protein: ${food.protein.toInt()}g", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Carbs: ${food.carbs.toInt()}g", color = Color(0xFF2196F3), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Fat: ${food.fat.toInt()}g", color = Color(0xFFE91E63), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SavedMealCard(
    meal: SavedMeal,
    onLogClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGray),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.mealName, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val itemsCount = meal.foodLogsJson.split(";").size
                Text("$itemsCount foods included", color = MediumGray, fontSize = 12.sp)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete meal", tint = Color(0xFFEF5350))
                }
                Button(
                    onClick = onLogClick,
                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log Meal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

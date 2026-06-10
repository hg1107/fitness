package com.example.fitnesstracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.ai.client.generativeai.GenerativeModel


data class NutritionGoal(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class ParsedFoodLogItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val matchedFood: FoodItem?
)

data class RecommendedFoodItem(
    val food: FoodItem,
    val reason: String
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NutritionViewModel(
    private val nutritionDao: NutritionDao,
    private val activityDao: ActivityDao
) : ViewModel() {

    private val _currentDate = MutableStateFlow(getCurrentDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val userProfile: Flow<UserProfile?> = activityDao.getUserProfile()

    val coachMessages: Flow<List<CoachMessage>> = nutritionDao.getAllCoachMessages()

    private val _isCoachThinking = MutableStateFlow(false)
    val isCoachThinking: StateFlow<Boolean> = _isCoachThinking.asStateFlow()

    // Today's food logs
    val todayFoodLogs: Flow<List<FoodLog>> = _currentDate.flatMapLatest { date ->
        nutritionDao.getFoodLogsForDate(date)
    }

    // Today's water logs
    val todayWaterAmount: Flow<Int> = _currentDate.flatMapLatest { date ->
        nutritionDao.getWaterAmountForDate(date).map { it ?: 0 }
    }

    // All weight logs
    val allWeightLogs: Flow<List<WeightLog>> = nutritionDao.getAllWeightLogs()

    // Weekly food logs (last 7 days)
    val weeklyFoodLogs: Flow<List<FoodLog>> = nutritionDao.getAllFoodLogs().map { logs ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val last7Days = (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            sdf.format(cal.time)
        }.toSet()
        logs.filter { it.date in last7Days }
    }

    // Weekly water logs (last 7 days)
    val weeklyWaterLogs: Flow<List<WaterLog>> = nutritionDao.getAllWaterLogs().map { logs ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val last7Days = (0..6).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            sdf.format(cal.time)
        }.toSet()
        logs.filter { it.date in last7Days }
    }

    // Saved meals
    val allSavedMeals: Flow<List<SavedMeal>> = nutritionDao.getAllSavedMeals()

    // Recent food items logged by the user
    val recentFoods: Flow<List<String>> = nutritionDao.getRecentFoods()

    // Search results state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: Flow<List<FoodItem>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                nutritionDao.getAllFoodItems()
            } else {
                nutritionDao.searchFoodItems(query)
            }
        }

    // AI Entry Parsed state
    private val _parsedAIEntry = MutableStateFlow<List<ParsedFoodLogItem>>(emptyList())
    val parsedAIEntry: StateFlow<List<ParsedFoodLogItem>> = _parsedAIEntry.asStateFlow()

    // Recommendations state
    val foodRecommendations: Flow<List<RecommendedFoodItem>> = combine(
        nutritionDao.getAllFoodItems(),
        userProfile,
        todayFoodLogs
    ) { allFoods, profile, logs ->
        if (profile == null) return@combine emptyList()

        val goal = calculateGoal(
            gender = profile.gender,
            age = profile.age,
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            activityLevel = profile.activityLevel,
            fitnessGoal = profile.fitnessGoal
        )

        val consumedCalories = logs.sumOf { it.calories * it.quantity }
        val consumedProtein = logs.sumOf { it.protein * it.quantity }
        val consumedCarbs = logs.sumOf { it.carbs * it.quantity }
        val consumedFat = logs.sumOf { it.fat * it.quantity }

        val remainingCalories = (goal.calories - consumedCalories).coerceAtLeast(0.0)
        val remainingProtein = (goal.protein - consumedProtein).coerceAtLeast(0.0)
        val remainingCarbs = (goal.carbs - consumedCarbs).coerceAtLeast(0.0)
        val remainingFat = (goal.fat - consumedFat).coerceAtLeast(0.0)

        generateRecommendations(
            foods = allFoods,
            profile = profile,
            remainingCalories = remainingCalories,
            remainingProtein = remainingProtein,
            remainingCarbs = remainingCarbs,
            remainingFat = remainingFat
        )
    }

    init {
        seedFoodDatabaseIfEmpty()
    }

    fun selectDate(date: String) {
        _currentDate.value = date
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearParsedAIEntry() {
        _parsedAIEntry.value = emptyList()
    }

    // Save profile from Onboarding
    fun saveUserProfile(
        name: String,
        age: Int,
        gender: String,
        weightKg: Double,
        heightCm: Double,
        fitnessGoal: String,
        activityLevel: String,
        dietaryPreference: String,
        foodLikes: String,
        foodDislikes: String,
        foodAllergies: String
    ) {
        viewModelScope.launch {
            val profile = UserProfile(
                id = "default_user",
                name = name,
                age = age,
                gender = gender,
                weightKg = weightKg,
                heightCm = heightCm,
                fitnessGoal = fitnessGoal,
                activityLevel = activityLevel,
                dietaryPreference = dietaryPreference,
                onboardingComplete = true,
                foodLikes = foodLikes,
                foodDislikes = foodDislikes,
                foodAllergies = foodAllergies
            )
            activityDao.insertUserProfile(profile)
            
            // Also log initial weight log
            nutritionDao.insertWeightLog(
                WeightLog(
                    timestamp = System.currentTimeMillis(),
                    weightKg = weightKg
                )
            )
        }
    }

    // Add Food log
    fun logFood(
        mealType: String,
        foodItem: FoodItem,
        quantity: Double,
        date: String = _currentDate.value
    ) {
        viewModelScope.launch {
            nutritionDao.insertFoodLog(
                FoodLog(
                    date = date,
                    mealType = mealType,
                    foodName = foodItem.name,
                    calories = foodItem.calories,
                    protein = foodItem.protein,
                    carbs = foodItem.carbs,
                    fat = foodItem.fat,
                    fiber = foodItem.fiber,
                    quantity = quantity,
                    servingUnit = foodItem.servingUnit
                )
            )
        }
    }

    // Remove Food log
    fun deleteFoodLog(log: FoodLog) {
        viewModelScope.launch {
            nutritionDao.deleteFoodLog(log)
        }
    }

    // Water log
    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            nutritionDao.insertWaterLog(
                WaterLog(
                    date = _currentDate.value,
                    amountMl = amountMl
                )
            )
        }
    }

    // Log weight
    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            nutritionDao.insertWeightLog(
                WeightLog(
                    timestamp = System.currentTimeMillis(),
                    weightKg = weightKg
                )
            )
            // Update weight in profile
            val currentProfile = activityDao.getUserProfileSync()
            if (currentProfile != null) {
                activityDao.insertUserProfile(
                    currentProfile.copy(weightKg = weightKg)
                )
            }
        }
    }

    // Save current logged foods in a meal slot as a Saved Meal
    fun saveAsCustomMeal(mealName: String, logs: List<FoodLog>) {
        viewModelScope.launch {
            if (mealName.isNotBlank() && logs.isNotEmpty()) {
                val json = serializeLogsToJson(logs)
                nutritionDao.insertSavedMeal(
                    SavedMeal(
                        mealName = mealName,
                        foodLogsJson = json
                    )
                )
            }
        }
    }

    // Log a Saved Meal instantly
    fun logSavedMeal(meal: SavedMeal, mealType: String) {
        viewModelScope.launch {
            val logs = deserializeJsonToLogs(meal.foodLogsJson)
            for (log in logs) {
                nutritionDao.insertFoodLog(
                    FoodLog(
                        date = _currentDate.value,
                        mealType = mealType,
                        foodName = log.foodName,
                        calories = log.calories,
                        protein = log.protein,
                        carbs = log.carbs,
                        fat = log.fat,
                        fiber = log.fiber,
                        quantity = log.quantity,
                        servingUnit = log.servingUnit
                    )
                )
            }
        }
    }

    fun deleteSavedMeal(meal: SavedMeal) {
        viewModelScope.launch {
            nutritionDao.deleteSavedMeal(meal)
        }
    }

    // Offline heuristic-based AI text entry parser
    fun parseTextEntry(text: String) {
        viewModelScope.launch {
            val parsed = mutableListOf<ParsedFoodLogItem>()
            val segments = text.split(Regex(",|\\band\\b|\\+"), 0)

            for (segment in segments) {
                val trimmed = segment.trim()
                if (trimmed.isEmpty()) continue

                // Regex to extract numbers and units
                val regex = Regex("(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z]*)")
                val match = regex.find(trimmed)

                var quantity = 1.0
                var unit = "serving"
                var foodQuery = trimmed

                if (match != null) {
                    quantity = match.groupValues[1].toDoubleOrNull() ?: 1.0
                    val rawUnit = match.groupValues[2].trim().lowercase()
                    unit = if (rawUnit.isNotEmpty()) rawUnit else "serving"
                    foodQuery = trimmed.replace(match.value, "").trim()
                }

                // Remove filler words
                foodQuery = foodQuery.replace(
                    Regex("\\b(of|bowl|glass|cup|plate|piece|pieces|serving|servings|g|ml|scoop|scoops|tbsp|tsp)\\b", RegexOption.IGNORE_CASE),
                    ""
                ).trim()
                
                if (foodQuery.isEmpty()) {
                    foodQuery = trimmed
                }

                // Match in DB
                val matched = searchDbForFoodMatch(foodQuery)

                if (matched != null) {
                    val multiplier = when (unit) {
                        "g", "grams", "gram" -> {
                            if (matched.servingUnit.lowercase() == "g") {
                                quantity / matched.servingSizeG
                            } else {
                                quantity / 100.0
                            }
                        }
                        "ml", "mls" -> {
                            if (matched.servingUnit.lowercase() == "ml") {
                                quantity / matched.servingSizeG
                            } else {
                                quantity / 100.0
                            }
                        }
                        else -> quantity
                    }

                    parsed.add(
                        ParsedFoodLogItem(
                            name = matched.name,
                            quantity = quantity,
                            unit = if (unit == "serving") matched.servingUnit else unit,
                            calories = matched.calories * multiplier,
                            protein = matched.protein * multiplier,
                            carbs = matched.carbs * multiplier,
                            fat = matched.fat * multiplier,
                            matchedFood = matched
                        )
                    )
                } else {
                    parsed.add(
                        ParsedFoodLogItem(
                            name = foodQuery,
                            quantity = quantity,
                            unit = unit,
                            calories = 0.0,
                            protein = 0.0,
                            carbs = 0.0,
                            fat = 0.0,
                            matchedFood = null
                        )
                    )
                }
            }
            _parsedAIEntry.value = parsed
        }
    }

    // Log the AI parsed items to database
    fun logParsedAIEntry(mealType: String) {
        viewModelScope.launch {
            val items = _parsedAIEntry.value
            for (item in items) {
                if (item.matchedFood != null) {
                    val multiplier = when (item.unit) {
                        "g", "grams", "gram" -> {
                            if (item.matchedFood.servingUnit.lowercase() == "g") {
                                item.quantity / item.matchedFood.servingSizeG
                            } else {
                                item.quantity / 100.0
                            }
                        }
                        "ml", "mls" -> {
                            if (item.matchedFood.servingUnit.lowercase() == "ml") {
                                item.quantity / item.matchedFood.servingSizeG
                            } else {
                                item.quantity / 100.0
                            }
                        }
                        else -> item.quantity
                    }
                    
                    nutritionDao.insertFoodLog(
                        FoodLog(
                            date = _currentDate.value,
                            mealType = mealType,
                            foodName = item.matchedFood.name,
                            calories = item.matchedFood.calories,
                            protein = item.matchedFood.protein,
                            carbs = item.matchedFood.carbs,
                            fat = item.matchedFood.fat,
                            fiber = item.matchedFood.fiber,
                            quantity = multiplier,
                            servingUnit = item.matchedFood.servingUnit
                        )
                    )
                }
            }
            clearParsedAIEntry()
        }
    }

    private suspend fun searchDbForFoodMatch(query: String): FoodItem? {
        val list = nutritionDao.getFoodItemByName(query)
        if (list != null) return list

        // Fallback: search query contains item or vice versa
        val allFoods = nutritionDao.searchFoodItems(query).firstOrNull() ?: emptyList()
        return allFoods.firstOrNull { food ->
            food.name.contains(query, ignoreCase = true) || query.contains(food.name, ignoreCase = true)
        } ?: allFoods.firstOrNull()
    }

    // Calculations & Utilities
    fun calculateGoal(
        gender: String,
        age: Int,
        weightKg: Double,
        heightCm: Double,
        activityLevel: String,
        fitnessGoal: String
    ): NutritionGoal {
        val bmr = if (gender.equals("Male", ignoreCase = true)) {
            10 * weightKg + 6.25 * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }

        val multiplier = when (activityLevel) {
            "Sedentary" -> 1.2
            "Lightly Active" -> 1.375
            "Moderately Active" -> 1.55
            "Very Active" -> 1.725
            "Athlete" -> 1.9
            else -> 1.55
        }

        val tdee = bmr * multiplier

        val calories = when (fitnessGoal) {
            "Fat Loss" -> tdee - 500
            "Lean Bulk" -> tdee + 300
            "Muscle Gain" -> tdee + 500
            "Weight Maintenance", "Recomposition" -> tdee
            else -> tdee
        }

        val protein = weightKg * 2.0
        val fat = (calories * 0.25) / 9.0
        val carbs = (calories - (protein * 4) - (fat * 9)) / 4.0

        return NutritionGoal(
            calories = calories.coerceAtLeast(1200.0),
            protein = protein.coerceAtLeast(40.0),
            carbs = carbs.coerceAtLeast(50.0),
            fat = fat.coerceAtLeast(30.0)
        )
    }

    private fun filterFoodsForUser(foods: List<FoodItem>, profile: UserProfile): List<FoodItem> {
        val dislikeList = profile.foodDislikes.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val allergyList = profile.foodAllergies.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

        return foods.filter { food ->
            // Dietary Preferences
            val dietOk = when (profile.dietaryPreference) {
                "Vegetarian" -> food.isVegetarian
                "Vegan" -> food.isVegan
                "Jain Vegetarian" -> {
                    food.isVegetarian && 
                            !food.name.contains("potato", ignoreCase = true) && 
                            !food.name.contains("onion", ignoreCase = true) && 
                            !food.name.contains("garlic", ignoreCase = true)
                }
                "Pescatarian" -> food.isVegetarian || food.name.contains("fish", ignoreCase = true) || food.name.contains("salmon", ignoreCase = true)
                "Eggetarian" -> food.isVegetarian || food.name.contains("egg", ignoreCase = true)
                else -> true // Non-Vegetarian / default
            }

            val nameLower = food.name.lowercase()
            val allergenLower = food.allergens.lowercase()

            val notDisliked = dislikeList.none { nameLower.contains(it) }
            val notAllergic = allergyList.none { nameLower.contains(it) || allergenLower.contains(it) }

            dietOk && notDisliked && notAllergic
        }
    }

    private fun generateRecommendations(
        foods: List<FoodItem>,
        profile: UserProfile,
        remainingCalories: Double,
        remainingProtein: Double,
        remainingCarbs: Double,
        remainingFat: Double
    ): List<RecommendedFoodItem> {
        val filtered = filterFoodsForUser(foods, profile)
        
        return filtered.map { food ->
            var score = 0.0
            var reason = ""

            val isLowCalGoal = profile.fitnessGoal == "Fat Loss" || profile.fitnessGoal == "Recomposition"
            val isHighCalGoal = profile.fitnessGoal == "Lean Bulk" || profile.fitnessGoal == "Muscle Gain"

            // Protein priority
            if (remainingProtein > 15.0 && food.protein > 5.0) {
                val proteinDensity = (food.protein * 4.0) / food.calories.coerceAtLeast(1.0)
                score += proteinDensity * 60.0
                reason = "High protein (${(food.protein).toInt()}g) to help complete your remaining protein goal."
            }

            // Fitness goal alignment
            if (isLowCalGoal) {
                if (food.calories < 120.0) {
                    score += 25.0
                    if (reason.isEmpty()) reason = "Low-calorie, filling food matching your Fat Loss goal."
                }
            } else if (isHighCalGoal) {
                if (food.calories > 200.0) {
                    score += 25.0
                    if (reason.isEmpty()) reason = "Calorie-dense nutrition to help meet your bulking requirements."
                }
            }

            // Remaining Carb/Fat requirements
            if (remainingCarbs > 30.0 && food.carbs > 15.0) {
                score += 15.0
                if (reason.isEmpty()) reason = "Healthy source of energy carbs to replenish your glycogen."
            }

            if (remainingFat > 15.0 && food.fat > 8.0) {
                score += 10.0
                if (reason.isEmpty()) reason = "Good fat profile to help hit your daily fat requirements."
            }

            if (reason.isEmpty()) {
                reason = "A nutritionally balanced selection matching your dietary settings."
            }

            Pair(food, score to reason)
        }
        .sortedByDescending { it.second.first }
        .take(5)
        .map { RecommendedFoodItem(it.first, it.second.second) }
    }

    private fun seedFoodDatabaseIfEmpty() {
        viewModelScope.launch {
            val count = nutritionDao.getAllFoodItems().firstOrNull()?.size ?: 0
            if (count == 0) {
                nutritionDao.insertFoodItems(SEED_FOODS)
            }
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Manual quick serialization/deserialization for food logs list in saved meals
    private fun serializeLogsToJson(logs: List<FoodLog>): String {
        // Since we don't have Gson/Moshi in imports, we can construct simple CSV/JSON or string serialization.
        // String format: foodName|calories|protein|carbs|fat|fiber|quantity|servingUnit;foodName|...
        return logs.joinToString(";") {
            "${it.foodName}|${it.calories}|${it.protein}|${it.carbs}|${it.fat}|${it.fiber}|${it.quantity}|${it.servingUnit}"
        }
    }

    private fun deserializeJsonToLogs(serialized: String): List<FoodLog> {
        if (serialized.isBlank()) return emptyList()
        return try {
            serialized.split(";").map { item ->
                val parts = item.split("|")
                FoodLog(
                    date = "",
                    mealType = "",
                    foodName = parts[0],
                    calories = parts[1].toDouble(),
                    protein = parts[2].toDouble(),
                    carbs = parts[3].toDouble(),
                    fat = parts[4].toDouble(),
                    fiber = parts[5].toDouble(),
                    quantity = parts[6].toDouble(),
                    servingUnit = parts[7]
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCoachSettings(
        budget: String,
        region: String,
        preferredCuisine: String,
        mealTimings: String,
        gymSchedule: String,
        availableFoodsAtHome: String,
        geminiApiKey: String
    ) {
        viewModelScope.launch {
            val currentProfile = activityDao.getUserProfileSync() ?: UserProfile()
            val updatedProfile = currentProfile.copy(
                budget = budget,
                region = region,
                preferredCuisine = preferredCuisine,
                mealTimings = mealTimings,
                gymSchedule = gymSchedule,
                availableFoodsAtHome = availableFoodsAtHome,
                geminiApiKey = geminiApiKey
            )
            activityDao.insertUserProfile(updatedProfile)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            nutritionDao.clearAllCoachMessages()
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val userMessage = CoachMessage(
                timestamp = System.currentTimeMillis(),
                sender = "user",
                text = messageText
            )
            nutritionDao.insertCoachMessage(userMessage)
            
            _isCoachThinking.value = true
            
            val profile = activityDao.getUserProfileSync() ?: UserProfile()
            val logs = nutritionDao.getAllFoodLogs().firstOrNull() ?: emptyList()
            
            val todayStr = getCurrentDateString()
            val todayLogs = logs.filter { it.date == todayStr }
            
            val goal = calculateGoal(
                gender = profile.gender,
                age = profile.age,
                weightKg = profile.weightKg,
                heightCm = profile.heightCm,
                activityLevel = profile.activityLevel,
                fitnessGoal = profile.fitnessGoal
            )
            
            val consumedCal = todayLogs.sumOf { it.calories * it.quantity }
            val consumedProt = todayLogs.sumOf { it.protein * it.quantity }
            val consumedCarb = todayLogs.sumOf { it.carbs * it.quantity }
            val consumedFat = todayLogs.sumOf { it.fat * it.quantity }
            val consumedFib = todayLogs.sumOf { it.fiber * it.quantity }
            
            val remainingCal = (goal.calories - consumedCal).coerceAtLeast(0.0)
            val remainingProt = (goal.protein - consumedProt).coerceAtLeast(0.0)
            val remainingCarb = (goal.carbs - consumedCarb).coerceAtLeast(0.0)
            val remainingFat = (goal.fat - consumedFat).coerceAtLeast(0.0)
            val remainingFib = (25.0 - consumedFib).coerceAtLeast(0.0)
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last30Days = (0..29).map { i ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                sdf.format(cal.time)
            }.toSet()
            val past30DaysLogs = logs.filter { it.date in last30Days }
            val uniquePastFoods = past30DaysLogs.map { it.foodName }.distinct().take(10).joinToString(", ")
            
            val recentMessages = nutritionDao.getAllCoachMessages().firstOrNull()?.takeLast(6) ?: emptyList()
            val chatHistoryStr = recentMessages.joinToString("\n") { 
                if (it.sender == "user") "User: ${it.text}" else "Coach: ${it.text}" 
            }
            
            var replyText = ""
            
            if (profile.geminiApiKey.isNotBlank()) {
                try {
                    val prompt = buildSystemPrompt(
                        profile = profile,
                        goal = goal,
                        consumedCal = consumedCal,
                        consumedProt = consumedProt,
                        consumedCarb = consumedCarb,
                        consumedFat = consumedFat,
                        consumedFib = consumedFib,
                        remainingCal = remainingCal,
                        remainingProt = remainingProt,
                        remainingCarb = remainingCarb,
                        remainingFat = remainingFat,
                        remainingFib = remainingFib,
                        uniquePastFoods = uniquePastFoods,
                        chatHistory = chatHistoryStr,
                        userQuery = messageText
                    )
                    
                    val model = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = profile.geminiApiKey
                    )
                    val response = model.generateContent(prompt)
                    replyText = response.text ?: ""
                } catch (e: Exception) {
                    replyText = generateOfflineCoachResponse(
                        query = messageText,
                        profile = profile,
                        goal = goal,
                        consumedCal = consumedCal,
                        consumedProt = consumedProt,
                        consumedCarb = consumedCarb,
                        consumedFat = consumedFat,
                        consumedFib = consumedFib,
                        remainingCal = remainingCal,
                        remainingProt = remainingProt,
                        remainingCarb = remainingCarb,
                        remainingFat = remainingFat,
                        remainingFib = remainingFib,
                        uniquePastFoods = uniquePastFoods,
                        errorMsg = "Note: Gemini API failed (${e.localizedMessage})."
                    )
                }
            } else {
                replyText = generateOfflineCoachResponse(
                    query = messageText,
                    profile = profile,
                    goal = goal,
                    consumedCal = consumedCal,
                    consumedProt = consumedProt,
                    consumedCarb = consumedCarb,
                    consumedFat = consumedFat,
                    consumedFib = consumedFib,
                    remainingCal = remainingCal,
                    remainingProt = remainingProt,
                    remainingCarb = remainingCarb,
                    remainingFat = remainingFat,
                    remainingFib = remainingFib,
                    uniquePastFoods = uniquePastFoods
                )
            }
            
            if (replyText.isBlank()) {
                replyText = "Sorry, I am unable to analyze your request right now. Please try again."
            }
            
            val coachMessage = CoachMessage(
                timestamp = System.currentTimeMillis(),
                sender = "coach",
                text = replyText
            )
            nutritionDao.insertCoachMessage(coachMessage)
            _isCoachThinking.value = false
        }
    }

    private fun buildSystemPrompt(
        profile: UserProfile,
        goal: NutritionGoal,
        consumedCal: Double,
        consumedProt: Double,
        consumedCarb: Double,
        consumedFat: Double,
        consumedFib: Double,
        remainingCal: Double,
        remainingProt: Double,
        remainingCarb: Double,
        remainingFat: Double,
        remainingFib: Double,
        uniquePastFoods: String,
        chatHistory: String,
        userQuery: String
    ): String {
        return """
            You are an intelligent Nutrition Coach integrated into a fitness tracking application.
            Your primary objective is to help users achieve their fitness goals through personalized nutrition guidance based on their profile, goals, activity levels, food intake history, and dietary preferences.
            
            User Context:
            - Name: ${profile.name}
            - Age: ${profile.age}
            - Gender: ${profile.gender}
            - Height: ${profile.heightCm} cm
            - Weight: ${profile.weightKg} kg
            - Fitness Goal: ${profile.fitnessGoal}
            - Activity Level: ${profile.activityLevel}
            - Dietary Preference: ${profile.dietaryPreference}
            - Food Allergies: ${profile.foodAllergies}
            - Food Dislikes: ${profile.foodDislikes}
            - Budget Level: ${profile.budget} (student, moderate, premium)
            - Region: ${profile.region}
            - Preferred Cuisine: ${profile.preferredCuisine}
            - Meal Timings: ${profile.mealTimings}
            - Gym Schedule: ${profile.gymSchedule}
            - Available Foods at Home: ${profile.availableFoodsAtHome}
            - Commonly Eaten recently: $uniquePastFoods
            
            Daily Nutrition Targets:
            - Calories Goal: ${goal.calories.toInt()} kcal (Consumed: ${consumedCal.toInt()}, Remaining: ${remainingCal.toInt()})
            - Protein Goal: ${goal.protein.toInt()} g (Consumed: ${consumedProt.toInt()}, Remaining: ${remainingProt.toInt()})
            - Carbohydrate Goal: ${goal.carbs.toInt()} g (Consumed: ${consumedCarb.toInt()}, Remaining: ${remainingCarb.toInt()})
            - Fat Goal: ${goal.fat.toInt()} g (Consumed: ${consumedFat.toInt()}, Remaining: ${remainingFat.toInt()})
            - Fiber Goal: 25 g (Consumed: ${consumedFib.toInt()}, Remaining: ${remainingFib.toInt()})
            
            Safety Rules:
            - Never provide medical diagnoses, treatment plans, extreme diets, dangerous calorie deficits/surpluses, or eating disorder advice.
            - If medical advice is required, advise consulting a qualified healthcare professional.
            
            Instructions:
            - Respond to the user's latest message naturally and conversationally, taking the chat history into account.
            - Use the context provided above to give personalized advice.
            - Provide specific food or meal recommendations when appropriate, considering the user's macros, budget, and region.
            - You can use markdown to format your response (e.g., bolding, bullet points, headers).
            - Do not output a rigidly structured template unless it makes sense for the user's request. Be dynamic, conversational, and directly answer the user's question.
            
            Recent Chat History:
            $chatHistory
            
            User's latest message: "$userQuery"
        """.trimIndent()
    }

    private fun generateOfflineCoachResponse(
        query: String,
        profile: UserProfile,
        goal: NutritionGoal,
        consumedCal: Double,
        consumedProt: Double,
        consumedCarb: Double,
        consumedFat: Double,
        consumedFib: Double,
        remainingCal: Double,
        remainingProt: Double,
        remainingCarb: Double,
        remainingFat: Double,
        remainingFib: Double,
        uniquePastFoods: String,
        errorMsg: String = ""
    ): String {
        val lowercaseQuery = query.lowercase()
        val isSubstitution = lowercaseQuery.contains("replace") || lowercaseQuery.contains("substitute") || lowercaseQuery.contains("instead of") || lowercaseQuery.contains("alternative") || lowercaseQuery.contains("→")
        val isProteinCheck = lowercaseQuery.contains("protein") || lowercaseQuery.contains("enough protein") || lowercaseQuery.contains("hit my protein")
        val isGoalCheck = lowercaseQuery.contains("cutting") || lowercaseQuery.contains("bulking") || lowercaseQuery.contains("bulk") || lowercaseQuery.contains("cut") || lowercaseQuery.contains("goal")
        val isMealRequest = lowercaseQuery.contains("breakfast") || lowercaseQuery.contains("lunch") || lowercaseQuery.contains("dinner") || lowercaseQuery.contains("snack") || lowercaseQuery.contains("recipe") || lowercaseQuery.contains("meal") || lowercaseQuery.contains("eat tonight")

        val statusText = buildString {
            if (errorMsg.isNotEmpty()) {
                append("$errorMsg\n\n")
            } else if (profile.geminiApiKey.isBlank()) {
                append("*(Offline Mode: Please add a Gemini API Key in settings for dynamic chat!)*\n\n")
            }
            append("You are a ${profile.age}-year-old ${profile.gender} from the ${profile.region} region. ")
            append("For your goal of **${profile.fitnessGoal}**, your targets are **${goal.calories.toInt()} kcal** and **${goal.protein.toInt()}g protein**. ")
            append("Today you have consumed **${consumedCal.toInt()} kcal** and **${consumedProt.toInt()}g protein**. ")
            
            if (remainingProt > 0.0) {
                append("You are currently short of your protein goal by **${remainingProt.toInt()}g**. ")
            } else {
                append("Awesome! You've met your protein target for today! ")
            }

            if (isSubstitution) {
                append("Analyzing your food substitution request to maintain your nutritional targets.")
            } else if (isProteinCheck) {
                append("Focusing on optimizing your protein intake density.")
            } else if (isGoalCheck) {
                append("Evaluating your meal distributions against your **${profile.fitnessGoal}** guidelines.")
            } else {
                append("Providing daily analysis and foods tailored to your **${profile.budget}** budget and **${profile.preferredCuisine}** cuisine.")
            }
        }

        val recommendedFoodsText = buildString {
            val isVeg = profile.dietaryPreference.equals("Vegetarian", ignoreCase = true) || 
                        profile.dietaryPreference.equals("Vegan", ignoreCase = true) ||
                        profile.dietaryPreference.equals("Jain Vegetarian", ignoreCase = true)
            
            if (isVeg) {
                append("- **Paneer (Cottage Cheese)** (Serving: 150g):\n")
                append("  - Calories: 398 kcal, P: 27.5g, C: 9g, F: 31.2g\n")
                append("  - Reason: Fits your preferred ${profile.preferredCuisine} cuisine, vegetarian preference, and is available at home.\n")
                
                append("- **Soy Chunks** (Serving: 50g dry):\n")
                append("  - Calories: 172 kcal, P: 26g, C: 16.5g, F: 0.2g\n")
                append("  - Reason: High protein density, extremely student-budget friendly, and quick to cook.\n")
                
                append("- **Greek Yogurt** (Serving: 200g):\n")
                append("  - Calories: 118 kcal, P: 20g, C: 7.2g, F: 0.8g\n")
                append("  - Reason: Clean, low-fat source of high quality protein and calcium.")
            } else {
                append("- **Chicken Breast (Grilled)** (Serving: 150g):\n")
                append("  - Calories: 247 kcal, P: 46.5g, C: 0g, F: 5.4g\n")
                append("  - Reason: Extremely lean source of complete protein. Ideal for ${profile.fitnessGoal}.\n")
                
                append("- **Egg Whites** (Serving: 4 pieces):\n")
                append("  - Calories: 68 kcal, P: 14.4g, C: 0.8g, F: 0.4g\n")
                append("  - Reason: Cheap, low-calorie protein source fitting your moderate budget and gym routine.\n")
                
                append("- **Oats** (Serving: 60g):\n")
                append("  - Calories: 233 kcal, P: 10.1g, C: 39.8g, F: 4.1g\n")
                append("  - Reason: Great source of energy carbs and fiber to support your workout program.")
            }
        }

        val suggestedMealText = buildString {
            if (isSubstitution) {
                if (lowercaseQuery.contains("chicken")) {
                    append("**Tofu / Paneer Rice Bowl (Chicken Alternative)**\n")
                    append("- Ingredients: 150g Paneer (or 200g Tofu), 150g Cooked Rice, 100g Mixed Vegetables (broccoli/carrots).\n")
                    append("- Macros: Calories: 550 kcal, Protein: 30g, Carbs: 58g, Fat: 22g, Fiber: 4g\n")
                    append("- Note: Added paneer to preserve protein intent, though fat content will be slightly higher than chicken.")
                } else if (lowercaseQuery.contains("milk")) {
                    append("**Skim Milk Oats (Milk Alternative)**\n")
                    append("- Ingredients: 50g Oats, 250ml Skim Milk, 1 sliced Banana.\n")
                    append("- Macros: Calories: 320 kcal, Protein: 14g, Carbs: 58g, Fat: 2g, Fiber: 6g")
                } else {
                    append("**Healthy Substitution Plate**\n")
                    append("- Ingredients: 150g Sweet Potato (instead of white rice), 150g Paneer/Tofu (instead of meat), 1 plate cucumber salad.\n")
                    append("- Macros: Calories: 490 kcal, Protein: 28g, Carbs: 45g, Fat: 21g, Fiber: 5g")
                }
            } else if (isProteinCheck) {
                append("**High Protein Recovery Meal**\n")
                append("- Ingredients: 150g Paneer or Chicken, 1 scoop Whey Protein in water, 100g Greek Yogurt.\n")
                append("- Macros: Calories: 450 kcal, Protein: 58g, Carbs: 9g, Fat: 18g, Fiber: 0g\n")
                append("- Goal Alignment: Directly targeted to make up for your ${remainingProt.toInt()}g protein deficit.")
            } else if (lowercaseQuery.contains("breakfast")) {
                append("**Bulking Oats & Eggs Breakfast**\n")
                append("- Ingredients: 60g Oats boiled in 200ml Whole Milk, 1 Banana, 2 Scrambled Eggs (or 100g Paneer).\n")
                append("- Macros: Calories: 580 kcal, Protein: 25g, Carbs: 78g, Fat: 18g, Fiber: 8g")
            } else if (lowercaseQuery.contains("snack")) {
                append("**Post-Workout Energy Snack**\n")
                append("- Ingredients: 1 scoop Whey Protein, 1 medium Apple, 15g Almonds.\n")
                append("- Macros: Calories: 290 kcal, Protein: 28g, Carbs: 28g, Fat: 10g, Fiber: 5g")
            } else {
                if (profile.preferredCuisine.contains("South Indian", ignoreCase = true)) {
                    append("**High Protein South Indian dinner**\n")
                    append("- Ingredients: 3 Steamed Idlis, 1 bowl Sambar (with lentils), 150g Paneer Bhurji / egg bhurji.\n")
                    append("- Macros: Calories: 560 kcal, Protein: 27g, Carbs: 65g, Fat: 21g, Fiber: 6g")
                } else {
                    append("**Punjabi Paneer & Roti Dinner**\n")
                    append("- Ingredients: 150g Paneer Tikka / Bhurji, 2 whole wheat Rotis (chapatis), 1 bowl Dal (Cooked), Salad.\n")
                    append("- Macros: Calories: 640 kcal, Protein: 32g, Carbs: 68g, Fat: 24g, Fiber: 10g")
                }
            }
        }

        val adviceText = buildString {
            append("Based on your ${profile.gymSchedule} schedule, stay consistent with eating timings (${profile.mealTimings}). ")
            if (remainingProt > 15.0) {
                append("Prioritize protein-dense snacks next to secure your target. ")
            } else {
                append("You are in a great position today. Keep your carbohydrate and fat ratios balanced. ")
            }
            append("Ensure you drink 3 liters of water and prepare your available home foods (**${profile.availableFoodsAtHome}**) for tomorrow.")
        }

        return """
            ### Current Status
            $statusText
            
            ### Remaining Targets
            - Calories: ${remainingCal.toInt()} kcal
            - Protein: ${remainingProt.toInt()} g
            - Carbs: ${remainingCarb.toInt()} g
            - Fat: ${remainingFat.toInt()} g
            - Fiber: ${remainingFib.toInt()} g
            
            ### Recommended Foods
            $recommendedFoodsText
            
            ### Suggested Meal
            $suggestedMealText
            
            ### Coach's Advice
            $adviceText
        """.trimIndent()
    }

    companion object {
        private val SEED_FOODS = listOf(
            FoodItem(name = "Oats", calories = 389.0, protein = 16.9, carbs = 66.3, fat = 6.9, fiber = 10.6, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Milk (Whole)", calories = 61.0, protein = 3.2, carbs = 4.8, fat = 3.3, fiber = 0.0, servingSizeG = 100.0, servingUnit = "ml", isVegetarian = true, isVegan = false),
            FoodItem(name = "Banana", calories = 89.0, protein = 1.1, carbs = 22.8, fat = 0.3, fiber = 2.6, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = true, isVegan = true),
            FoodItem(name = "Chicken Breast", calories = 165.0, protein = 31.0, carbs = 0.0, fat = 3.6, fiber = 0.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = false, isVegan = false),
            FoodItem(name = "Egg (Whole)", calories = 78.0, protein = 6.3, carbs = 0.6, fat = 5.3, fiber = 0.0, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = false, isVegan = false),
            FoodItem(name = "Egg White", calories = 17.0, protein = 3.6, carbs = 0.2, fat = 0.1, fiber = 0.0, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = false, isVegan = false),
            FoodItem(name = "White Rice (Cooked)", calories = 130.0, protein = 2.7, carbs = 28.0, fat = 0.3, fiber = 0.4, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Brown Rice (Cooked)", calories = 112.0, protein = 2.3, carbs = 24.0, fat = 0.8, fiber = 1.8, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Paneer", calories = 265.0, protein = 18.3, carbs = 6.0, fat = 20.8, fiber = 0.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = false),
            FoodItem(name = "Roti (Whole Wheat)", calories = 120.0, protein = 3.5, carbs = 24.0, fat = 0.8, fiber = 3.5, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = true, isVegan = true),
            FoodItem(name = "Dal (Cooked)", calories = 116.0, protein = 9.0, carbs = 20.0, fat = 0.4, fiber = 8.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Tofu", calories = 76.0, protein = 8.0, carbs = 1.9, fat = 4.8, fiber = 0.3, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Soy Chunks (Dry)", calories = 345.0, protein = 52.0, carbs = 33.0, fat = 0.5, fiber = 13.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Peanuts", calories = 567.0, protein = 25.8, carbs = 16.1, fat = 49.2, fiber = 8.5, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true, allergens = "Peanuts"),
            FoodItem(name = "Greek Yogurt", calories = 59.0, protein = 10.0, carbs = 3.6, fat = 0.4, fiber = 0.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = false),
            FoodItem(name = "Potato (Boiled)", calories = 87.0, protein = 1.9, carbs = 20.0, fat = 0.1, fiber = 1.8, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Salmon Fish", calories = 208.0, protein = 20.0, carbs = 0.0, fat = 13.0, fiber = 0.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = false, isVegan = false),
            FoodItem(name = "Paneer Bhurji", calories = 230.0, protein = 14.0, carbs = 5.0, fat = 18.0, fiber = 1.5, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = false),
            FoodItem(name = "Chicken Curry", calories = 160.0, protein = 14.0, carbs = 5.0, fat = 9.0, fiber = 1.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = false, isVegan = false),
            FoodItem(name = "Whey Protein Shake", calories = 120.0, protein = 24.0, carbs = 3.0, fat = 1.5, fiber = 0.0, servingSizeG = 1.0, servingUnit = "scoop", isVegetarian = true, isVegan = false),
            FoodItem(name = "Peanut Butter", calories = 94.0, protein = 4.0, carbs = 3.0, fat = 8.0, fiber = 1.0, servingSizeG = 1.0, servingUnit = "tbsp", isVegetarian = true, isVegan = true, allergens = "Peanuts"),
            FoodItem(name = "Almonds", calories = 7.0, protein = 0.25, carbs = 0.25, fat = 0.6, fiber = 0.15, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = true, isVegan = true, allergens = "Nuts"),
            FoodItem(name = "Apple", calories = 95.0, protein = 0.5, carbs = 25.0, fat = 0.3, fiber = 4.4, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = true, isVegan = true),
            FoodItem(name = "Spinach (Cooked)", calories = 23.0, protein = 3.0, carbs = 3.8, fat = 0.3, fiber = 2.4, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Broccoli (Cooked)", calories = 35.0, protein = 2.4, carbs = 7.0, fat = 0.4, fiber = 3.3, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Sweet Potato", calories = 90.0, protein = 2.0, carbs = 21.0, fat = 0.1, fiber = 3.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Avocado", calories = 160.0, protein = 2.0, carbs = 8.5, fat = 14.7, fiber = 6.7, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Chickpeas (Cooked)", calories = 164.0, protein = 9.0, carbs = 27.0, fat = 2.6, fiber = 7.6, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Rajma Kidney Beans", calories = 127.0, protein = 8.7, carbs = 22.8, fat = 0.5, fiber = 6.4, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Mixed Vegetable Sabzi", calories = 90.0, protein = 2.0, carbs = 10.0, fat = 5.0, fiber = 3.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Fish Curry (Salmon)", calories = 150.0, protein = 16.0, carbs = 4.0, fat = 8.0, fiber = 0.0, servingSizeG = 100.0, servingUnit = "g", isVegetarian = false, isVegan = false),
            FoodItem(name = "Cucumber", calories = 15.0, protein = 0.7, carbs = 3.6, fat = 0.1, fiber = 0.5, servingSizeG = 100.0, servingUnit = "g", isVegetarian = true, isVegan = true),
            FoodItem(name = "Green Salad", calories = 30.0, protein = 1.0, carbs = 5.0, fat = 0.2, fiber = 2.5, servingSizeG = 1.0, servingUnit = "bowl", isVegetarian = true, isVegan = true),
            FoodItem(name = "Roti / Chapati", calories = 120.0, protein = 3.5, carbs = 24.0, fat = 0.8, fiber = 3.5, servingSizeG = 1.0, servingUnit = "piece", isVegetarian = true, isVegan = true)
        )
    }
}

class NutritionViewModelFactory(
    private val nutritionDao: NutritionDao,
    private val activityDao: ActivityDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NutritionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NutritionViewModel(nutritionDao, activityDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

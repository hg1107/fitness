package com.example.fitnesstracker.ui

import androidx.lifecycle.SavedStateHandle
import com.example.fitnesstracker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModelTest {

    private lateinit var fakeNutritionDao: FakeNutritionDao
    private lateinit var fakeActivityDao: FakeActivityDaoForNutrition
    private lateinit var fakeBodyMeasurementDao: FakeBodyMeasurementDao
    private lateinit var viewModel: NutritionViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeNutritionDao = FakeNutritionDao()
        fakeActivityDao = FakeActivityDaoForNutrition()
        fakeBodyMeasurementDao = FakeBodyMeasurementDao()

        viewModel = NutritionViewModel(
            nutritionDao = fakeNutritionDao,
            activityDao = fakeActivityDao,
            bodyMeasurementDao = fakeBodyMeasurementDao,
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCalculateGoalDefaultFormula() {
        // Test Male, 25 years old, 70kg, 175cm, Moderately Active, Lean Bulk
        val goal = viewModel.calculateGoal(
            gender = "Male",
            age = 25,
            weightKg = 70.0,
            heightCm = 175.0,
            activityLevel = "Moderately Active",
            fitnessGoal = "Lean Bulk"
        )

        // BMR = 10 * 70 + 6.25 * 175 - 5 * 25 + 5 = 700 + 1093.75 - 125 + 5 = 1673.75
        // TDEE = BMR * 1.55 = 1673.75 * 1.55 = 2594.3125
        // Lean Bulk = TDEE + 300 = 2894.3125
        assertEquals(2894.3125, goal.calories, 0.1)
        assertEquals(140.0, goal.protein, 0.1) // 70 * 2.0
    }

    @Test
    fun testCalculateGoalCustomInputsOverride() {
        val goal = viewModel.calculateGoal(
            gender = "Female",
            age = 30,
            weightKg = 60.0,
            heightCm = 160.0,
            activityLevel = "Sedentary",
            fitnessGoal = "Fat Loss",
            customCalories = 1800.0,
            customProtein = 120.0,
            customCarbs = 200.0,
            customFat = 50.0
        )

        assertEquals(1800.0, goal.calories, 0.01)
        assertEquals(120.0, goal.protein, 0.01)
        assertEquals(200.0, goal.carbs, 0.01)
        assertEquals(50.0, goal.fat, 0.01)
    }

    @Test
    fun testLogFood() = runTest {
        val foodItem = FoodItem(
            name = "Banana",
            calories = 89.0,
            protein = 1.1,
            carbs = 22.8,
            fat = 0.3,
            servingUnit = "g",
            isVegetarian = true
        )

        viewModel.logFood("Breakfast", foodItem, 1.5)

        val foodLogs = viewModel.todayFoodLogs.first()
        assertEquals(1, foodLogs.size)
        assertEquals("Banana", foodLogs[0].foodName)
        assertEquals("Breakfast", foodLogs[0].mealType)
        assertEquals(1.5, foodLogs[0].quantity, 0.01)
    }

    @Test
    fun testDeleteFoodLog() = runTest {
        val log = FoodLog(
            id = 101L,
            date = viewModel.currentDate.value,
            mealType = "Lunch",
            foodName = "Chicken Salad",
            calories = 350.0,
            protein = 30.0,
            carbs = 10.0,
            fat = 15.0,
            fiber = 2.0,
            quantity = 1.0,
            servingUnit = "serving"
        )
        fakeNutritionDao.insertFoodLog(log)

        var foodLogs = viewModel.todayFoodLogs.first()
        assertEquals(1, foodLogs.size)

        viewModel.deleteFoodLog(log)

        foodLogs = viewModel.todayFoodLogs.first()
        assertEquals(0, foodLogs.size)
    }

    @Test
    fun testLogWaterAndUndo() = runTest {
        viewModel.logWater(250)
        viewModel.logWater(500)

        val totalWater = viewModel.todayWaterAmount.first()
        assertEquals(750, totalWater)

        viewModel.undoLastWaterLog()
        val undoneWater = viewModel.todayWaterAmount.first()
        assertEquals(250, undoneWater)
    }

    @Test
    fun testLogWeightUpdatesProfile() = runTest {
        // Initialize user profile
        val profile = UserProfile(id = "default_user", weightKg = 70.0)
        fakeActivityDao.insertUserProfile(profile)

        viewModel.logWeight(75.5)

        // Verify weight logged
        val weightLogs = viewModel.allWeightLogs.first()
        assertEquals(1, weightLogs.size)
        assertEquals(75.5, weightLogs[0].weightKg, 0.01)

        // Verify profile weight updated
        val updatedProfile = fakeActivityDao.getUserProfileSync()
        assertNotNull(updatedProfile)
        assertEquals(75.5, updatedProfile!!.weightKg, 0.01)
    }

    @Test
    fun testLogBodyMeasurement() = runTest {
        viewModel.logBodyMeasurement(
            chest = 100.0,
            waist = 80.0,
            hips = 95.0,
            arms = 38.0,
            thighs = 55.0
        )

        val measurements = viewModel.bodyMeasurements.first()
        assertEquals(1, measurements.size)
        assertEquals(100.0, measurements[0].chestCm ?: 0.0, 0.01)
        assertEquals(80.0, measurements[0].waistCm ?: 0.0, 0.01)
        assertEquals(95.0, measurements[0].hipsCm ?: 0.0, 0.01)
        assertEquals(38.0, measurements[0].armsCm ?: 0.0, 0.01)
        assertEquals(55.0, measurements[0].thighsCm ?: 0.0, 0.01)
    }
}

// --- Fakes ---

class FakeNutritionDao : NutritionDao {
    val foodItems = mutableListOf<FoodItem>()
    val foodLogs = mutableListOf<FoodLog>()
    val waterLogs = mutableListOf<WaterLog>()
    val weightLogs = mutableListOf<WeightLog>()
    val savedMeals = mutableListOf<SavedMeal>()

    override suspend fun insertFoodItem(item: FoodItem) {
        foodItems.removeIf { it.name == item.name }
        foodItems.add(item)
    }

    override suspend fun insertFoodItems(items: List<FoodItem>) {
        items.forEach { insertFoodItem(it) }
    }

    override fun getAllFoodItems(): Flow<List<FoodItem>> = flow {
        emit(foodItems)
    }

    override fun searchFoodItems(query: String): Flow<List<FoodItem>> = flow {
        emit(foodItems.filter { it.name.contains(query, ignoreCase = true) })
    }

    override fun getAllFoodItemsLimit(limit: Int): Flow<List<FoodItem>> = flow {
        emit(foodItems.take(limit))
    }

    override fun searchFoodItemsLimit(query: String, limit: Int): Flow<List<FoodItem>> = flow {
        emit(foodItems.filter { it.name.contains(query, ignoreCase = true) }.take(limit))
    }

    override suspend fun getFoodItemByName(name: String): FoodItem? {
        return foodItems.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    override suspend fun insertFoodLog(log: FoodLog) {
        val logToInsert = if (log.id == 0L) log.copy(id = (foodLogs.size + 1).toLong()) else log
        foodLogs.add(logToInsert)
    }

    override suspend fun deleteFoodLog(log: FoodLog) {
        foodLogs.removeIf { it.id == log.id }
    }

    override fun getFoodLogsForDate(date: String): Flow<List<FoodLog>> = flow {
        emit(foodLogs.filter { it.date == date })
    }

    override fun getAllFoodLogs(): Flow<List<FoodLog>> = flow {
        emit(foodLogs.reversed())
    }

    override fun getFoodLogsSince(since: String): Flow<List<FoodLog>> = flow {
        emit(foodLogs.filter { it.date >= since }.reversed())
    }

    override fun getAllWaterLogs(): Flow<List<WaterLog>> = flow {
        emit(waterLogs.reversed())
    }

    override fun getWaterLogsSince(since: String): Flow<List<WaterLog>> = flow {
        emit(waterLogs.filter { it.date >= since }.reversed())
    }

    override fun getRecentFoods(): Flow<List<String>> = flow {
        emit(foodLogs.map { it.foodName }.distinct().take(20))
    }

    override suspend fun insertSavedMeal(meal: SavedMeal) {
        savedMeals.add(meal)
    }

    override fun getAllSavedMeals(): Flow<List<SavedMeal>> = flow {
        emit(savedMeals)
    }

    override suspend fun deleteSavedMeal(meal: SavedMeal) {
        savedMeals.removeIf { it.id == meal.id }
    }

    override suspend fun insertWeightLog(log: WeightLog) {
        weightLogs.add(log)
    }

    override fun getAllWeightLogs(): Flow<List<WeightLog>> = flow {
        emit(weightLogs.sortedByDescending { it.timestamp })
    }

    override suspend fun getLatestWeightLogSync(): WeightLog? {
        return weightLogs.maxByOrNull { it.timestamp }
    }

    override suspend fun deleteWeightLog(log: WeightLog) {
        weightLogs.removeIf { it.timestamp == log.timestamp }
    }

    override suspend fun insertWaterLog(log: WaterLog) {
        val logToInsert = if (log.id == 0L) log.copy(id = (waterLogs.size + 1).toLong()) else log
        waterLogs.add(logToInsert)
    }

    override fun getWaterAmountForDate(date: String): Flow<Int?> = flow {
        val sum = waterLogs.filter { it.date == date }.sumOf { it.amountMl }
        emit(if (sum == 0) null else sum)
    }

    override fun getWaterLogsForDate(date: String): Flow<List<WaterLog>> = flow {
        emit(waterLogs.filter { it.date == date }.reversed())
    }

    override suspend fun deleteWaterLog(log: WaterLog) {
        waterLogs.removeIf { it.id == log.id }
    }
}

class FakeBodyMeasurementDao : BodyMeasurementDao {
    val measurements = mutableListOf<BodyMeasurement>()

    override suspend fun insertBodyMeasurement(measurement: BodyMeasurement) {
        measurements.add(measurement)
    }

    override fun getAllBodyMeasurements(): Flow<List<BodyMeasurement>> = flow {
        emit(measurements.sortedByDescending { it.date })
    }

    override suspend fun deleteBodyMeasurement(measurement: BodyMeasurement) {
        measurements.removeIf { it.date == measurement.date }
    }
}

class FakeActivityDaoForNutrition : ActivityDao {
    var profile: UserProfile? = null
    val activities = mutableListOf<ActivityRecord>()
    val activityPoints = mutableListOf<ActivityPoint>()

    override suspend fun insertActivity(activity: ActivityRecord): Long {
        val id = (activities.size + 1).toLong()
        activities.add(activity.copy(id = id))
        return id
    }

    override suspend fun insertActivityPoints(points: List<ActivityPoint>) {
        activityPoints.addAll(points)
    }

    override fun getAllActivities(): Flow<List<ActivityRecord>> = flow {
        emit(activities.reversed())
    }

    override suspend fun getActivityById(activityId: Long): ActivityRecord? {
        return activities.firstOrNull { it.id == activityId }
    }

    override fun getPointsForActivity(activityId: Long): Flow<List<ActivityPoint>> = flow {
        emit(activityPoints.filter { it.activityId == activityId })
    }

    override suspend fun getPointsForActivitySync(activityId: Long): List<ActivityPoint> {
        return activityPoints.filter { it.activityId == activityId }
    }

    override suspend fun deleteActivity(activity: ActivityRecord) {
        activities.removeIf { it.id == activity.id }
        activityPoints.removeIf { it.activityId == activity.id }
    }

    override suspend fun updateActivityNotes(id: Long, notes: String) {
        val index = activities.indexOfFirst { it.id == id }
        if (index != -1) {
            activities[index] = activities[index].copy(notes = notes)
        }
    }

    override suspend fun markActivitiesAsSynced(ids: List<Long>) {
        ids.forEach { id ->
            val index = activities.indexOfFirst { it.id == id }
            if (index != -1) {
                activities[index] = activities[index].copy(isSynced = true)
            }
        }
    }

    override fun getTotalDistanceMetersSince(since: Long): Flow<Double> = flow {
        emit(activities.filter { it.startTime >= since }.sumOf { it.distanceMeters })
    }

    override fun getTotalCaloriesSince(since: Long): Flow<Double> = flow {
        emit(activities.filter { it.startTime >= since }.sumOf { it.calories })
    }

    override fun getActivityCountSince(since: Long): Flow<Int> = flow {
        emit(activities.count { it.startTime >= since })
    }

    override fun getTotalDurationSecondsSince(since: Long): Flow<Long> = flow {
        emit(activities.filter { it.startTime >= since }.sumOf { it.durationSeconds })
    }

    override fun getActiveDayCountSince(since: Long): Flow<Int> = flow {
        val count = activities.filter { it.startTime >= since }
            .map { it.startTime / (24 * 60 * 60 * 1000) }
            .distinct()
            .size
        emit(count)
    }

    override fun getTodayCaloriesBurned(startOfDay: Long): Flow<Double> = flow {
        emit(activities.filter { it.startTime >= startOfDay }.sumOf { it.calories })
    }

    override suspend fun insertUserProfile(profile: UserProfile) {
        this.profile = profile
    }

    override fun getUserProfile(): Flow<UserProfile?> = flow {
        emit(profile)
    }

    override suspend fun getUserProfileSync(): UserProfile? {
        return profile
    }

    override suspend fun getTotalCaloriesBurnedInRange(since: Long, until: Long): Double? {
        return activities.filter { it.startTime in since until until }.sumOf { it.calories }
    }
}

package com.example.fitnesstracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fitnesstracker.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseTest {

    private lateinit var db: WorkoutDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var nutritionDao: NutritionDao
    private lateinit var activityDao: ActivityDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            // Add all migrations to verify they compile and run correctly
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .allowMainThreadQueries()
            .build()
        workoutDao = db.workoutDao()
        nutritionDao = db.nutritionDao()
        activityDao = db.activityDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testUserProfilePersistence() = runBlocking {
        val profile = UserProfile(
            id = "default_user",
            name = "Test Athlete",
            age = 28,
            weightKg = 72.5,
            heightCm = 178.0,
            preferredUnits = "Imperial",
            waterTargetMl = 3500
        )
        activityDao.insertUserProfile(profile)

        val retrieved = activityDao.getUserProfileSync()
        assertNotNull(retrieved)
        assertEquals("Test Athlete", retrieved?.name)
        assertEquals(72.5, retrieved?.weightKg ?: 0.0, 0.01)
        assertEquals(178.0, retrieved?.heightCm ?: 0.0, 0.01)
        assertEquals("Imperial", retrieved?.preferredUnits)
        assertEquals(3500, retrieved?.waterTargetMl)
    }

    @Test
    @Throws(Exception::class)
    fun testWorkoutSessionCascadeDelete() = runBlocking {
        val session = WorkoutSession(
            id = 1L,
            exerciseName = "Squats",
            timestamp = System.currentTimeMillis(),
            notes = "Heavy day"
        )
        val sets = listOf(
            WorkoutSet(id = 1L, sessionId = 1L, setIndex = 1, weight = 100.0, reps = 5),
            WorkoutSet(id = 2L, sessionId = 1L, setIndex = 2, weight = 100.0, reps = 5)
        )

        // Save session and sets
        workoutDao.saveWorkoutSession(session, sets)

        // Retrieve sessions with sets
        val sessionsWithSets = workoutDao.getAllSessionsWithSets().first()
        assertEquals(1, sessionsWithSets.size)
        assertEquals(2, sessionsWithSets[0].sets.size)

        // Delete session (must cascade delete sets)
        workoutDao.deleteWorkoutSession(sessionsWithSets[0].session)

        val sessionsAfterDelete = workoutDao.getAllSessionsWithSets().first()
        assertEquals(0, sessionsAfterDelete.size)
    }

    @Test
    @Throws(Exception::class)
    fun testFoodLogIndexingAndRetrieval() = runBlocking {
        val log1 = FoodLog(
            date = "2026-06-18",
            mealType = "Breakfast",
            foodName = "Oats",
            calories = 300.0,
            protein = 10.0,
            carbs = 50.0,
            fat = 5.0,
            quantity = 1.0,
            servingUnit = "g"
        )
        val log2 = FoodLog(
            date = "2026-06-18",
            mealType = "Lunch",
            foodName = "Chicken Breast",
            calories = 200.0,
            protein = 30.0,
            carbs = 0.0,
            fat = 3.0,
            quantity = 1.0,
            servingUnit = "g"
        )
        val log3 = FoodLog(
            date = "2026-06-19",
            mealType = "Breakfast",
            foodName = "Banana",
            calories = 100.0,
            protein = 1.0,
            carbs = 25.0,
            fat = 0.0,
            quantity = 1.0,
            servingUnit = "piece"
        )

        nutritionDao.insertFoodLog(log1)
        nutritionDao.insertFoodLog(log2)
        nutritionDao.insertFoodLog(log3)

        // Get logs since "2026-06-18" (should return all 3)
        val logsSince18 = nutritionDao.getFoodLogsSince("2026-06-18").first()
        assertEquals(3, logsSince18.size)

        // Get logs since "2026-06-19" (should return only log3)
        val logsSince19 = nutritionDao.getFoodLogsSince("2026-06-19").first()
        assertEquals(1, logsSince19.size)
        assertEquals("Banana", logsSince19[0].foodName)
    }
}

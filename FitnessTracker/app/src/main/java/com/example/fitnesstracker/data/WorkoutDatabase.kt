package com.example.fitnesstracker.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "planned_exercises")
data class PlannedExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val exerciseName: String,
    val targetMuscle: String
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseName: String,
    val timestamp: Long,
    val notes: String = ""
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val setIndex: Int,
    val weight: Double,
    val reps: Int
)

data class SessionWithSets(
    @Embedded val session: WorkoutSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val sets: List<WorkoutSet>
)

@Dao
interface WorkoutDao {
    // Planned Routine Queries
    @Query("SELECT * FROM planned_exercises WHERE dayOfWeek = :dayOfWeek ORDER BY id ASC")
    fun getPlannedExercisesForDay(dayOfWeek: Int): Flow<List<PlannedExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedExercise(plannedExercise: PlannedExercise): Long

    @Delete
    suspend fun deletePlannedExercise(plannedExercise: PlannedExercise)

    @Query("DELETE FROM planned_exercises WHERE exerciseName = :name")
    suspend fun deletePlannedExercisesByName(name: String)

    // Log Session Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSets(sets: List<WorkoutSet>)

    @Transaction
    suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<WorkoutSet>) {
        val sessionId = insertWorkoutSession(session)
        val setsWithSession = sets.map { it.copy(sessionId = sessionId) }
        insertWorkoutSets(setsWithSession)
    }

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessionsWithSets(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE exerciseName = :exerciseName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSessionWithSetsForExercise(exerciseName: String): SessionWithSets?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE exerciseName = :exerciseName ORDER BY timestamp DESC")
    fun getAllSessionsForExercise(exerciseName: String): Flow<List<SessionWithSets>>

    // Get the all-time max weight for a specific exercise across ALL sessions
    @Query("SELECT MAX(ws.weight) FROM workout_sets ws INNER JOIN workout_sessions s ON ws.sessionId = s.id WHERE s.exerciseName = :exerciseName")
    suspend fun getMaxWeightForExercise(exerciseName: String): Double?

    // Delete a logged session (cascades to its sets via ForeignKey)
    @Delete
    suspend fun deleteWorkoutSession(session: WorkoutSession)

    // Count distinct workout sessions in the current week
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE timestamp >= :since")
    fun getSessionCountSince(since: Long): Flow<Int>

    // Get all unique exercise names from plan and logs for autocomplete
    @Query("SELECT DISTINCT exerciseName FROM (SELECT exerciseName FROM planned_exercises UNION SELECT exerciseName FROM workout_sessions) ORDER BY exerciseName ASC")
    fun getAllUniqueExerciseNames(): Flow<List<String>>

    // Get all planned exercises to find which days have workouts scheduled
    @Query("SELECT * FROM planned_exercises")
    fun getAllPlannedExercises(): Flow<List<PlannedExercise>>

    // Get user profile to check unit settings (Metric vs Imperial)
    @Query("SELECT * FROM user_profile WHERE id = 'default_user'")
    suspend fun getUserProfileSync(): UserProfile?

    // Get distinct exercise names logged since a given timestamp (for today's completion check)
    @Query("SELECT DISTINCT exerciseName FROM workout_sessions WHERE timestamp >= :since")
    fun getLoggedExerciseNamesSince(since: Long): Flow<List<String>>

    // Get all sessions since a timestamp for streak calculation
    @Query("SELECT * FROM workout_sessions WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getAllSessionsSince(since: Long): Flow<List<WorkoutSession>>
}

@Entity(tableName = "activities")
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val activityType: String, // "Running", "Walking", "Cycling"
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val calories: Double,
    val avgSpeed: Double, // meters/sec
    val avgPace: Double,  // seconds/km
    val notes: String = "",
    val isSynced: Boolean = false
)

@Entity(
    tableName = "activity_points",
    foreignKeys = [
        ForeignKey(
            entity = ActivityRecord::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("activityId")]
)
data class ActivityPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "default_user",
    val name: String = "Athlete",
    val age: Int = 30,
    val weightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val preferredUnits: String = "Metric", // "Metric" or "Imperial"
    val mapboxToken: String = "",
    val gender: String = "Male",
    val fitnessGoal: String = "Weight Maintenance",
    val activityLevel: String = "Moderately Active",
    val dietaryPreference: String = "Non-Vegetarian",
    val onboardingComplete: Boolean = false,
    val foodLikes: String = "",
    val foodDislikes: String = "",
    val foodAllergies: String = "",
    val budget: String = "Moderate",
    val region: String = "Indian",
    val preferredCuisine: String = "Punjabi",
    val mealTimings: String = "Breakfast: 8 AM, Lunch: 1:30 PM, Dinner: 8:30 PM",
    val gymSchedule: String = "Mon, Wed, Fri",
    val availableFoodsAtHome: String = "eggs, milk, paneer, oats, banana, rice, roti",
    val geminiApiKey: String = "",
    val waterTargetMl: Int = 3000
)

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val servingSizeG: Double = 100.0,
    val servingUnit: String = "g",
    val isVegetarian: Boolean = true,
    val isVegan: Boolean = false,
    val allergens: String = ""
)

@Entity(tableName = "food_logs")
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String, // "Breakfast", "Lunch", "Snack", "Dinner"
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val quantity: Double = 1.0,
    val servingUnit: String = "g"
)

@Entity(tableName = "saved_meals")
data class SavedMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealName: String,
    val foodLogsJson: String
)

@Entity(tableName = "weight_logs")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val weightKg: Double
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val amountMl: Int
)

@Entity(tableName = "coach_messages")
data class CoachMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sender: String, // "user" or "coach"
    val text: String
)

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityPoints(points: List<ActivityPoint>)

    @Query("SELECT * FROM activities ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activities WHERE id = :activityId")
    suspend fun getActivityById(activityId: Long): ActivityRecord?

    @Query("SELECT * FROM activity_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    fun getPointsForActivity(activityId: Long): Flow<List<ActivityPoint>>

    @Query("SELECT * FROM activity_points WHERE activityId = :activityId ORDER BY timestamp ASC")
    suspend fun getPointsForActivitySync(activityId: Long): List<ActivityPoint>

    @Delete
    suspend fun deleteActivity(activity: ActivityRecord)

    @Query("UPDATE activities SET notes = :notes WHERE id = :id")
    suspend fun updateActivityNotes(id: Long, notes: String)

    @Query("UPDATE activities SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markActivitiesAsSynced(ids: List<Long>)

    // User Profile Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE id = 'default_user'")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 'default_user'")
    suspend fun getUserProfileSync(): UserProfile?

    // Get total calories burned for a date range
    @Query("SELECT SUM(calories) FROM activities WHERE startTime >= :since AND startTime < :until")
    suspend fun getTotalCaloriesBurnedInRange(since: Long, until: Long): Double?
}

@Dao
interface NutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(items: List<FoodItem>)

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchFoodItems(query: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name = :name LIMIT 1")
    suspend fun getFoodItemByName(name: String): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLog)

    @Delete
    suspend fun deleteFoodLog(log: FoodLog)

    @Query("SELECT * FROM food_logs WHERE date = :date ORDER BY id ASC")
    fun getFoodLogsForDate(date: String): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs ORDER BY date DESC")
    fun getAllFoodLogs(): Flow<List<FoodLog>>

    // Fix #19: SQL-level weekly filter to avoid full-table scan + in-memory filtering
    @Query("SELECT * FROM food_logs WHERE date >= :since ORDER BY date DESC")
    fun getFoodLogsSince(since: String): Flow<List<FoodLog>>

    @Query("SELECT * FROM water_logs ORDER BY date DESC")
    fun getAllWaterLogs(): Flow<List<WaterLog>>

    // Fix #19: SQL-level weekly filter for water logs
    @Query("SELECT * FROM water_logs WHERE date >= :since ORDER BY date DESC")
    fun getWaterLogsSince(since: String): Flow<List<WaterLog>>

    @Query("SELECT DISTINCT foodName FROM food_logs ORDER BY id DESC LIMIT 20")
    fun getRecentFoods(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedMeal(meal: SavedMeal)

    @Query("SELECT * FROM saved_meals ORDER BY mealName ASC")
    fun getAllSavedMeals(): Flow<List<SavedMeal>>

    @Delete
    suspend fun deleteSavedMeal(meal: SavedMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog)

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWeightLogSync(): WeightLog?

    @Delete
    suspend fun deleteWeightLog(log: WeightLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(log: WaterLog)

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE date = :date")
    fun getWaterAmountForDate(date: String): Flow<Int?>

    @Query("SELECT * FROM water_logs WHERE date = :date ORDER BY id DESC")
    fun getWaterLogsForDate(date: String): Flow<List<WaterLog>>

    @Delete
    suspend fun deleteWaterLog(log: WaterLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoachMessage(message: CoachMessage)

    @Query("SELECT * FROM coach_messages ORDER BY timestamp ASC")
    fun getAllCoachMessages(): Flow<List<CoachMessage>>

    @Query("DELETE FROM coach_messages")
    suspend fun clearAllCoachMessages()
}

// Migration from v5 to v6: adds waterTargetMl column to user_profile
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_profile ADD COLUMN waterTargetMl INTEGER NOT NULL DEFAULT 3000")
    }
}

@Database(
    entities = [
        PlannedExercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        ActivityRecord::class,
        ActivityPoint::class,
        UserProfile::class,
        FoodItem::class,
        FoodLog::class,
        SavedMeal::class,
        WeightLog::class,
        WaterLog::class,
        CoachMessage::class
    ],
    version = 6,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun activityDao(): ActivityDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                // Fix #26: versions 1-4 have no explicit migrations; users on those
                // versions get a destructive reset. Version 5→6 is safe (addColumn only).
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                .addMigrations(MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }

        /** Closes the open instance so the underlying file can be replaced (backup restore). */
        fun closeAndReset() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                INSTANCE = null
            }
        }
    }
}

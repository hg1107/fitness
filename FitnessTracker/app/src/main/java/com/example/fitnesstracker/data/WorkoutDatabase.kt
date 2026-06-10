package com.example.fitnesstracker.data

import android.content.Context
import androidx.room.*
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
    val mapboxToken: String = ""
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
}

@Database(
    entities = [
        PlannedExercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        ActivityRecord::class,
        ActivityPoint::class,
        UserProfile::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun activityDao(): ActivityDao

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
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


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
}

@Database(
    entities = [PlannedExercise::class, WorkoutSession::class, WorkoutSet::class],
    version = 2,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

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

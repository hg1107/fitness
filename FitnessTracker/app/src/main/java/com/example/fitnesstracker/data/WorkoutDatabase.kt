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
    val timestamp: Long
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
}

@Database(
    entities = [PlannedExercise::class, WorkoutSession::class, WorkoutSet::class],
    version = 1,
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

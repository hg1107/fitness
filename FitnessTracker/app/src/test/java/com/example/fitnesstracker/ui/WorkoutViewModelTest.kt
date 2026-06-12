package com.example.fitnesstracker.ui

import com.example.fitnesstracker.data.PlannedExercise
import com.example.fitnesstracker.data.SessionWithSets
import com.example.fitnesstracker.data.WorkoutDao
import com.example.fitnesstracker.data.WorkoutSession
import com.example.fitnesstracker.data.WorkoutSet
import com.example.fitnesstracker.data.UserProfile
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var viewModel: WorkoutViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeWorkoutDao()
        viewModel = WorkoutViewModel(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSelectDayAndAddExercise() = runTest {
        // Given Wednesday (day 3) is selected
        viewModel.selectDay(3)

        // When we add an exercise
        viewModel.addPlannedExercise("Bench Press", "Chest")

        // Then it should be saved in the database for Wednesday
        val exercises = viewModel.plannedExercises.first()
        assertEquals(1, exercises.size)
        assertEquals("Bench Press", exercises[0].exerciseName)
        assertEquals("Chest", exercises[0].targetMuscle)
        assertEquals(3, exercises[0].dayOfWeek)
    }

    @Test
    fun testLoggingSetsInputFlow() = runTest {
        // When we start logging "Squats" (first session, no history)
        viewModel.startLogging("Squats")

        // Then we should start with 1 empty set input
        assertEquals(1, viewModel.currentSets.size)
        assertEquals("", viewModel.currentSets[0].weight)
        assertEquals("", viewModel.currentSets[0].reps)

        // When we edit the set
        viewModel.updateSetWeight(0, "100")
        viewModel.updateSetReps(0, "5")

        // And we add a second set
        viewModel.addSet()

        // Then the second set should copy values from the first set
        assertEquals(2, viewModel.currentSets.size)
        assertEquals("100", viewModel.currentSets[1].weight)
        assertEquals("5", viewModel.currentSets[1].reps)

        // When we delete the first set
        viewModel.deleteSet(0)

        // Then the remaining set should be re-indexed to index 0
        assertEquals(1, viewModel.currentSets.size)
        assertEquals(0, viewModel.currentSets[0].setIndex)
        assertEquals("100", viewModel.currentSets[0].weight)
    }

    @Test
    fun testSavingAndCalculatingWeeklyVolume() = runTest {
        // Given we log a session of Deadlift: 2 sets of 150kg x 5 reps
        viewModel.startLogging("Deadlift")
        viewModel.updateSetWeight(0, "150")
        viewModel.updateSetReps(0, "5")
        
        viewModel.addSet() // Copy values: 150 x 5
        
        // When we save the session
        var wasSaved = false
        viewModel.saveSession { wasSaved = true }

        // Then it should be saved and volume calculated
        assertEquals(true, wasSaved)
        
        val weeklyVolume = viewModel.weeklyVolume.first()
        // Volume = (150 * 5) + (150 * 5) = 1500
        assertEquals(1500.0, weeklyVolume, 0.01)

        val weeklySets = viewModel.weeklySetCount.first()
        assertEquals(2, weeklySets)
    }

    @Test
    fun testDeleteSessionDoesNotDeletePlannedExercise() = runTest {
        // Given a planned exercise for Wednesday (day 3)
        viewModel.selectDay(3)
        viewModel.addPlannedExercise("Squats", "Legs")
        
        // Confirm it is added
        val plannedBefore = viewModel.plannedExercises.first()
        assertEquals(1, plannedBefore.size)
        assertEquals("Squats", plannedBefore[0].exerciseName)
        
        // Log a session for Wednesday (2026-06-10 is Wednesday)
        val calendar = java.util.Calendar.getInstance()
        calendar.set(2026, java.util.Calendar.JUNE, 10, 10, 0)
        val wednesdayTimestamp = calendar.timeInMillis
        
        val session = WorkoutSession(
            id = 1,
            exerciseName = "Squats",
            timestamp = wednesdayTimestamp,
            notes = ""
        )
        val sessionWithSets = SessionWithSets(session, emptyList())
        
        // When we delete this session
        viewModel.deleteSession(sessionWithSets)
        
        // Then the planned exercise should still exist
        val plannedAfter = viewModel.plannedExercises.first()
        assertEquals(1, plannedAfter.size)
    }
}

class FakeWorkoutDao : WorkoutDao {
    val plannedExercises = mutableListOf<PlannedExercise>()
    val sessions = mutableListOf<SessionWithSets>()

    override fun getPlannedExercisesForDay(dayOfWeek: Int): Flow<List<PlannedExercise>> = flow {
        emit(plannedExercises.filter { it.dayOfWeek == dayOfWeek })
    }

    override suspend fun insertPlannedExercise(plannedExercise: PlannedExercise): Long {
        val newExercise = plannedExercise.copy(id = plannedExercises.size.toLong() + 1)
        plannedExercises.add(newExercise)
        return newExercise.id
    }

    override suspend fun deletePlannedExercise(plannedExercise: PlannedExercise) {
        plannedExercises.removeIf { it.id == plannedExercise.id }
    }

    override suspend fun deletePlannedExercisesByName(name: String) {
        plannedExercises.removeIf { it.exerciseName == name }
    }

    override fun getLoggedExerciseNamesSince(since: Long): Flow<List<String>> = flow {
        emit(sessions.filter { it.session.timestamp >= since }.map { it.session.exerciseName }.distinct())
    }

    override fun getAllSessionsSince(since: Long): Flow<List<WorkoutSession>> = flow {
        emit(sessions.filter { it.session.timestamp >= since }.map { it.session })
    }

    override suspend fun insertWorkoutSession(session: WorkoutSession): Long {
        val id = sessions.size.toLong() + 1
        sessions.add(SessionWithSets(session.copy(id = id), emptyList()))
        return id
    }

    override suspend fun insertWorkoutSets(sets: List<WorkoutSet>) {
        if (sessions.isNotEmpty()) {
            val lastSessionIndex = sessions.size - 1
            val lastSession = sessions[lastSessionIndex]
            sessions[lastSessionIndex] = lastSession.copy(sets = lastSession.sets + sets)
        }
    }

    override suspend fun saveWorkoutSession(session: WorkoutSession, sets: List<WorkoutSet>) {
        val sessionId = insertWorkoutSession(session)
        val setsWithSession = sets.map { it.copy(sessionId = sessionId) }
        insertWorkoutSets(setsWithSession)
    }

    override fun getAllSessionsWithSets(): Flow<List<SessionWithSets>> = flow {
        emit(sessions.reversed())
    }

    override suspend fun getLastSessionWithSetsForExercise(exerciseName: String): SessionWithSets? {
        return sessions.lastOrNull { it.session.exerciseName == exerciseName }
    }

    override fun getAllSessionsForExercise(exerciseName: String): Flow<List<SessionWithSets>> = flow {
        emit(sessions.filter { it.session.exerciseName == exerciseName }.reversed())
    }

    override suspend fun getMaxWeightForExercise(exerciseName: String): Double? {
        return sessions.filter { it.session.exerciseName == exerciseName }
            .flatMap { it.sets }
            .maxOfOrNull { it.weight }
    }

    override suspend fun deleteWorkoutSession(session: WorkoutSession) {
        sessions.removeIf { it.session.id == session.id }
    }

    override fun getSessionCountSince(since: Long): Flow<Int> = flow {
        emit(sessions.count { it.session.timestamp >= since })
    }

    override fun getAllUniqueExerciseNames(): Flow<List<String>> = flow {
        val names = (plannedExercises.map { it.exerciseName } + sessions.map { it.session.exerciseName }).distinct().sorted()
        emit(names)
    }

    override fun getAllPlannedExercises(): Flow<List<PlannedExercise>> = flow {
        emit(plannedExercises)
    }

    override suspend fun getUserProfileSync(): UserProfile? {
        return UserProfile(
            id = "default_user",
            name = "John Doe",
            age = 25,
            gender = "Male",
            weightKg = 70.0,
            heightCm = 175.0,
            fitnessGoal = "Lean Bulk",
            activityLevel = "Moderately Active",
            preferredUnits = "Metric",
            dietaryPreference = "Non-Vegetarian",
            foodAllergies = "None"
        )
    }
}

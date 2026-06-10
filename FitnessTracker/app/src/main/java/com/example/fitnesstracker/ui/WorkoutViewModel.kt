package com.example.fitnesstracker.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.PlannedExercise
import com.example.fitnesstracker.data.SessionWithSets
import com.example.fitnesstracker.data.WorkoutDao
import com.example.fitnesstracker.data.WorkoutSession
import com.example.fitnesstracker.data.WorkoutSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ChartMetric {
    WEIGHT, REPS
}

data class SetInputState(
    val setIndex: Int,
    val weight: String = "",
    val reps: String = "",
    val isPersonalBest: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val workoutDao: WorkoutDao) : ViewModel() {

    // --- Dashboard / Routine Manager ---
    private val _selectedDay = MutableStateFlow(getCurrentDayOfWeek()) // 1 = Monday, ..., 7 = Sunday
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    val plannedExercises: Flow<List<PlannedExercise>> = _selectedDay
        .flatMapLatest { day ->
            workoutDao.getPlannedExercisesForDay(day)
        }

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun addPlannedExercise(name: String, targetMuscle: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                workoutDao.insertPlannedExercise(
                    PlannedExercise(
                        dayOfWeek = _selectedDay.value,
                        exerciseName = name.trim(),
                        targetMuscle = targetMuscle.trim().ifEmpty { "General" }
                    )
                )
            }
        }
    }

    fun deletePlannedExercise(exercise: PlannedExercise) {
        viewModelScope.launch {
            workoutDao.deletePlannedExercise(exercise)
        }
    }

    // --- Log Exercise Screen State ---
    private val _loggingExerciseName = MutableStateFlow("")
    val loggingExerciseName: StateFlow<String> = _loggingExerciseName.asStateFlow()

    private val _previousSession = MutableStateFlow<SessionWithSets?>(null)
    val previousSession: StateFlow<SessionWithSets?> = _previousSession.asStateFlow()

    // SnapshotStateList triggers recompositions automatically when elements are added/removed/updated.
    val currentSets = mutableStateListOf<SetInputState>()

    private val _chartMetric = MutableStateFlow(ChartMetric.WEIGHT)
    val chartMetric: StateFlow<ChartMetric> = _chartMetric.asStateFlow()

    private val _sessionNotes = MutableStateFlow("")
    val sessionNotes: StateFlow<String> = _sessionNotes.asStateFlow()

    // Tracks total volume in current session for live display
    private val _currentVolume = MutableStateFlow(0.0)
    val currentVolume: StateFlow<Double> = _currentVolume.asStateFlow()

    private fun recalculateVolume() {
        _currentVolume.value = currentSets.sumOf {
            (it.weight.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0)
        }
    }

    // Personal best tracking: max single-set weight for the exercise ever
    private val _personalBestWeight = MutableStateFlow(0.0)
    val personalBestWeight: StateFlow<Double> = _personalBestWeight.asStateFlow()

    fun setChartMetric(metric: ChartMetric) {
        _chartMetric.value = metric
    }

    fun updateSessionNotes(notes: String) {
        _sessionNotes.value = notes
    }

    fun startLogging(exerciseName: String) {
        _loggingExerciseName.value = exerciseName
        _sessionNotes.value = ""
        currentSets.clear()
        viewModelScope.launch {
            val lastSession = workoutDao.getLastSessionWithSetsForExercise(exerciseName)
            _previousSession.value = lastSession

            // Compute personal best from last session
            _personalBestWeight.value = lastSession?.sets?.maxOfOrNull { it.weight } ?: 0.0

            // Pre-populate with standard sets if lastSession exists, else start with 1 empty set
            if (lastSession != null && lastSession.sets.isNotEmpty()) {
                lastSession.sets.forEach { set ->
                    currentSets.add(
                        SetInputState(
                            setIndex = set.setIndex,
                            weight = set.weight.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() },
                            reps = set.reps.toString()
                        )
                    )
                }
            } else {
                currentSets.add(SetInputState(setIndex = 0, weight = "", reps = ""))
            }
        }
    }

    fun updateSetWeight(index: Int, weight: String) {
        if (index in currentSets.indices) {
            val prevBest = _personalBestWeight.value
            val newWeight = weight.toDoubleOrNull() ?: 0.0
            currentSets[index] = currentSets[index].copy(
                weight = weight,
                isPersonalBest = prevBest > 0 && newWeight > prevBest
            )
            recalculateVolume()
        }
    }

    fun updateSetReps(index: Int, reps: String) {
        if (index in currentSets.indices) {
            currentSets[index] = currentSets[index].copy(reps = reps)
            recalculateVolume()
        }
    }

    fun addSet() {
        val newIndex = currentSets.size
        // Pre-fill with the values from the previous set if available to speed up logging
        val templateSet = currentSets.lastOrNull()
        currentSets.add(
            SetInputState(
                setIndex = newIndex,
                weight = templateSet?.weight ?: "",
                reps = templateSet?.reps ?: ""
            )
        )
        recalculateVolume()
    }

    fun deleteSet(index: Int) {
        if (index in currentSets.indices) {
            currentSets.removeAt(index)
            // Re-index remaining sets to ensure 0-based continuity
            val updated = currentSets.mapIndexed { idx, set ->
                set.copy(setIndex = idx)
            }
            currentSets.clear()
            currentSets.addAll(updated)
            recalculateVolume()
        }
    }

    fun copyPreviousSessionSets() {
        val lastSession = _previousSession.value
        if (lastSession != null && lastSession.sets.isNotEmpty()) {
            currentSets.clear()
            lastSession.sets.forEach { set ->
                currentSets.add(
                    SetInputState(
                        setIndex = set.setIndex,
                        weight = set.weight.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() },
                        reps = set.reps.toString()
                    )
                )
            }
            recalculateVolume()
        }
    }

    fun saveSession(onSuccess: () -> Unit) {
        val exerciseName = _loggingExerciseName.value
        if (exerciseName.isBlank() || currentSets.isEmpty()) return

        viewModelScope.launch {
            val session = WorkoutSession(
                exerciseName = exerciseName,
                timestamp = System.currentTimeMillis(),
                notes = _sessionNotes.value.trim()
            )

            // Convert string inputs to numeric data types (fail-safe defaults)
            val sets = currentSets.map { set ->
                WorkoutSet(
                    sessionId = 0, // Assigned inside room transaction
                    setIndex = set.setIndex,
                    weight = set.weight.toDoubleOrNull() ?: 0.0,
                    reps = set.reps.toIntOrNull() ?: 0
                )
            }

            workoutDao.saveWorkoutSession(session, sets)
            onSuccess()
        }
    }

    // --- Rest Timer ---
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _restTimerRunning = MutableStateFlow(false)
    val restTimerRunning: StateFlow<Boolean> = _restTimerRunning.asStateFlow()

    private val _restTimerDuration = MutableStateFlow(90) // default 90s
    val restTimerDuration: StateFlow<Int> = _restTimerDuration.asStateFlow()

    private var timerJob: Job? = null

    fun startRestTimer(seconds: Int = _restTimerDuration.value) {
        timerJob?.cancel()
        _restTimerDuration.value = seconds
        _restTimerSeconds.value = seconds
        _restTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.value -= 1
            }
            _restTimerRunning.value = false
        }
    }

    fun stopRestTimer() {
        timerJob?.cancel()
        _restTimerRunning.value = false
        _restTimerSeconds.value = 0
    }

    fun setRestTimerDuration(seconds: Int) {
        _restTimerDuration.value = seconds
    }

    // --- History & Stats ---
    val allSessions: Flow<List<SessionWithSets>> = workoutDao.getAllSessionsWithSets()

    // Calculated metrics for the current week (last 7 days)
    val weeklyVolume: Flow<Double> = allSessions.map { sessions ->
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        sessions.filter { it.session.timestamp >= oneWeekAgo }
            .flatMap { it.sets }
            .sumOf { it.weight * it.reps }
    }

    val weeklySetCount: Flow<Int> = allSessions.map { sessions ->
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        sessions.filter { it.session.timestamp >= oneWeekAgo }
            .flatMap { it.sets }
            .size
    }

    // Boolean list of active workout days in the current week (Index 0 = Monday, ..., 6 = Sunday)
    val weeklyActiveDays: Flow<List<Boolean>> = allSessions.map { sessions ->
        val activeDays = MutableList(7) { false }
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        sessions.forEach { item ->
            calendar.timeInMillis = item.session.timestamp
            if (calendar.get(Calendar.WEEK_OF_YEAR) == currentWeek &&
                calendar.get(Calendar.YEAR) == currentYear) {
                // Calendar.DAY_OF_WEEK: Sunday = 1, Monday = 2, ..., Saturday = 7
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val mappedIndex = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> -1
                }
                if (mappedIndex in 0..6) {
                    activeDays[mappedIndex] = true
                }
            }
        }
        activeDays
    }

    fun getSessionsForExercise(exerciseName: String): Flow<List<SessionWithSets>> {
        return workoutDao.getAllSessionsForExercise(exerciseName)
    }

    companion object {
        fun getCurrentDayOfWeek(): Int {
            // Calendar: Sunday = 1, Monday = 2, ..., Saturday = 7
            val calendar = Calendar.getInstance()
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }
    }
}

class WorkoutViewModelFactory(private val workoutDao: WorkoutDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(workoutDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

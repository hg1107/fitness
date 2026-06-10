package com.example.fitnesstracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data class LogExercise(val exerciseName: String) : NavKey
@Serializable data object History : NavKey

@Serializable data object Track : NavKey
@Serializable data class ActivitySummary(val activityId: Long) : NavKey
@Serializable data class ActivityDetail(val activityId: Long) : NavKey


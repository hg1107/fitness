package com.example.fitnesstracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Onboarding : NavKey
@Serializable data object Dashboard : NavKey
@Serializable data class LogExercise(val exerciseName: String) : NavKey
@Serializable data object History : NavKey
@Serializable data object Track : NavKey
@Serializable data class ActivitySummary(val activityId: Long) : NavKey
@Serializable data class ActivityDetail(val activityId: Long) : NavKey
@Serializable data object Nutrition : NavKey
@Serializable data class FoodSearch(val mealType: String) : NavKey
@Serializable data object Profile : NavKey
@Serializable data object BodyMeasurements : NavKey
@Serializable data object WorkoutPrograms : NavKey



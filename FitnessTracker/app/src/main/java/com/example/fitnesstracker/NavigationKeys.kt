package com.example.fitnesstracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data class LogExercise(val exerciseName: String) : NavKey
@Serializable data object History : NavKey

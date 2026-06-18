package com.example.fitnesstracker.util

import kotlin.math.*

object FitnessMath {
    /**
     * Calculates the distance between two geo-coordinates using the Haversine formula.
     * Returns distance in meters.
     */
    fun calculateHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Calculates estimated calories burned based on activity MET rating, weight in kg, and duration in seconds.
     */
    fun calculateCaloriesBurned(activityType: String, weightKg: Double, durationSec: Long): Double {
        val met = when (activityType) {
            "Walking" -> 3.8
            "Running" -> 8.0
            "Cycling" -> 7.5
            else -> 6.0
        }
        val hours = durationSec / 3600.0
        return met * weightKg * hours
    }
}

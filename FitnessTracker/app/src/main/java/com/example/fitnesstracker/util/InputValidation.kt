package com.example.fitnesstracker.util

/**
 * Centralized input validation and sanitization.
 * All user-supplied strings must pass through these helpers before
 * being persisted to the database or sent over the network.
 */
object InputValidation {

    /** Trims whitespace and caps to [maxLen] characters. For names, exercise names, etc. */
    fun sanitizeName(input: String, maxLen: Int = 100): String =
        input.trim().take(maxLen)

    /** Trims whitespace and caps session/activity notes to [maxLen] characters. */
    fun sanitizeNotes(input: String, maxLen: Int = 500): String =
        input.trim().take(maxLen)

    /**
     * Sanitizes a comma-separated preference list (foodLikes, foodDislikes, foodAllergies).
     * Caps to [maxLen] chars, trims each token, collapses duplicates.
     */
    fun sanitizeCommaList(input: String, maxLen: Int = 300): String {
        return input.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
            .take(maxLen)
    }

    /**
     * Returns a valid age Int in [5, 120] or null if invalid.
     */
    fun validateAge(input: String): Int? {
        val age = input.trim().toIntOrNull() ?: return null
        return if (age in 5..120) age else null
    }

    /**
     * Returns a valid weight in kg (or lbs when [isImperial] is true) or null if out of range.
     */
    fun validateWeight(input: String, isImperial: Boolean): Double? {
        val w = input.trim().toDoubleOrNull() ?: return null
        return if (isImperial) {
            if (w in 44.0..660.0) w else null   // 20 kg – 300 kg in lbs
        } else {
            if (w in 20.0..300.0) w else null
        }
    }

    /**
     * Returns a valid height in cm (or inches when [isImperial] is true) or null if out of range.
     */
    fun validateHeight(input: String, isImperial: Boolean): Double? {
        val h = input.trim().toDoubleOrNull() ?: return null
        return if (isImperial) {
            if (h in 20.0..100.0) h else null   // ~51 cm – 254 cm in inches
        } else {
            if (h in 50.0..250.0) h else null
        }
    }

    /** Validates and returns a water amount in ml in [50, 5000]. */
    fun validateWaterAmount(amountMl: Int): Int? =
        if (amountMl in 50..5000) amountMl else null

    /** Returns a quantity multiplier in (0, 100] or null. */
    fun validateFoodQuantity(input: String): Double? {
        val q = input.trim().toDoubleOrNull() ?: return null
        return if (q > 0.0 && q <= 100.0) q else null
    }
}

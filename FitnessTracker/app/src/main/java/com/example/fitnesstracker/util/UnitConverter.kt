package com.example.fitnesstracker.util

object UnitConverter {
    // Weight: 1 kg = 2.20462 lbs
    fun kgToLbs(kg: Double): Double {
        return kg * 2.20462
    }

    fun lbsToKg(lbs: Double): Double {
        return lbs / 2.20462
    }

    // Distance: meters to km, meters to miles
    fun metersToKm(meters: Double): Double {
        return meters / 1000.0
    }

    fun metersToMiles(meters: Double): Double {
        return meters * 0.000621371
    }

    // Speed: m/s to km/h, m/s to mph
    fun mpsToKmh(mps: Double): Double {
        return mps * 3.6
    }

    fun mpsToMph(mps: Double): Double {
        return mps * 2.23694
    }

    // Pace: seconds per kilometer to seconds per mile
    fun paceKmToMile(paceSecPerKm: Double): Double {
        return paceSecPerKm * 1.609344
    }

    // Height: cm to inches, inches to cm
    fun cmToInches(cm: Double): Double {
        return cm * 0.393701
    }

    fun inchesToCm(inches: Double): Double {
        return inches / 0.393701
    }
}

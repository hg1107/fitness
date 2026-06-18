package com.example.fitnesstracker.util

import android.content.Context
import android.content.SharedPreferences
import com.example.fitnesstracker.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import java.util.Calendar

object GeminiWorkoutCoach {

    private const val PREFS_NAME = "ai_coach_prefs"
    private const val KEY_COACH_TIP = "last_coach_tip"
    private const val KEY_LAST_FETCH_TIME = "last_fetch_time"

    fun isAvailable(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotBlank()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCachedTip(context: Context): String? {
        val prefs = getPrefs(context)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH_TIME, 0L)
        val now = System.currentTimeMillis()
        
        // Cache is valid for 7 days
        if (now - lastFetch < 7 * 24 * 60 * 60 * 1000L) {
            return prefs.getString(KEY_COACH_TIP, null)
        }
        return null
    }

    suspend fun getCoachTip(context: Context, volumeTrend: List<Pair<Long, Double>>): String {
        val cached = getCachedTip(context)
        if (cached != null) return cached

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return "Progressive overload is key! Try to increase weight or reps slightly in your next session to keep making progress."
        }

        try {
            val model = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )

            val trendText = if (volumeTrend.isEmpty()) {
                "No workouts logged yet."
            } else {
                volumeTrend.joinToString("\n") { (timestamp, volume) ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(timestamp))
                    "- $dateStr: $volume kg/lbs total volume"
                }
            }

            val prompt = """
                You are an elite, motivating personal trainer.
                Analyze the following recent training volume trend for a user:
                $trendText
                
                Provide a short, highly motivating, and actionable training tip (max 2 sentences) based on this trend.
                Keep it concise and positive. Do NOT include markdown styling or headers. Just output the plain text tip.
            """.trimIndent()

            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            val tip = response.text?.trim() ?: throw Exception("Empty response")
            if (tip.isNotEmpty()) {
                getPrefs(context).edit()
                    .putString(KEY_COACH_TIP, tip)
                    .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
                    .apply()
                return tip
            }
        } catch (e: Exception) {
            AppLogger.e("GeminiWorkoutCoach", "Failed to fetch coach tip from Gemini", e)
        }

        // Return a sensible default if it fails
        return "You're doing great! Keep showing up and focus on consistent, controlled repetitions for maximum hypertrophy."
    }
}

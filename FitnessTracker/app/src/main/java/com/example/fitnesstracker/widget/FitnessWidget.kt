package com.example.fitnesstracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.data.UserProfile
import com.example.fitnesstracker.data.WorkoutDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Home screen widget: today's planned exercises with completion state and
 * calories consumed vs. target. Tapping opens the app. Refreshed by the
 * launcher every 30 minutes (updatePeriodMillis).
 */
class FitnessWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = WorkoutDatabase.getDatabase(context)
        val planned = try {
            db.workoutDao().getPlannedExercisesForDay(currentDayOfWeek()).first()
        } catch (e: Exception) {
            emptyList()
        }
        val logged = try {
            db.workoutDao().getLoggedExerciseNamesSince(startOfDay()).first().toSet()
        } catch (e: Exception) {
            emptySet()
        }
        val profile = try {
            db.activityDao().getUserProfileSync()
        } catch (e: Exception) {
            null
        } ?: UserProfile()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val consumed = try {
            db.nutritionDao().getFoodLogsForDate(today).first().sumOf { it.calories * it.quantity }
        } catch (e: Exception) {
            0.0
        }
        val goal = calorieGoal(profile)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF111113))
                    .padding(12.dp)
                    .clickable(actionStartActivity(android.content.Intent(context, MainActivity::class.java).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }))
            ) {
                Text(
                    "Today's Plan",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(6.dp))
                if (planned.isEmpty()) {
                    Text(
                        "Rest day - recover well!",
                        style = TextStyle(color = ColorProvider(Color(0xFF9E9E9E)), fontSize = 12.sp)
                    )
                } else {
                    planned.take(4).forEach { exercise ->
                        val done = logged.contains(exercise.exerciseName)
                        Text(
                            (if (done) "\u2713 " else "\u25CB ") + exercise.exerciseName,
                            style = TextStyle(
                                color = ColorProvider(
                                    if (done) Color(0xFF00E676) else Color(0xFFBDBDBD)
                                ),
                                fontSize = 12.sp
                            )
                        )
                    }
                    if (planned.size > 4) {
                        Text(
                            "+${planned.size - 4} more",
                            style = TextStyle(color = ColorProvider(Color(0xFF9E9E9E)), fontSize = 11.sp)
                        )
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    "Calories: ${consumed.toInt()} / ${goal.toInt()} kcal",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF00E676)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    private fun calorieGoal(p: UserProfile): Double {
        val bmr = if (p.gender.equals("Male", ignoreCase = true)) {
            10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + 5
        } else {
            10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age - 161
        }
        val multiplier = when (p.activityLevel) {
            "Sedentary" -> 1.2
            "Lightly Active" -> 1.375
            "Moderately Active" -> 1.55
            "Very Active" -> 1.725
            "Athlete" -> 1.9
            else -> 1.55
        }
        val tdee = bmr * multiplier
        val calories = when (p.fitnessGoal) {
            "Fat Loss" -> tdee - 500
            "Lean Bulk" -> tdee + 300
            "Muscle Gain" -> tdee + 500
            else -> tdee
        }
        return calories.coerceAtLeast(1200.0)
    }

    private fun currentDayOfWeek(): Int {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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

    private fun startOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

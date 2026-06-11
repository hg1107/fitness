package com.example.fitnesstracker.util

import android.content.Context
import android.content.Intent
import com.example.fitnesstracker.data.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds CSV exports of the user's data and opens the system share sheet,
 * so logs can be backed up or analyzed in a spreadsheet without requiring
 * any storage permissions.
 */
object DataExporter {

    private fun csvEscape(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    suspend fun shareWorkoutsCsv(context: Context) {
        val csv = withContext(Dispatchers.IO) {
            val dao = WorkoutDatabase.getDatabase(context).workoutDao()
            val sessions = dao.getAllSessionsWithSets().first()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            buildString {
                append("date,exercise,set,weight_kg,reps,notes\n")
                sessions.forEach { item ->
                    item.sets.sortedBy { it.setIndex }.forEach { set ->
                        append(sdf.format(Date(item.session.timestamp)))
                        append(',').append(csvEscape(item.session.exerciseName))
                        append(',').append(set.setIndex + 1)
                        append(',').append(set.weight)
                        append(',').append(set.reps)
                        append(',').append(csvEscape(item.session.notes))
                        append('\n')
                    }
                }
            }
        }
        share(context, "FitnessTracker Workouts Export", csv)
    }

    suspend fun shareNutritionCsv(context: Context) {
        val csv = withContext(Dispatchers.IO) {
            val dao = WorkoutDatabase.getDatabase(context).nutritionDao()
            val logs = dao.getAllFoodLogs().first()
            buildString {
                append("date,meal,food,quantity,unit,calories,protein_g,carbs_g,fat_g,fiber_g\n")
                logs.forEach { log ->
                    append(log.date)
                    append(',').append(csvEscape(log.mealType))
                    append(',').append(csvEscape(log.foodName))
                    append(',').append(log.quantity)
                    append(',').append(csvEscape(log.servingUnit))
                    append(',').append(log.calories * log.quantity)
                    append(',').append(log.protein * log.quantity)
                    append(',').append(log.carbs * log.quantity)
                    append(',').append(log.fat * log.quantity)
                    append(',').append(log.fiber * log.quantity)
                    append('\n')
                }
            }
        }
        share(context, "FitnessTracker Nutrition Export", csv)
    }

    suspend fun shareActivitiesCsv(context: Context) {
        val csv = withContext(Dispatchers.IO) {
            val dao = WorkoutDatabase.getDatabase(context).activityDao()
            val activities = dao.getAllActivities().first()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            buildString {
                append("start,type,duration_s,distance_m,calories,avg_speed_mps,avg_pace_s_per_km,notes\n")
                activities.forEach { a ->
                    append(sdf.format(Date(a.startTime)))
                    append(',').append(csvEscape(a.activityType))
                    append(',').append(a.durationSeconds)
                    append(',').append(a.distanceMeters)
                    append(',').append(a.calories)
                    append(',').append(a.avgSpeed)
                    append(',').append(a.avgPace)
                    append(',').append(csvEscape(a.notes))
                    append('\n')
                }
            }
        }
        share(context, "FitnessTracker Activities Export", csv)
    }

    /**
     * Exports a recorded activity's GPS route as a GPX 1.1 track, shareable
     * with apps like Strava, Komoot, or any GPX viewer.
     */
    suspend fun shareActivityGpx(context: Context, activityId: Long) {
        val gpx = withContext(Dispatchers.IO) {
            val dao = WorkoutDatabase.getDatabase(context).activityDao()
            val activity = dao.getActivityById(activityId) ?: return@withContext null
            val points = dao.getPointsForActivitySync(activityId)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                append("<gpx version=\"1.1\" creator=\"FitnessTracker\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
                append("  <trk>\n")
                append("    <name>").append(activity.activityType).append(' ')
                    .append(sdf.format(Date(activity.startTime))).append("</name>\n")
                append("    <trkseg>\n")
                points.forEach { p ->
                    append("      <trkpt lat=\"").append(p.latitude)
                        .append("\" lon=\"").append(p.longitude).append("\">")
                    append("<time>").append(sdf.format(Date(p.timestamp))).append("</time>")
                    append("</trkpt>\n")
                }
                append("    </trkseg>\n  </trk>\n</gpx>\n")
            }
        }
        if (gpx != null) {
            share(context, "FitnessTracker GPX Export", gpx)
        }
    }

    private fun share(context: Context, subject: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }
}

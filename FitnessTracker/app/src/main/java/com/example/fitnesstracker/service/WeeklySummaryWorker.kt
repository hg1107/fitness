package com.example.fitnesstracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.fitnesstracker.data.WorkoutDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Weekly recap notification (Sunday evening): number of workouts, total
 * volume lifted with week-over-week trend, and distance covered.
 */
class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val db = WorkoutDatabase.getDatabase(applicationContext)
            val sessions = db.workoutDao().getAllSessionsWithSets().first()
            val activities = db.activityDao().getAllActivities().first()

            val weekStart = startOfWeek()
            val prevWeekStart = weekStart - 7L * 24 * 60 * 60 * 1000

            val thisWeek = sessions.filter { it.session.timestamp >= weekStart }
            val lastWeek = sessions.filter { it.session.timestamp in prevWeekStart until weekStart }

            val volume = thisWeek.flatMap { it.sets }.sumOf { it.weight * it.reps }
            val lastVolume = lastWeek.flatMap { it.sets }.sumOf { it.weight * it.reps }
            val distanceKm = activities
                .filter { it.startTime >= weekStart }
                .sumOf { it.distanceMeters } / 1000.0

            // Nothing to celebrate - stay silent rather than nagging
            if (thisWeek.isEmpty() && distanceKm == 0.0) return Result.success()

            val trend = when {
                lastVolume <= 0.0 -> ""
                volume >= lastVolume ->
                    " (up ${(((volume - lastVolume) / lastVolume) * 100).toInt()}% vs last week)"
                else ->
                    " (down ${(((lastVolume - volume) / lastVolume) * 100).toInt()}% vs last week)"
            }
            val text = String.format(
                Locale.US,
                "%d workout%s \u00b7 %.0f kg lifted%s \u00b7 %.1f km covered",
                thisWeek.size,
                if (thisWeek.size == 1) "" else "s",
                volume,
                trend,
                distanceKm
            )
            postNotification(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success()
    }

    private fun postNotification(text: String) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weekly Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Your weekly training recap" }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Your week in training")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Monday 00:00 of the current week, consistent with WorkoutViewModel stats
    private fun startOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val daysToSubtract = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
        return calendar.timeInMillis
    }

    companion object {
        private const val CHANNEL_ID = "weekly_summary_channel"
        private const val NOTIFICATION_ID = 3002
        private const val WORK_NAME = "weekly_summary"

        /** Schedules (or keeps) the weekly recap, first firing next Sunday at 7 PM. */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY || timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val initialDelay = next.timeInMillis - now.timeInMillis
            val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

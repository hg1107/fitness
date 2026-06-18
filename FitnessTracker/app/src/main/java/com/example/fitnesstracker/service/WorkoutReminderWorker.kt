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
import com.example.fitnesstracker.util.AppLogger
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Daily workout reminder: if the user has exercises planned for today and has
 * not logged any workout yet, posts a notification in the evening.
 */
class WorkoutReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val dao = WorkoutDatabase.getDatabase(applicationContext).workoutDao()
            val planned = dao.getPlannedExercisesForDay(currentDayOfWeek()).first()
            if (planned.isEmpty()) return Result.success()

            val loggedToday = dao.getLoggedExerciseNamesSince(startOfDay()).first()
            if (loggedToday.isNotEmpty()) return Result.success()

            postNotification(planned.size, planned.joinToString(", ") { it.exerciseName })
        } catch (e: Exception) {
            AppLogger.e("WorkoutReminderWorker", "Failed to run reminder check", e)
        }
        return Result.success()
    }

    private fun postNotification(count: Int, exercises: String) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Reminds you about planned workouts you haven't logged yet" }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val text = "$count exercise${if (count > 1) "s" else ""} waiting: $exercises"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Workout planned for today")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            AppLogger.e("WorkoutReminderWorker", "Failed to post notification", e)
        }
    }

    private fun currentDayOfWeek(): Int {
        // App convention: 1 = Monday, ..., 7 = Sunday
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

    companion object {
        private const val CHANNEL_ID = "workout_reminder_channel"
        private const val NOTIFICATION_ID = 3001
        private const val WORK_NAME = "workout_reminder"
        private const val REMINDER_HOUR = 18 // 6 PM local time

        /** Schedules (or keeps) the daily reminder, first firing at the next 6 PM. */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = next.timeInMillis - now.timeInMillis
            val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
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

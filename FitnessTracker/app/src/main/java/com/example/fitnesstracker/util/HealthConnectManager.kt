package com.example.fitnesstracker.util

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import com.example.fitnesstracker.data.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Manual push-sync to Health Connect: writes unsynced GPS activities and the
 * latest weight log. All calls are guarded for availability and API level.
 */
object HealthConnectManager {

    val PERMISSIONS: Set<String> = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
    )

    fun isAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return try {
            HealthConnectClient.getOrCreate(context)
                .permissionController
                .getGrantedPermissions()
                .containsAll(PERMISSIONS)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Writes all unsynced activities (exercise session + distance + calories)
     * and the latest weight log. Returns the number of activities synced.
     */
    suspend fun syncAll(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0
        return withContext(Dispatchers.IO) {
            try {
                val client = HealthConnectClient.getOrCreate(context)
                val db = WorkoutDatabase.getDatabase(context)
                val unsynced = db.activityDao().getAllActivities().first().filter { !it.isSynced }

                val records = mutableListOf<Record>()
                unsynced.forEach { a ->
                    val start = Instant.ofEpochMilli(a.startTime)
                    val endMillis = if (a.endTime > a.startTime) {
                        a.endTime
                    } else {
                        a.startTime + (a.durationSeconds.coerceAtLeast(1)) * 1000
                    }
                    val end = Instant.ofEpochMilli(endMillis)
                    val type = when (a.activityType) {
                        "Running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                        "Walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
                        "Cycling" -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
                        else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
                    }
                    records.add(
                        ExerciseSessionRecord(
                            startTime = start,
                            startZoneOffset = null,
                            endTime = end,
                            endZoneOffset = null,
                            exerciseType = type,
                            title = a.activityType
                        )
                    )
                    if (a.distanceMeters > 0) {
                        records.add(
                            DistanceRecord(
                                startTime = start,
                                startZoneOffset = null,
                                endTime = end,
                                endZoneOffset = null,
                                distance = Length.meters(a.distanceMeters)
                            )
                        )
                    }
                    if (a.calories > 0) {
                        records.add(
                            TotalCaloriesBurnedRecord(
                                startTime = start,
                                startZoneOffset = null,
                                endTime = end,
                                endZoneOffset = null,
                                energy = Energy.kilocalories(a.calories)
                            )
                        )
                    }
                }

                val latestWeight = db.nutritionDao().getLatestWeightLogSync()
                if (latestWeight != null) {
                    records.add(
                        WeightRecord(
                            time = Instant.ofEpochMilli(latestWeight.timestamp),
                            zoneOffset = null,
                            weight = Mass.kilograms(latestWeight.weightKg)
                        )
                    )
                }

                if (records.isNotEmpty()) {
                    client.insertRecords(records)
                }
                if (unsynced.isNotEmpty()) {
                    db.activityDao().markActivitiesAsSynced(unsynced.map { it.id })
                }
                unsynced.size
            } catch (e: Exception) {
                AppLogger.e("HealthConnectManager", "Sync to Health Connect failed", e)
                0
            }
        }
    }
}

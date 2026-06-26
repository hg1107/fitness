package com.example.fitnesstracker.util

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.fitnesstracker.data.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Full database backup & restore via the Storage Access Framework.
 * Backup checkpoints the WAL first so the single file contains all writes;
 * restore validates the picked file before replacing the live database.
 */
object BackupManager {

    private const val DB_NAME = "workout_database"

    suspend fun backupTo(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = WorkoutDatabase.getDatabase(context)
            // Flush the write-ahead log into the main database file
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
            val dbFile = context.getDatabasePath(DB_NAME)
            val out = context.contentResolver.openOutputStream(uri) ?: return@withContext false
            out.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Database backup failed", e)
            false
        }
    }

    suspend fun restoreFrom(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val temp = File(context.cacheDir, "restore_candidate.db")
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext false
            input.use { stream ->
                temp.outputStream().use { out -> stream.copyTo(out) }
            }

            // Validate: must be a SQLite DB containing the user_profile table
            val valid = try {
                SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY).use { check ->
                    check.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name='user_profile'",
                        null
                    ).use { cursor -> cursor.count > 0 }
                }
            } catch (e: Exception) {
                false
            }
            if (!valid) {
                temp.delete()
                return@withContext false
            }

            WorkoutDatabase.closeAndReset()
            val dbFile = context.getDatabasePath(DB_NAME)
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            temp.copyTo(dbFile, overwrite = true)
            temp.delete()
            true
        } catch (e: Exception) {
            AppLogger.e("BackupManager", "Database restore failed", e)
            false
        }
    }

    /** Relaunches the app so Room reopens the restored database cleanly. */
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}

package com.example.fitnesstracker.util

import android.util.Log
import com.example.fitnesstracker.BuildConfig

/**
 * A central logging utility that prints logs in debug builds and respects
 * production log configurations (e.g. could send to Crashlytics/Sentry or be silent).
 */
object AppLogger {

    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, msg)
        }
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, msg, tr)
        }
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        // In debug, log to logcat. In production, this can also log to Crashlytics.
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg, tr)
        }
    }
}

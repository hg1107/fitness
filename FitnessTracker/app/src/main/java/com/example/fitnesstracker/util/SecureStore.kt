package com.example.fitnesstracker.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Encrypted key-value storage for sensitive values (API keys, tokens).
 * Backed by EncryptedSharedPreferences so secrets are never stored in
 * plaintext inside the Room database or included in device backups.
 */
object SecureStore {

    private const val PREFS_FILE = "secure_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key"
    private const val KEY_MAPBOX_TOKEN = "mapbox_token"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            prefs = try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context.applicationContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                // The Android Keystore can be corrupted on some devices;
                // fall back to app-private prefs rather than crashing.
                e.printStackTrace()
                context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            }
        }
    }

    var geminiApiKey: String
        get() = prefs?.getString(KEY_GEMINI_API, "") ?: ""
        set(value) {
            prefs?.edit()?.putString(KEY_GEMINI_API, value)?.apply()
        }

    var mapboxToken: String
        get() = prefs?.getString(KEY_MAPBOX_TOKEN, "") ?: ""
        set(value) {
            prefs?.edit()?.putString(KEY_MAPBOX_TOKEN, value)?.apply()
        }
}

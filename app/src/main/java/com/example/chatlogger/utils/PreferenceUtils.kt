// File: app/src/main/java/com/example/chatlogger/utils/PreferenceUtils.kt
package com.example.chatlogger.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtils {
    private const val PREF_NAME = "chat_logger_prefs"
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return preferences.getString(key, defaultValue) ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return preferences.getLong(key, defaultValue)
    }

    fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun contains(key: String): Boolean {
        return preferences.contains(key)
    }

    fun isLoggerEnabled(): Boolean {
        return getBoolean(Constants.PREF_LOGGER_ENABLED, false)
    }

    fun setLoggerEnabled(enabled: Boolean) {
        putBoolean(Constants.PREF_LOGGER_ENABLED, enabled)
    }

    fun isDarkModeEnabled(): Boolean {
        return getBoolean(Constants.PREF_DARK_MODE, false)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        putBoolean(Constants.PREF_DARK_MODE, enabled)
    }

    fun getUserId(): String {
        return getString(Constants.PREF_USER_ID, "")
    }

    fun setUserId(userId: String) {
        putString(Constants.PREF_USER_ID, userId)
    }
}
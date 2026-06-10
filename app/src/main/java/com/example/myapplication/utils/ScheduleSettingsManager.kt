package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.data.model.ScheduleSettings
import com.google.gson.Gson

class ScheduleSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("schedule_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_SETTINGS = "schedule_settings"
    }
    
    fun saveSettings(settings: ScheduleSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_SETTINGS, json).apply()
    }
    
    fun getSettings(): ScheduleSettings {
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ScheduleSettings::class.java)
            } catch (e: Exception) {
                ScheduleSettings()
            }
        } else {
            ScheduleSettings()
        }
    }
    
    fun resetToDefault() {
        prefs.edit().remove(KEY_SETTINGS).apply()
    }
}


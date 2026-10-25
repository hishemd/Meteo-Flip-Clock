package com.htcweather.flipclock.utils

import android.content.Context
import android.content.SharedPreferences
import com.htcweather.flipclock.data.remote.RetrofitClient

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("htc_weather_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API, "")?.ifBlank { null } ?: RetrofitClient.DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var useCelsius: Boolean
        get() = prefs.getBoolean(KEY_CELSIUS, true)
        set(value) = prefs.edit().putBoolean(KEY_CELSIUS, value).apply()

    var autoRefreshMinutes: Int
        get() = prefs.getInt(KEY_REFRESH, 30)
        set(value) = prefs.edit().putInt(KEY_REFRESH, value).apply()

    var useGps: Boolean
        get() = prefs.getBoolean(KEY_GPS, true)
        set(value) = prefs.edit().putBoolean(KEY_GPS, value).apply()

    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SECONDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SECONDS, value).apply()

    var lastLat: Float
        get() = prefs.getFloat(KEY_LAT, 48.8566f)
        set(value) = prefs.edit().putFloat(KEY_LAT, value).apply()

    var lastLon: Float
        get() = prefs.getFloat(KEY_LON, 2.3522f)
        set(value) = prefs.edit().putFloat(KEY_LON, value).apply()

    var lastConditionId: Int
        get() = prefs.getInt(KEY_CONDITION, 800)
        set(value) = prefs.edit().putInt(KEY_CONDITION, value).apply()

    var lastCityName: String
        get() = prefs.getString(KEY_CITY, "Maison") ?: "Maison"
        set(value) = prefs.edit().putString(KEY_CITY, value).apply()

    var lastTemp: Float
        get() = prefs.getFloat(KEY_TEMP, 20f)
        set(value) = prefs.edit().putFloat(KEY_TEMP, value).apply()

    var lastTempMax: Float
        get() = prefs.getFloat(KEY_TEMP_MAX, 25f)
        set(value) = prefs.edit().putFloat(KEY_TEMP_MAX, value).apply()

    var lastTempMin: Float
        get() = prefs.getFloat(KEY_TEMP_MIN, 15f)
        set(value) = prefs.edit().putFloat(KEY_TEMP_MIN, value).apply()

    companion object {
        private const val KEY_API = "api_key"
        private const val KEY_CELSIUS = "use_celsius"
        private const val KEY_REFRESH = "auto_refresh"
        private const val KEY_GPS = "use_gps"
        private const val KEY_SECONDS = "show_seconds"
        private const val KEY_LAT = "last_lat"
        private const val KEY_LON = "last_lon"
        private const val KEY_CONDITION = "last_condition"
        private const val KEY_CITY = "last_city"
        private const val KEY_TEMP = "last_temp"
        private const val KEY_TEMP_MAX = "last_temp_max"
        private const val KEY_TEMP_MIN = "last_temp_min"
    }
}

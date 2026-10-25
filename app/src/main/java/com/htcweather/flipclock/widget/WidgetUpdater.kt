package com.htcweather.flipclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.htcweather.flipclock.R
import com.htcweather.flipclock.data.WeatherRepository
import com.htcweather.flipclock.data.local.AppDatabase
import com.htcweather.flipclock.data.model.WeatherType
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.ui.MainActivity
import com.htcweather.flipclock.utils.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object WidgetUpdater {

    fun weatherIcon(conditionId: Int): String = when (conditionId.toWeatherType()) {
        WeatherType.CLEAR -> "☀️"
        WeatherType.CLOUDS -> "☁️"
        WeatherType.RAIN, WeatherType.DRIZZLE -> "🌧️"
        WeatherType.THUNDERSTORM -> "⛈️"
        WeatherType.SNOW -> "❄️"
        WeatherType.MIST -> "🌫️"
        else -> "🌤️"
    }

    fun fillClock(views: RemoteViews, hour1: Int, hour2: Int, min1: Int, min2: Int) {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        views.setTextViewText(hour1, (h / 10).toString())
        views.setTextViewText(hour2, (h % 10).toString())
        views.setTextViewText(min1, (m / 10).toString())
        views.setTextViewText(min2, (m % 10).toString())
    }

    fun fillWeather(context: Context, views: RemoteViews, cityId: Int, tempId: Int, highLowId: Int, iconId: Int, dateId: Int?) {
        val prefs = AppPreferences(context)
        views.setTextViewText(cityId, prefs.lastCityName)
        views.setTextViewText(tempId, String.format(Locale.FRENCH, "%.1f°C", prefs.lastTemp))
        views.setTextViewText(highLowId, String.format(Locale.FRENCH, "%.0f° / %.0f°", prefs.lastTempMax, prefs.lastTempMin))
        views.setTextViewText(iconId, weatherIcon(prefs.lastConditionId))
        dateId?.let {
            val fmt = SimpleDateFormat("EEE d MMM", Locale.FRENCH)
            views.setTextViewText(it, fmt.format(Date()).replaceFirstChar { c -> c.uppercase() })
        }
    }

    fun setClick(context: Context, views: RemoteViews, rootId: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(rootId, pi)
    }

    fun refreshWeatherAsync(context: Context, manager: AppWidgetManager, appWidgetId: Int, updateFn: () -> Unit) {
        val prefs = AppPreferences(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = WeatherRepository(AppDatabase.getInstance(context).cityDao())
                repo.fetchWeather(prefs.lastLat.toDouble(), prefs.lastLon.toDouble(), prefs.apiKey)
                    .onSuccess { w ->
                        prefs.lastTemp = w.temperature.toFloat()
                        prefs.lastTempMax = w.tempMax.toFloat()
                        prefs.lastTempMin = w.tempMin.toFloat()
                        prefs.lastConditionId = w.conditionId
                        prefs.lastCityName = w.cityName
                        android.os.Handler(android.os.Looper.getMainLooper()).post { updateFn() }
                    }
            } catch (_: Exception) {}
        }
    }
}

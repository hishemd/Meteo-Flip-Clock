package com.htcweather.flipclock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.htcweather.flipclock.R
import com.htcweather.flipclock.data.WeatherRepository
import com.htcweather.flipclock.data.local.AppDatabase
import com.htcweather.flipclock.data.model.toWeatherType
import com.htcweather.flipclock.data.model.WeatherType
import com.htcweather.flipclock.ui.MainActivity
import com.htcweather.flipclock.utils.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherFlipWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateAppWidget(context, manager, id) }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}

    companion object {
        fun updateAppWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_flip)
            val prefs = AppPreferences(context)

            // Clock digits
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)
            views.setTextViewText(R.id.widgetHour1, (h / 10).toString())
            views.setTextViewText(R.id.widgetHour2, (h % 10).toString())
            views.setTextViewText(R.id.widgetMin1, (m / 10).toString())
            views.setTextViewText(R.id.widgetMin2, (m % 10).toString())

            // Date
            val dateFmt = SimpleDateFormat("EEE d MMM", Locale.FRENCH)
            views.setTextViewText(R.id.widgetDate, dateFmt.format(Date()).replaceFirstChar { it.uppercase() })

            // Cached weather from prefs
            views.setTextViewText(R.id.widgetCity, prefs.lastCityName)
            val temp = prefs.lastTemp
            views.setTextViewText(R.id.widgetTemp, String.format(Locale.FRENCH, "%.1f°C", temp))
            views.setTextViewText(
                R.id.widgetHighLow,
                String.format(Locale.FRENCH, "%.0f° / %.0f°", prefs.lastTempMax, prefs.lastTempMin)
            )

            // Weather icon emoji-style via text (remote views limited)
            val icon = when (prefs.lastConditionId.toWeatherType()) {
                WeatherType.CLEAR -> "☀️"
                WeatherType.CLOUDS -> "☁️"
                WeatherType.RAIN, WeatherType.DRIZZLE -> "🌧️"
                WeatherType.THUNDERSTORM -> "⛈️"
                WeatherType.SNOW -> "❄️"
                WeatherType.MIST -> "🌫️"
                else -> "🌤️"
            }
            views.setTextViewText(R.id.widgetWeatherIcon, icon)

            // Click → open app
            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pi)

            manager.updateAppWidget(appWidgetId, views)

            // Refresh weather in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = WeatherRepository(AppDatabase.getInstance(context).cityDao())
                    val result = repo.fetchWeather(
                        prefs.lastLat.toDouble(),
                        prefs.lastLon.toDouble(),
                        prefs.apiKey
                    )
                    result.onSuccess { w ->
                        prefs.lastTemp = w.temperature.toFloat()
                        prefs.lastTempMax = w.tempMax.toFloat()
                        prefs.lastTempMin = w.tempMin.toFloat()
                        prefs.lastConditionId = w.conditionId
                        prefs.lastCityName = w.cityName
                        // Re-update UI on main
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            updateAppWidget(context, manager, appWidgetId)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
}

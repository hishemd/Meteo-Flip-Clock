package com.htcweather.flipclock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.htcweather.flipclock.R

class Widget3Compact : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    companion object {
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_3_compact)
            WidgetUpdater.fillWeather(context, views, R.id.city, R.id.temp, R.id.highLow, R.id.icon, R.id.date)
            WidgetUpdater.setClick(context, views, R.id.widgetRoot)
            manager.updateAppWidget(id, views)
            WidgetUpdater.refreshWeatherAsync(context, manager, id) { update(context, manager, id) }
        }
    }
}

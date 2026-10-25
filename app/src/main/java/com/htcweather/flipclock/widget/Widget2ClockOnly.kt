package com.htcweather.flipclock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.htcweather.flipclock.R
import java.text.SimpleDateFormat
import java.util.*

class Widget2ClockOnly : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }
    companion object {
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_2_clock_only)
            WidgetUpdater.fillClock(views, R.id.h1, R.id.h2, R.id.m1, R.id.m2)
            val fmt = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)
            views.setTextViewText(R.id.date, fmt.format(Date()).replaceFirstChar { it.uppercase() })
            WidgetUpdater.setClick(context, views, R.id.widgetRoot)
            manager.updateAppWidget(id, views)
        }
    }
}

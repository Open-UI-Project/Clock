package org.openui.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.openui.clock.R

class AnalogClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            try {
                val views = buildViews(context, null)
                appWidgetManager.updateAppWidget(id, views)
            } catch (e: Exception) {
                Log.e("AnalogWidget", "Error in onUpdate", e)
            }
        }
        ClockWidgetManager.updateAllWidgets(context)
    }

    companion object {
        fun buildViews(context: Context, nextAlarm: String?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_analog_clock)

            val timePendingIntent = ClockWidgetManager.getTimePendingIntent(context)
            val alarmPendingIntent = ClockWidgetManager.getAlarmPendingIntent(context)

            views.setOnClickPendingIntent(R.id.analog_clock_view, timePendingIntent)
            views.setOnClickPendingIntent(R.id.analog_text_clock_date, timePendingIntent)
            views.setOnClickPendingIntent(R.id.layout_alarm_section, alarmPendingIntent)

            if (!nextAlarm.isNullOrBlank()) {
                views.setTextViewText(R.id.analog_alarm_text, nextAlarm)
                views.setViewVisibility(R.id.layout_alarm_section, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.layout_alarm_section, View.GONE)
            }

            return views
        }

        fun updateAll(context: Context, nextAlarm: String?) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AnalogClockWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds.isEmpty()) return

            for (widgetId in allWidgetIds) {
                try {
                    val views = buildViews(context, nextAlarm)
                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) {
                    Log.e("AnalogWidget", "Error updating widget $widgetId", e)
                }
            }
        }
    }
}

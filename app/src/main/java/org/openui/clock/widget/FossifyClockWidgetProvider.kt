package org.openui.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.openui.clock.R

class FossifyClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            try {
                val views = buildViews(context, null)
                appWidgetManager.updateAppWidget(id, views)
            } catch (e: Exception) {
                Log.e("FossifyWidget", "Error in onUpdate", e)
            }
        }
        ClockWidgetManager.updateAllWidgets(context)
    }

    companion object {
        fun buildViews(context: Context, nextAlarm: String?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_fossify_digital)

            val timePendingIntent = ClockWidgetManager.getTimePendingIntent(context)
            val alarmPendingIntent = ClockWidgetManager.getAlarmPendingIntent(context)

            views.setOnClickPendingIntent(R.id.fossify_text_clock_time, timePendingIntent)
            views.setOnClickPendingIntent(R.id.fossify_text_clock_date, timePendingIntent)
            views.setOnClickPendingIntent(R.id.fossify_alarm_section, alarmPendingIntent)

            if (!nextAlarm.isNullOrBlank()) {
                views.setTextViewText(R.id.fossify_alarm_text, nextAlarm)
                views.setViewVisibility(R.id.fossify_alarm_section, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.fossify_alarm_section, View.GONE)
            }

            return views
        }

        fun updateAll(context: Context, nextAlarm: String?) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, FossifyClockWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds.isEmpty()) return

            for (widgetId in allWidgetIds) {
                try {
                    val views = buildViews(context, nextAlarm)
                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) {
                    Log.e("FossifyWidget", "Error updating widget $widgetId", e)
                }
            }
        }
    }
}

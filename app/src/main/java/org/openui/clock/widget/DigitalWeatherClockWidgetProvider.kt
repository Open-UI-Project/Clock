package org.openui.clock.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.openui.clock.R
import org.openui.clock.weather.WeatherData
import org.openui.clock.weather.WeatherRepository

class DigitalWeatherClockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val weatherData = WeatherRepository(context).getWeatherData()
        for (id in appWidgetIds) {
            try {
                val views = buildViews(context, null, weatherData)
                appWidgetManager.updateAppWidget(id, views)
            } catch (e: Exception) {
                Log.e("SamsungWidget", "Error in onUpdate", e)
            }
        }
        ClockWidgetManager.updateAllWidgets(context)
    }

    companion object {
        fun buildViews(context: Context, nextAlarm: String?, weatherData: WeatherData): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_samsung_digital)

            val timePendingIntent = ClockWidgetManager.getTimePendingIntent(context)
            val alarmPendingIntent = ClockWidgetManager.getAlarmPendingIntent(context)
            val weatherPendingIntent = ClockWidgetManager.getWeatherPendingIntent(context)

            views.setOnClickPendingIntent(R.id.layout_time_section, timePendingIntent)
            views.setOnClickPendingIntent(R.id.layout_alarm_section, alarmPendingIntent)
            views.setOnClickPendingIntent(R.id.layout_weather_section, weatherPendingIntent)

            if (!nextAlarm.isNullOrBlank()) {
                views.setTextViewText(R.id.samsung_alarm_text, nextAlarm)
                views.setViewVisibility(R.id.layout_alarm_section, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.layout_alarm_section, View.GONE)
            }

            if (weatherData.isAvailable && weatherData.temperature.isNotBlank()) {
                views.setTextViewText(R.id.samsung_temp_text, weatherData.temperature)
                views.setImageViewResource(R.id.samsung_weather_icon, weatherData.iconType.drawableRes)
                views.setTextViewText(
                    R.id.samsung_condition_text,
                    weatherData.condition
                )
                views.setTextViewText(
                    R.id.samsung_city_text,
                    weatherData.city
                )
                views.setViewVisibility(R.id.layout_weather_section, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.layout_weather_section, View.GONE)
            }

            return views
        }

        fun updateAll(context: Context, nextAlarm: String?, weatherData: WeatherData) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, DigitalWeatherClockWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds.isEmpty()) return

            for (widgetId in allWidgetIds) {
                try {
                    val views = buildViews(context, nextAlarm, weatherData)
                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (e: Exception) {
                    Log.e("SamsungWidget", "Error updating widget $widgetId", e)
                }
            }
        }
    }
}

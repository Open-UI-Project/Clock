package org.openui.clock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openui.clock.MainActivity
import org.openui.clock.R
import org.openui.clock.data.Alarm
import org.openui.clock.data.ClockDatabase
import org.openui.clock.util.AlarmTimeUtils
import org.openui.clock.weather.BreezyWeatherHelper
import org.openui.clock.weather.WeatherData
import org.openui.clock.weather.WeatherRepository
import java.time.LocalDateTime
import java.util.Locale

object ClockWidgetManager {

    suspend fun getNextActiveAlarmText(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val db = ClockDatabase.getDatabase(context)
            val alarms: List<Alarm> = db.clockDao().getAllAlarms().firstOrNull() ?: emptyList()
            val enabledAlarms = alarms.filter { it.isEnabled }
            if (enabledAlarms.isEmpty()) return@withContext null

            var earliest: LocalDateTime? = null
            for (alarm in enabledAlarms) {
                val days = AlarmTimeUtils.parseDaysOfWeek(alarm.daysOfWeek)
                val nextDateTime = AlarmTimeUtils.getNextAlarmDateTime(alarm.hour, alarm.minute, days)
                if (earliest == null || nextDateTime.isBefore(earliest)) {
                    earliest = nextDateTime
                }
            }

            if (earliest != null) {
                val hourStr = String.format(Locale.getDefault(), "%02d", earliest.hour)
                val minStr = String.format(Locale.getDefault(), "%02d", earliest.minute)
                return@withContext "$hourStr:$minStr"
            }
        } catch (e: Exception) {}
        null
    }

    fun getTimePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra(MainActivity.EXTRA_TAB, "WORLD_CLOCK")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra(MainActivity.EXTRA_TAB, "ALARM")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getWeatherPendingIntent(context: Context): PendingIntent {
        val launchBreezy = BreezyWeatherHelper.getBreezyLaunchIntent(context)
        val intent = launchBreezy ?: Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra(MainActivity.EXTRA_TAB, "WORLD_CLOCK")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            103,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            val freshWeather = BreezyWeatherHelper.getOrFetchWeather(context)
            if (freshWeather != null && freshWeather.temperature.isNotBlank()) {
                WeatherRepository(context).saveWeatherData(freshWeather)
            }

            val nextAlarm = getNextActiveAlarmText(context)
            val weatherData = WeatherRepository(context).getWeatherData()

            withContext(Dispatchers.Main) {
                DigitalWeatherClockWidgetProvider.updateAll(context, nextAlarm, weatherData)
                OnePlusClockWidgetProvider.updateAll(context, nextAlarm, weatherData)
                FossifyClockWidgetProvider.updateAll(context, nextAlarm)
                CompactClockWidgetProvider.updateAll(context, nextAlarm, weatherData)
                AnalogClockWidgetProvider.updateAll(context, nextAlarm)
            }
        }
    }
}

package org.openui.clock.weather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.openui.clock.widget.ClockWidgetManager

class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        Log.d("WeatherUpdateReceiver", "Received broadcast action: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var weatherData: WeatherData? = null

                if (action == ACTION_GENERIC_WEATHER || action.contains("WEATHER")) {
                    weatherData = parseGenericWeatherIntent(intent)
                }

                if (weatherData == null || weatherData.temperature.isBlank()) {
                    weatherData = BreezyWeatherHelper.getOrFetchWeather(context, forceRefresh = true)
                }

                if (weatherData != null && weatherData.temperature.isNotBlank()) {
                    val repo = WeatherRepository(context)
                    repo.saveWeatherData(weatherData)
                    ClockWidgetManager.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                Log.e("WeatherUpdateReceiver", "Error processing weather update", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun parseGenericWeatherIntent(intent: Intent): WeatherData? {
        var tempStr = ""
        var conditionStr = ""
        var cityStr = ""
        var iconType = WeatherIconType.PARTLY_CLOUDY

        val extras = intent.extras

        val tempCandidates = listOf(
            "EXTRA_WEATHER_TEMPERATURE", "TEMPERATURE", "temp", "temperature",
            "current_temp", "EXTRA_TEMPERATURE", "temp_c"
        )
        for (key in tempCandidates) {
            val v = extras?.get(key)
            if (v != null) {
                tempStr = BreezyWeatherHelper.formatTemperature(v.toString())
                if (tempStr.isNotBlank()) break
            }
        }

        val condCandidates = listOf(
            "EXTRA_WEATHER_CONDITION", "WEATHER_TEXT", "DESCRIPTION", "condition",
            "weather_description", "EXTRA_CONDITION", "sky"
        )
        for (key in condCandidates) {
            val v = extras?.getString(key)
            if (!v.isNullOrBlank()) {
                conditionStr = v
                iconType = WeatherIconType.fromConditionText(v)
                break
            }
        }

        val locCandidates = listOf(
            "EXTRA_WEATHER_LOCATION", "LOCATION", "CITY", "city", "place"
        )
        for (key in locCandidates) {
            val v = extras?.getString(key)
            if (!v.isNullOrBlank()) {
                cityStr = v
                break
            }
        }

        val jsonPayload = intent.getStringExtra("JSON") ?: intent.getStringExtra("EXTRA_DATA")
        if (!jsonPayload.isNullOrBlank()) {
            try {
                val json = JSONObject(jsonPayload)
                if (tempStr.isBlank() && json.has("temp")) {
                    tempStr = BreezyWeatherHelper.formatTemperature(json.optString("temp"))
                }
                if (conditionStr.isBlank() && json.has("text")) {
                    conditionStr = json.optString("text")
                    iconType = WeatherIconType.fromConditionText(conditionStr)
                }
                if (cityStr.isBlank() && json.has("location")) {
                    cityStr = json.optString("location")
                }
            } catch (e: Exception) {}
        }

        return if (tempStr.isNotBlank()) {
            WeatherData(
                temperature = tempStr,
                condition = conditionStr,
                city = cityStr.ifBlank { "Breezy Weather" },
                iconType = iconType,
                isAvailable = true,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        } else {
            null
        }
    }

    companion object {
        const val ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"
        const val ACTION_UPDATE_NOTIFIER = "org.breezyweather.ACTION_UPDATE_NOTIFIER"
        const val ACTION_DEBUG_UPDATE_NOTIFIER = "org.breezyweather.debug.ACTION_UPDATE_NOTIFIER"
    }
}

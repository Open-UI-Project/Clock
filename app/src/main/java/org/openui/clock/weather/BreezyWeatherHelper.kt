package org.openui.clock.weather

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import kotlin.math.roundToInt

object BreezyWeatherHelper {

    private const val TAG = "BreezyWeatherHelper"

    val BREEZY_PACKAGE_NAMES = listOf(
        "org.breezyweather",
        "org.breezyweather.debug",
        "org.breezyweather.nightly",
        "org.breezyweather.fdroid"
    )

    private val CONTENT_URIS = listOf(
        "content://org.breezyweather.provider.weather/weather",
        "content://org.breezyweather.provider/weather",
        "content://org.breezyweather.provider.weather",
        "content://org.breezyweather.provider",
        "content://org.breezyweather.contentprovider/weather",
        "content://org.breezyweather.debug.provider.weather/weather",
        "content://org.breezyweather.debug.provider/weather",
        "content://org.breezyweather.nightly.provider.weather/weather",
        "content://org.breezyweather.fdroid.provider.weather/weather"
    )

    fun isBreezyWeatherInstalled(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in BREEZY_PACKAGE_NAMES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {

            }
        }
        return false
    }

    fun getBreezyLaunchIntent(context: Context): Intent? {
        val pm = context.packageManager
        for (pkg in BREEZY_PACKAGE_NAMES) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                return intent
            }
        }
        return null
    }

    suspend fun getOrFetchWeather(context: Context, forceRefresh: Boolean = false): WeatherData? {
        val repo = WeatherRepository(context)
        val cached = repo.getWeatherData()
        val isStale = (System.currentTimeMillis() - cached.lastUpdatedMillis) > 30 * 60 * 1000L

        if (!forceRefresh && cached.isAvailable && cached.temperature.isNotBlank() && !isStale) {
            return cached
        }

        if (isBreezyWeatherInstalled(context)) {
            val breezyData = queryWeather(context)
            if (breezyData != null && breezyData.temperature.isNotBlank()) {
                repo.saveWeatherData(breezyData)
                return breezyData
            }
        }

        val openMeteoData = OpenMeteoWeatherService.fetchWeather(context)
        if (openMeteoData != null && openMeteoData.temperature.isNotBlank()) {
            repo.saveWeatherData(openMeteoData)
            return openMeteoData
        }

        return if (cached.isAvailable && cached.temperature.isNotBlank()) cached else null
    }

    fun queryWeather(context: Context): WeatherData? {
        for (uriString in CONTENT_URIS) {
            try {
                val data = queryUri(context, Uri.parse(uriString))
                if (data != null && data.temperature.isNotBlank()) {
                    Log.d(TAG, "Successfully read weather from Breezy Weather provider: $uriString")
                    return data
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed query for $uriString: ${e.message}")
            }
        }
        return null
    }

    private fun queryUri(context: Context, uri: Uri): WeatherData? {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                return parseCursor(cursor)
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "SecurityException reading $uri: ${e.message}")
        } catch (e: Exception) {
            Log.d(TAG, "Exception reading $uri: ${e.message}")
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {}
        }
        return null
    }

    private fun parseCursor(cursor: Cursor): WeatherData {
        val columnNames = cursor.columnNames.map { it.lowercase() }
        Log.d(TAG, "Breezy cursor columns: $columnNames")

        var tempStr = ""
        var conditionStr = ""
        var cityStr = ""
        var iconType = WeatherIconType.PARTLY_CLOUDY

        fun findColumn(vararg candidates: String): Int {
            for (c in candidates) {
                val idx = columnNames.indexOf(c.lowercase())
                if (idx != -1) return idx
            }
            return -1
        }

        val tempIdx = findColumn(
            "temperature", "temp", "cur_temp", "current_temp",
            "temp_c", "temperature_c", "current_temperature", "extra_weather_temperature"
        )
        if (tempIdx != -1) {
            val raw = cursor.getString(tempIdx) ?: ""
            tempStr = formatTemperature(raw)
        }

        val condIdx = findColumn(
            "condition", "weather_text", "description", "sky_text",
            "weather_description", "status", "sky", "extra_weather_condition"
        )
        if (condIdx != -1) {
            conditionStr = cursor.getString(condIdx) ?: ""
        }

        val codeIdx = findColumn("weather_code", "code", "icon_id", "icon")
        if (codeIdx != -1) {
            val code = cursor.getInt(codeIdx)
            iconType = WeatherIconType.fromWeatherCode(code)
        } else if (conditionStr.isNotBlank()) {
            iconType = WeatherIconType.fromConditionText(conditionStr)
        }

        val cityIdx = findColumn("city", "location", "city_name", "location_name", "place", "extra_weather_location")
        if (cityIdx != -1) {
            cityStr = cursor.getString(cityIdx) ?: ""
        }

        return WeatherData(
            temperature = tempStr,
            condition = conditionStr,
            city = cityStr,
            iconType = iconType,
            isAvailable = tempStr.isNotBlank(),
            lastUpdatedMillis = System.currentTimeMillis(),
            source = "Breezy Weather"
        )
    }

    fun formatTemperature(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val numeric = trimmed.replace("°", "").replace("C", "").replace("F", "").trim()
        val floatVal = numeric.toFloatOrNull()
        return if (floatVal != null) {

            val cVal = when {
                floatVal > 2000 -> (floatVal / 10.0 - 273.15).roundToInt()
                floatVal > 150 -> (floatVal - 273.15).roundToInt()
                else -> floatVal.roundToInt()
            }
            val sign = if (cVal > 0) "+" else ""
            "$sign$cVal°"
        } else {
            if (trimmed.endsWith("°")) trimmed else "$trimmed°"
        }
    }
}

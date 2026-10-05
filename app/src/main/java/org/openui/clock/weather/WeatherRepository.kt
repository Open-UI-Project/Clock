package org.openui.clock.weather

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWeatherData(): WeatherData {
        val isAvailable = prefs.getBoolean(KEY_IS_AVAILABLE, false)
        val temp = prefs.getString(KEY_TEMPERATURE, "") ?: ""
        val condition = prefs.getString(KEY_CONDITION, "") ?: ""
        val city = prefs.getString(KEY_CITY, "") ?: ""
        val source = prefs.getString(KEY_SOURCE, "Open-Meteo") ?: "Open-Meteo"

        if (city == "Open-UI Weather" || (temp == "+12°" && condition == "Clear")) {
            clearWeatherData()
            return WeatherData(isAvailable = false)
        }

        val iconName = prefs.getString(KEY_ICON_TYPE, WeatherIconType.PARTLY_CLOUDY.name)
        val iconType = try {
            WeatherIconType.valueOf(iconName ?: WeatherIconType.PARTLY_CLOUDY.name)
        } catch (e: Exception) {
            WeatherIconType.PARTLY_CLOUDY
        }
        val lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0L)

        return WeatherData(
            temperature = temp,
            condition = condition,
            city = city,
            iconType = iconType,
            isAvailable = isAvailable && temp.isNotBlank(),
            lastUpdatedMillis = lastUpdated,
            source = source
        )
    }

    fun clearWeatherData() {
        prefs.edit()
            .remove(KEY_IS_AVAILABLE)
            .remove(KEY_TEMPERATURE)
            .remove(KEY_CONDITION)
            .remove(KEY_CITY)
            .remove(KEY_ICON_TYPE)
            .remove(KEY_LAST_UPDATED)
            .apply()
    }

    fun saveWeatherData(data: WeatherData) {
        prefs.edit()
            .putBoolean(KEY_IS_AVAILABLE, data.isAvailable)
            .putString(KEY_TEMPERATURE, data.temperature)
            .putString(KEY_CONDITION, data.condition)
            .putString(KEY_CITY, data.city)
            .putString(KEY_ICON_TYPE, data.iconType.name)
            .putLong(KEY_LAST_UPDATED, data.lastUpdatedMillis)
            .putString(KEY_SOURCE, data.source)
            .apply()
    }

    fun setCustomCityLocation(cityName: String, lat: Double, lon: Double) {
        prefs.edit()
            .putString(KEY_CUSTOM_CITY, cityName)
            .putString(KEY_CUSTOM_LAT, lat.toString())
            .putString(KEY_CUSTOM_LON, lon.toString())
            .apply()
    }

    fun getCustomCityLocation(): Triple<String, Double, Double>? {
        val city = prefs.getString(KEY_CUSTOM_CITY, null) ?: return null
        val latStr = prefs.getString(KEY_CUSTOM_LAT, null) ?: return null
        val lonStr = prefs.getString(KEY_CUSTOM_LON, null) ?: return null
        val lat = latStr.toDoubleOrNull() ?: return null
        val lon = lonStr.toDoubleOrNull() ?: return null
        return Triple(city, lat, lon)
    }

    fun clearCustomCityLocation() {
        prefs.edit()
            .remove(KEY_CUSTOM_CITY)
            .remove(KEY_CUSTOM_LAT)
            .remove(KEY_CUSTOM_LON)
            .apply()
    }

    fun formatLastUpdated(lastUpdatedMillis: Long): String {
        if (lastUpdatedMillis == 0L) return ""
        val sdf = SimpleDateFormat("HH:mm, d MMM", Locale.getDefault())
        return sdf.format(Date(lastUpdatedMillis))
    }

    companion object {
        private const val PREFS_NAME = "openui_weather_cache"
        private const val KEY_IS_AVAILABLE = "is_available"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_CONDITION = "condition"
        private const val KEY_CITY = "city"
        private const val KEY_ICON_TYPE = "icon_type"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_SOURCE = "source"
        private const val KEY_CUSTOM_CITY = "custom_city"
        private const val KEY_CUSTOM_LAT = "custom_lat"
        private const val KEY_CUSTOM_LON = "custom_lon"
    }
}

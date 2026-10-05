package org.openui.clock.weather

import androidx.annotation.DrawableRes
import org.openui.clock.R

enum class WeatherIconType(@DrawableRes val drawableRes: Int) {
    SUNNY(R.drawable.ic_weather_sunny),
    CLOUDY(R.drawable.ic_weather_cloudy),
    PARTLY_CLOUDY(R.drawable.ic_weather_partly_cloudy),
    RAINY(R.drawable.ic_weather_rainy),
    SNOWY(R.drawable.ic_weather_snowy),
    THUNDER(R.drawable.ic_weather_thunder);

    companion object {
        fun fromConditionText(text: String?): WeatherIconType {
            if (text == null) return PARTLY_CLOUDY
            val lower = text.lowercase()
            return when {
                lower.contains("гроза") || lower.contains("шторм") || lower.contains("thunder") || lower.contains("storm") -> THUNDER
                lower.contains("снег") || lower.contains("метель") || lower.contains("snow") || lower.contains("flurry") -> SNOWY
                lower.contains("дожд") || lower.contains("ливень") || lower.contains("морос") || lower.contains("rain") || lower.contains("drizzle") || lower.contains("shower") -> RAINY
                lower.contains("ясно") || lower.contains("солн") || lower.contains("clear") || lower.contains("sunny") -> SUNNY
                lower.contains("пасмур") || lower.contains("облач") || lower.contains("туман") || lower.contains("cloud") || lower.contains("overcast") || lower.contains("fog") -> {
                    if (lower.contains("перемен") || lower.contains("проясн") || lower.contains("partly")) PARTLY_CLOUDY else CLOUDY
                }
                else -> PARTLY_CLOUDY
            }
        }

        fun fromWeatherCode(code: Int): WeatherIconType {
            return when (code) {
                0, 1 -> SUNNY
                2 -> PARTLY_CLOUDY
                3, 45, 48 -> CLOUDY
                51, 53, 55, 61, 63, 65, 80, 81, 82 -> RAINY
                71, 73, 75, 77, 85, 86 -> SNOWY
                95, 96, 99 -> THUNDER
                else -> PARTLY_CLOUDY
            }
        }
    }
}

data class WeatherData(
    val temperature: String = "",
    val condition: String = "",
    val city: String = "",
    val iconType: WeatherIconType = WeatherIconType.PARTLY_CLOUDY,
    val isAvailable: Boolean = false,
    val lastUpdatedMillis: Long = 0L,
    val source: String = "Open-Meteo"
)

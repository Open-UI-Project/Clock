package org.openui.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openui.clock.weather.BreezyWeatherHelper
import org.openui.clock.weather.WeatherIconType

class WeatherUnitTest {

    @Test
    fun testFormatTemperature() {
        assertEquals("+21°", BreezyWeatherHelper.formatTemperature("21"))
        assertEquals("+21°", BreezyWeatherHelper.formatTemperature("+21°C"))
        assertEquals("-5°", BreezyWeatherHelper.formatTemperature("-5.2"))
        assertEquals("0°", BreezyWeatherHelper.formatTemperature("0"))
    }

    @Test
    fun testWeatherIconFromCondition() {
        assertEquals(WeatherIconType.SUNNY, WeatherIconType.fromConditionText("Ясно"))
        assertEquals(WeatherIconType.RAINY, WeatherIconType.fromConditionText("Небольшой дождь"))
        assertEquals(WeatherIconType.SNOWY, WeatherIconType.fromConditionText("Снегопад"))
        assertEquals(WeatherIconType.THUNDER, WeatherIconType.fromConditionText("Гроза"))
        assertEquals(WeatherIconType.PARTLY_CLOUDY, WeatherIconType.fromConditionText("Переменная облачность"))
        assertEquals(WeatherIconType.CLOUDY, WeatherIconType.fromConditionText("Пасмурно"))
    }
}

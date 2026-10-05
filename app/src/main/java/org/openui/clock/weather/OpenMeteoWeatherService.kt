package org.openui.clock.weather

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class CitySearchResult(
    val name: String,
    val country: String,
    val admin1: String,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = when {
            admin1.isNotBlank() && admin1 != name -> "$name, $admin1"
            country.isNotBlank() -> "$name, $country"
            else -> name
        }
}

object OpenMeteoWeatherService {

    private const val TAG = "OpenMeteoWeather"
    private val cityWeatherCache = ConcurrentHashMap<String, WeatherData>()

    suspend fun fetchWeather(context: Context): WeatherData? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            return@withContext null
        }

        try {
            val repo = WeatherRepository(context)
            val custom = repo.getCustomCityLocation()
            val (lat, lon, cityName) = if (custom != null) {
                Triple(custom.second, custom.third, custom.first)
            } else {
                val resolved = resolveCoordinates(context)
                Triple(resolved.latitude, resolved.longitude, resolved.city)
            }

            return@withContext fetchWeatherForCoordinates(lat, lon, cityName)
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun fetchWeatherForCity(cityName: String, timeZoneId: String): WeatherData? = withContext(Dispatchers.IO) {
        if (cityName.isBlank()) return@withContext null
        val cacheKey = "$cityName:$timeZoneId"
        val cached = cityWeatherCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.lastUpdatedMillis) < 30 * 60 * 1000L) {
            return@withContext cached
        }

        try {
            val searchList = searchCities(cityName)
            val matched = searchList.firstOrNull()
            val data = if (matched != null) {
                fetchWeatherForCoordinates(matched.latitude, matched.longitude, cityName)
            } else {
                val tzCoords = getCoordsFromTimeZone(timeZoneId)
                fetchWeatherForCoordinates(tzCoords.latitude, tzCoords.longitude, cityName)
            }

            if (data != null) {
                cityWeatherCache[cacheKey] = data
            }
            return@withContext data
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun fetchWeatherForCoordinates(lat: Double, lon: Double, cityName: String): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?" +
                    "latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,snowfall,weather_code,is_day" +
                    "&timezone=auto"

            val jsonResponse = httpGet(urlString) ?: return@withContext null
            val rootJson = JSONObject(jsonResponse)
            val current = rootJson.optJSONObject("current") ?: return@withContext null

            val tempDouble = current.optDouble("temperature_2m", Double.NaN)
            if (tempDouble.isNaN()) {
                return@withContext null
            }
            val tempInt = tempDouble.roundToInt()
            val tempFormatted = if (tempInt > 0) "+$tempInt°" else "$tempInt°"

            val weatherCode = current.optInt("weather_code", 0)
            val iconType = WeatherIconType.fromWeatherCode(weatherCode)
            val conditionText = getConditionRussianText(weatherCode)

            return@withContext WeatherData(
                temperature = tempFormatted,
                condition = conditionText,
                city = cityName,
                iconType = iconType,
                isAvailable = true,
                lastUpdatedMillis = System.currentTimeMillis(),
                source = "Open-Meteo"
            )
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun searchCities(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val isRussian = Locale.getDefault().language.equals("ru", ignoreCase = true)
            val lang = if (isRussian) "ru" else "en"
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=$lang&format=json"

            val jsonStr = httpGet(url) ?: return@withContext emptyList()
            val root = JSONObject(jsonStr)
            val resultsArr = root.optJSONArray("results") ?: return@withContext emptyList()

            val list = mutableListOf<CitySearchResult>()
            for (i in 0 until resultsArr.length()) {
                val item = resultsArr.getJSONObject(i)
                val name = item.optString("name", "")
                val country = item.optString("country", "")
                val admin1 = item.optString("admin1", "")
                val lat = item.optDouble("latitude", Double.NaN)
                val lon = item.optDouble("longitude", Double.NaN)
                if (name.isNotBlank() && !lat.isNaN() && !lon.isNaN()) {
                    list.add(CitySearchResult(name, country, admin1, lat, lon))
                }
            }
            return@withContext list
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    private data class LocationCoords(val latitude: Double, val longitude: Double, val city: String)

    @SuppressLint("MissingPermission")
    private suspend fun resolveCoordinates(context: Context): LocationCoords {
        val hasCoarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasCoarse || hasFine) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    val providers = listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.PASSIVE_PROVIDER
                    )
                    var bestLocation: Location? = null
                    for (prov in providers) {
                        try {
                            if (lm.isProviderEnabled(prov)) {
                                val loc = lm.getLastKnownLocation(prov)
                                if (loc != null && (bestLocation == null || loc.time > bestLocation.time)) {
                                    bestLocation = loc
                                }
                            }
                        } catch (e: Exception) {}
                    }

                    if (bestLocation != null && (System.currentTimeMillis() - bestLocation.time) < 2 * 3600 * 1000L) {
                        return LocationCoords(bestLocation.latitude, bestLocation.longitude, resolveCityFromTimeZone())
                    }

                    val activeLoc = withTimeoutOrNull(3000L) {
                        requestSingleLocationUpdate(lm)
                    }
                    if (activeLoc != null) {
                        return LocationCoords(activeLoc.latitude, activeLoc.longitude, resolveCityFromTimeZone())
                    } else if (bestLocation != null) {
                        return LocationCoords(bestLocation.latitude, bestLocation.longitude, resolveCityFromTimeZone())
                    }
                }
            } catch (e: Exception) {}
        }

        val tzFallback = fallbackCoordsFromTimeZone()
        val tzId = TimeZone.getDefault().id
        if (tzId.equals("UTC", ignoreCase = true) || tzId.equals("GMT", ignoreCase = true)) {
            val ipCoords = fetchIpLocation()
            if (ipCoords != null) {
                return ipCoords
            }
        }

        return tzFallback
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocationUpdate(lm: LocationManager): Location? = suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                try {
                    lm.removeUpdates(this)
                } catch (e: Exception) {}
                if (cont.isActive) {
                    cont.resume(location)
                }
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            var requested = false
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                requested = true
            } else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                requested = true
            }
            if (!requested && cont.isActive) {
                cont.resume(null)
            }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }

        cont.invokeOnCancellation {
            try {
                lm.removeUpdates(listener)
            } catch (e: Exception) {}
        }
    }

    private fun fetchIpLocation(): LocationCoords? {
        try {
            val jsonStr = httpGet("http://ip-api.com/json") ?: httpGet("https://ipapi.co/json")
            if (jsonStr != null) {
                val json = JSONObject(jsonStr)
                val status = json.optString("status", "")
                if (status.isEmpty() || status.equals("success", ignoreCase = true)) {
                    val lat = json.optDouble("lat", Double.NaN).let {
                        if (it.isNaN()) json.optDouble("latitude", Double.NaN) else it
                    }
                    val lon = json.optDouble("lon", Double.NaN).let {
                        if (it.isNaN()) json.optDouble("longitude", Double.NaN) else it
                    }
                    val city = json.optString("city", "").ifBlank {
                        json.optString("regionName", "")
                    }
                    if (!lat.isNaN() && !lon.isNaN()) {
                        return LocationCoords(lat, lon, city)
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun getCoordsFromTimeZone(tzId: String): LocationCoords {
        return when {
            tzId.contains("Moscow", ignoreCase = true) -> LocationCoords(55.7558, 37.6173, "Москва")
            tzId.contains("Yekaterinburg", ignoreCase = true) -> LocationCoords(56.8389, 60.6057, "Екатеринбург")
            tzId.contains("Novosibirsk", ignoreCase = true) -> LocationCoords(55.0084, 82.9357, "Новосибирск")
            tzId.contains("Samara", ignoreCase = true) -> LocationCoords(53.1959, 50.1002, "Самара")
            tzId.contains("Krasnoyarsk", ignoreCase = true) -> LocationCoords(56.0153, 92.8932, "Красноярск")
            tzId.contains("Irkutsk", ignoreCase = true) -> LocationCoords(52.2871, 104.3050, "Иркутск")
            tzId.contains("Vladivostok", ignoreCase = true) -> LocationCoords(43.1155, 131.8855, "Владивосток")
            tzId.contains("Kaliningrad", ignoreCase = true) -> LocationCoords(54.7104, 20.4522, "Калининград")
            tzId.contains("Volgograd", ignoreCase = true) -> LocationCoords(48.7080, 44.5133, "Волгоград")
            tzId.contains("Saratov", ignoreCase = true) -> LocationCoords(51.5406, 46.0086, "Саратов")
            tzId.contains("Ulyanovsk", ignoreCase = true) -> LocationCoords(54.3142, 48.4031, "Ульяновск")
            tzId.contains("Omsk", ignoreCase = true) -> LocationCoords(54.9885, 73.3242, "Омск")
            tzId.contains("Barnaul", ignoreCase = true) -> LocationCoords(53.3548, 83.7698, "Барнаул")
            tzId.contains("Tomsk", ignoreCase = true) -> LocationCoords(56.4977, 84.9744, "Томск")
            tzId.contains("Novokuznetsk", ignoreCase = true) -> LocationCoords(53.7596, 87.1216, "Новокузнецк")
            tzId.contains("Yakutsk", ignoreCase = true) -> LocationCoords(62.0355, 129.6755, "Якутск")
            tzId.contains("Chita", ignoreCase = true) -> LocationCoords(52.0336, 113.5009, "Чита")
            tzId.contains("Magadan", ignoreCase = true) -> LocationCoords(59.5638, 150.8037, "Магадан")
            tzId.contains("Sakhalin", ignoreCase = true) -> LocationCoords(46.9592, 142.7386, "Южно-Сахалинск")
            tzId.contains("Kamchatka", ignoreCase = true) -> LocationCoords(53.0452, 158.6508, "Петропавловск-Камчатский")
            tzId.contains("Anadyr", ignoreCase = true) -> LocationCoords(64.7342, 177.5103, "Анадырь")
            tzId.contains("Almaty", ignoreCase = true) -> LocationCoords(43.2220, 76.8512, "Алматы")
            tzId.contains("Tashkent", ignoreCase = true) -> LocationCoords(41.2995, 69.2401, "Ташкент")
            tzId.contains("Minsk", ignoreCase = true) -> LocationCoords(53.9006, 27.5590, "Минск")
            tzId.contains("Kyiv", ignoreCase = true) || tzId.contains("Kiev", ignoreCase = true) -> LocationCoords(50.4501, 30.5234, "Киев")
            tzId.contains("London", ignoreCase = true) -> LocationCoords(51.5074, -0.1278, "London")
            tzId.contains("Berlin", ignoreCase = true) -> LocationCoords(52.5200, 13.4050, "Berlin")
            tzId.contains("Paris", ignoreCase = true) -> LocationCoords(48.8566, 2.3522, "Paris")
            tzId.contains("New_York", ignoreCase = true) -> LocationCoords(40.7128, -74.0060, "New York")
            tzId.contains("Tokyo", ignoreCase = true) -> LocationCoords(35.6762, 139.6503, "Tokyo")
            tzId.contains("Dubai", ignoreCase = true) -> LocationCoords(25.2048, 55.2708, "Dubai")
            tzId.contains("Singapore", ignoreCase = true) -> LocationCoords(1.3521, 103.8198, "Singapore")
            tzId.contains("Hong_Kong", ignoreCase = true) -> LocationCoords(22.3193, 114.1694, "Hong Kong")
            tzId.contains("Sydney", ignoreCase = true) -> LocationCoords(-33.8688, 151.2093, "Sydney")
            else -> {
                val cityPart = tzId.substringAfterLast("/").replace("_", " ")
                LocationCoords(55.7558, 37.6173, cityPart.ifBlank { "Москва" })
            }
        }
    }

    private fun fallbackCoordsFromTimeZone(): LocationCoords {
        return getCoordsFromTimeZone(TimeZone.getDefault().id)
    }

    private fun resolveCityFromTimeZone(): String {
        val tzId = TimeZone.getDefault().id
        return when {
            tzId.contains("Moscow", ignoreCase = true) -> "Москва"
            tzId.contains("Yekaterinburg", ignoreCase = true) -> "Екатеринбург"
            tzId.contains("Novosibirsk", ignoreCase = true) -> "Новосибирск"
            tzId.contains("Samara", ignoreCase = true) -> "Самара"
            tzId.contains("Krasnoyarsk", ignoreCase = true) -> "Красноярск"
            tzId.contains("Irkutsk", ignoreCase = true) -> "Иркутск"
            tzId.contains("Vladivostok", ignoreCase = true) -> "Владивосток"
            tzId.contains("Kaliningrad", ignoreCase = true) -> "Калининград"
            tzId.contains("Volgograd", ignoreCase = true) -> "Волгоград"
            tzId.contains("Saratov", ignoreCase = true) -> "Саратов"
            tzId.contains("Omsk", ignoreCase = true) -> "Омск"
            tzId.contains("Barnaul", ignoreCase = true) -> "Барнаул"
            tzId.contains("Tomsk", ignoreCase = true) -> "Томск"
            tzId.contains("Almaty", ignoreCase = true) -> "Алматы"
            tzId.contains("Minsk", ignoreCase = true) -> "Минск"
            else -> tzId.substringAfterLast("/").replace("_", " ").ifBlank { "Москва" }
        }
    }

    private fun getConditionRussianText(wmoCode: Int): String {
        val isRussian = Locale.getDefault().language.equals("ru", ignoreCase = true)
        return if (isRussian) {
            when (wmoCode) {
                0 -> "Ясно"
                1 -> "В основном ясно"
                2 -> "Переменная облачность"
                3 -> "Пасмурно"
                45, 48 -> "Туман"
                51, 53, 55 -> "Морось"
                56, 57 -> "Ледяная морось"
                61 -> "Небольшой дождь"
                63 -> "Дождь"
                65 -> "Сильный дождь"
                66, 67 -> "Ледяной дождь"
                71, 73, 75 -> "Снег"
                77 -> "Снежная крупа"
                80 -> "Кратковременный дождь"
                81, 82 -> "Ливень"
                85, 86 -> "Снегопад"
                95 -> "Гроза"
                96, 99 -> "Гроза с градом"
                else -> "Облачно"
            }
        } else {
            when (wmoCode) {
                0 -> "Clear sky"
                1 -> "Mainly clear"
                2 -> "Partly cloudy"
                3 -> "Overcast"
                45, 48 -> "Fog"
                51, 53, 55 -> "Drizzle"
                56, 57 -> "Freezing Drizzle"
                61 -> "Slight rain"
                63 -> "Rain"
                65 -> "Heavy rain"
                66, 67 -> "Freezing rain"
                71, 73, 75 -> "Snow fall"
                77 -> "Snow grains"
                80, 81, 82 -> "Rain showers"
                85, 86 -> "Snow showers"
                95 -> "Thunderstorm"
                96, 99 -> "Thunderstorm with hail"
                else -> "Cloudy"
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "OpenUIClock/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

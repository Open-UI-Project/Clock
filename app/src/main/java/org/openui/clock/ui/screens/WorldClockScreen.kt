package org.openui.clock.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.openui.clock.R
import org.openui.clock.data.WorldClockCity
import org.openui.clock.ui.components.CitySearchDialog
import org.openui.clock.ui.components.WeatherSettingsDialog
import org.openui.clock.weather.BreezyWeatherHelper
import org.openui.clock.weather.OpenMeteoWeatherService
import org.openui.clock.weather.WeatherData
import org.openui.clock.weather.WeatherRepository
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

private val SelectionPillShape = RoundedCornerShape(32.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorldClockScreen(
    cities: List<WorldClockCity>,
    onAddCity: (WorldClockCity) -> Unit,
    onDeleteCity: (WorldClockCity) -> Unit,
    externalShowAddDialog: Boolean = false,
    onExternalShowAddDialogHandled: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showCitySearch by remember { mutableStateOf(false) }
    var showWeatherDialog by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedCityIds by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChange(isSelectionMode)
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedCityIds = emptySet()
    }

    val repo = remember { WeatherRepository(context) }
    var weatherData by remember { mutableStateOf(repo.getWeatherData()) }

    LaunchedEffect(showWeatherDialog) {
        if (!showWeatherDialog) {
            weatherData = repo.getWeatherData()
        }
    }

    LaunchedEffect(Unit) {
        val fresh = BreezyWeatherHelper.getOrFetchWeather(context)
        if (fresh != null) {
            weatherData = fresh
        }
    }

    LaunchedEffect(externalShowAddDialog) {
        if (externalShowAddDialog) {
            showCitySearch = true
            onExternalShowAddDialogHandled()
        }
    }

    var localTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            localTimeMillis = System.currentTimeMillis()
        }
    }

    val headerTimeStr = remember(localTimeMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(localTimeMillis)
    }

    val headerZoneStr = remember {
        val tz = java.util.TimeZone.getDefault()
        val name = tz.getDisplayName(false, java.util.TimeZone.LONG, Locale.getDefault())
        val id = tz.id.substringAfterLast("/").replace("_", " ")
        if (name.contains(id, ignoreCase = true)) {
            name
        } else {
            when (tz.id) {
                "Asia/Yekaterinburg" -> context.getString(R.string.tz_yekaterinburg)
                "Europe/Moscow" -> context.getString(R.string.tz_moscow)
                else -> "$id, $name"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = headerTimeStr,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = headerZoneStr,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (weatherData.isAvailable && weatherData.temperature.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF262035))
                                .clickable { showWeatherDialog = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = weatherData.iconType.drawableRes),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${weatherData.temperature}  ${weatherData.condition} (${weatherData.city})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF262035).copy(alpha = 0.6f))
                                .clickable { showWeatherDialog = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_weather_sunny),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Настроить погоду (Open-Meteo)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA5B4FC)
                            )
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showCitySearch = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { showWeatherDialog = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_weather_sunny),
                                contentDescription = "Погода",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            items(cities, key = { it.id }) { city ->
                val isSelected = selectedCityIds.contains(city.id)
                CityClockCard(
                    city = city,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onClick = {
                        if (isSelectionMode) {
                            selectedCityIds = if (isSelected) {
                                selectedCityIds - city.id
                            } else {
                                selectedCityIds + city.id
                            }
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            isSelectionMode = true
                            selectedCityIds = setOf(city.id)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = SelectionPillShape,
                color = Color(0xFF252733).copy(alpha = 0.95f),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            isSelectionMode = false
                            selectedCityIds = emptySet()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    val allSelected = cities.isNotEmpty() && selectedCityIds.size == cities.size
                    TextButton(
                        onClick = {
                            selectedCityIds = if (allSelected) {
                                emptySet()
                            } else {
                                cities.map { it.id }.toSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                            color = Color(0xFFC4B5FD),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            val toDelete = cities.filter { selectedCityIds.contains(it.id) }
                            toDelete.forEach { onDeleteCity(it) }
                            isSelectionMode = false
                            selectedCityIds = emptySet()
                        },
                        enabled = selectedCityIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFDC2626).copy(alpha = 0.35f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.delete),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (showCitySearch) {
            CitySearchDialog(
                onDismiss = { showCitySearch = false },
                onCitySelected = { newCity ->
                    onAddCity(newCity)
                    showCitySearch = false
                }
            )
        }

        if (showWeatherDialog) {
            WeatherSettingsDialog(
                onDismiss = {
                    showWeatherDialog = false
                    weatherData = repo.getWeatherData()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CityClockCard(
    city: WorldClockCity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeDiff = getCityTimeDifference(city.timeZoneId)
    val currentTime = city.getCurrentTimeFormatted()

    var cityWeather by remember { mutableStateOf<WeatherData?>(null) }

    LaunchedEffect(city.cityName, city.timeZoneId) {
        val w = OpenMeteoWeatherService.fetchWeatherForCity(city.cityName, city.timeZoneId)
        if (w != null) {
            cityWeather = w
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A2140) else Color(0xFF1E1F28).copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF6366F1).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.05f)),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF6366F1) else Color.Transparent)
                        .border(2.dp, if (isSelected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.cityName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeDiff,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (cityWeather != null && cityWeather!!.temperature.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(id = cityWeather!!.iconType.drawableRes),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cityWeather!!.temperature,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }
            }
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun getCityTimeDifference(timeZoneId: String): String {
    val context = LocalContext.current
    return try {
        val localZone = ZoneId.systemDefault()
        val targetZone = ZoneId.of(timeZoneId)
        if (localZone.id == targetZone.id) {
            return context.getString(R.string.local_timezone)
        }

        val nowLocal = ZonedDateTime.now(localZone)
        val nowTarget = ZonedDateTime.now(targetZone)

        val localOffset = localZone.rules.getOffset(nowLocal.toInstant()).totalSeconds
        val targetOffset = targetZone.rules.getOffset(nowTarget.toInstant()).totalSeconds

        val diffSeconds = targetOffset - localOffset
        val diffHours = Math.abs(diffSeconds / 3600)
        val diffMinutes = Math.abs((diffSeconds % 3600) / 60)

        val hStr = context.getString(R.string.hours_short)
        val mStr = context.getString(R.string.minutes_short)

        val timeString = if (diffMinutes == 0) {
            "$diffHours $hStr"
        } else {
            "$diffHours $hStr $diffMinutes $mStr"
        }

        val diffString = if (diffSeconds < 0) {
            context.getString(R.string.earlier, timeString)
        } else if (diffSeconds > 0) {
            context.getString(R.string.later, timeString)
        } else {
            context.getString(R.string.same_time)
        }

        val localDate = nowLocal.toLocalDate()
        val targetDate = nowTarget.toLocalDate()

        val dayString = when {
            targetDate.isBefore(localDate) -> context.getString(R.string.yesterday)
            targetDate.isAfter(localDate) -> context.getString(R.string.tomorrow)
            else -> ""
        }

        "$diffString$dayString"
    } catch (e: Exception) {
        ""
    }
}

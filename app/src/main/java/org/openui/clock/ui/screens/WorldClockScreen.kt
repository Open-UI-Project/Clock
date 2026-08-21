package org.openui.clock.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.openui.clock.data.WorldClockCity
import org.openui.clock.ui.components.CitySearchDialog
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

private val CityCardShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorldClockScreen(
    cities: List<WorldClockCity>,
    onAddCity: (WorldClockCity) -> Unit,
    onDeleteCity: (WorldClockCity) -> Unit,
    externalShowAddDialog: Boolean = false,
    onExternalShowAddDialogHandled: () -> Unit = {}
) {
    var showCitySearch by remember { mutableStateOf(false) }

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

    val context = LocalContext.current
    val headerZoneStr = remember {
        val tz = java.util.TimeZone.getDefault()
        val name = tz.getDisplayName(false, java.util.TimeZone.LONG, Locale.getDefault())
        val id = tz.id.substringAfterLast("/").replace("_", " ")
        if (name.contains(id, ignoreCase = true)) {
            name
        } else {
            // Mapping for common Russian cities if needed, otherwise fallback
            when (tz.id) {
                "Asia/Yekaterinburg" -> context.getString(org.openui.clock.R.string.tz_yekaterinburg)
                "Europe/Moscow" -> context.getString(org.openui.clock.R.string.tz_moscow)
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
                        .padding(top = 40.dp, bottom = 24.dp),
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = headerZoneStr,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

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
                            contentDescription = stringResource(org.openui.clock.R.string.add),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                            contentDescription = stringResource(org.openui.clock.R.string.sort),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(org.openui.clock.R.string.options),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            items(cities, key = { it.id }) { city ->
                CityClockCard(
                    city = city,
                    onDelete = { onDeleteCity(city) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (showCitySearch) {
        CitySearchDialog(
            onDismiss = { showCitySearch = false },
            onCitySelected = { city ->
                onAddCity(city)
                showCitySearch = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CityClockCard(
    city: WorldClockCity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeDiff = getCityTimeDifference(city.timeZoneId)
    val currentTime = city.getCurrentTimeFormatted()

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1F28).copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Do nothing for now */ },
                onLongClick = onDelete
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(
                    text = timeDiff,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Text(
                text = currentTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 40.sp,
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
            return context.getString(org.openui.clock.R.string.local_timezone)
        }

        val nowLocal = ZonedDateTime.now(localZone)
        val nowTarget = ZonedDateTime.now(targetZone)

        val localOffset = localZone.rules.getOffset(nowLocal.toInstant()).totalSeconds
        val targetOffset = targetZone.rules.getOffset(nowTarget.toInstant()).totalSeconds

        val diffSeconds = targetOffset - localOffset
        val diffHours = Math.abs(diffSeconds / 3600)
        val diffMinutes = Math.abs((diffSeconds % 3600) / 60)

        val hStr = context.getString(org.openui.clock.R.string.hours_short)
        val mStr = context.getString(org.openui.clock.R.string.minutes_short)

        val timeString = if (diffMinutes == 0) {
            "$diffHours $hStr"
        } else {
            "$diffHours $hStr $diffMinutes $mStr"
        }

        val diffString = if (diffSeconds < 0) {
            context.getString(org.openui.clock.R.string.earlier, timeString)
        } else if (diffSeconds > 0) {
            context.getString(org.openui.clock.R.string.later, timeString)
        } else {
            context.getString(org.openui.clock.R.string.same_time)
        }

        val localDate = nowLocal.toLocalDate()
        val targetDate = nowTarget.toLocalDate()

        val dayString = when {
            targetDate.isBefore(localDate) -> context.getString(org.openui.clock.R.string.yesterday)
            targetDate.isAfter(localDate) -> context.getString(org.openui.clock.R.string.tomorrow)
            else -> ""
        }

        "$diffString$dayString"
    } catch (e: Exception) {
        ""
    }
}

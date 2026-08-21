package org.openui.clock.ui.components

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.openui.clock.data.WorldClockCity

data class AvailableCity(val cityName: String, val country: String, val timeZoneId: String)

private val defaultCities = listOf(
    AvailableCity("Москва", "Россия", "Europe/Moscow"),
    AvailableCity("Санкт-Петербург", "Россия", "Europe/Moscow"),
    AvailableCity("Новосибирск", "Россия", "Asia/Novosibirsk"),
    AvailableCity("Екатеринбург", "Россия", "Asia/Yekaterinburg"),
    AvailableCity("Казань", "Россия", "Europe/Moscow"),
    AvailableCity("Владивосток", "Россия", "Asia/Vladivostok"),
    AvailableCity("Минск", "Беларусь", "Europe/Minsk"),
    AvailableCity("Алматы", "Казахстан", "Asia/Almaty"),
    AvailableCity("Астана", "Казахстан", "Asia/Aqtobe"),
    AvailableCity("Ташкент", "Узбекистан", "Asia/Tashkent"),
    AvailableCity("Лондон", "Великобритания", "Europe/London"),
    AvailableCity("Париж", "Франция", "Europe/Paris"),
    AvailableCity("Берлин", "Германия", "Europe/Berlin"),
    AvailableCity("Нью-Йорк", "США", "America/New_York"),
    AvailableCity("Токио", "Япония", "Asia/Tokyo"),
    AvailableCity("Пекин", "Китай", "Asia/Shanghai"),
    AvailableCity("Сидней", "Австралия", "Australia/Sydney")
)

@Composable
fun CitySearchDialog(
    onDismiss: () -> Unit,
    onCitySelected: (WorldClockCity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCities by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) defaultCities
            else defaultCities.filter {
                it.cityName.contains(searchQuery, ignoreCase = true) ||
                it.country.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(org.openui.clock.R.string.search_city_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(org.openui.clock.R.string.close))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(org.openui.clock.R.string.search_city_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredCities, key = { "${it.cityName}_${it.timeZoneId}" }) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCitySelected(
                                        WorldClockCity(
                                            cityName = city.cityName,
                                            country = city.country,
                                            timeZoneId = city.timeZoneId
                                        )
                                    )
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.cityName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${city.country} (${city.timeZoneId})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

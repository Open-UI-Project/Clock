package org.openui.clock.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Immutable
@Entity(tableName = "world_clock_cities")
data class WorldClockCity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cityName: String,
    val timeZoneId: String,
    val country: String = ""
) {
    fun getCurrentTimeFormatted(): String {
        return try {
            val zone = ZoneId.of(timeZoneId)
            val zdt = ZonedDateTime.now(zone)
            zdt.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            "--:--"
        }
    }

    fun getTimeDifferenceFormatted(): String {
        return try {
            val nowLocal = ZonedDateTime.now()
            val nowCity = ZonedDateTime.now(ZoneId.of(timeZoneId))
            val diffHours = (nowCity.offset.totalSeconds - nowLocal.offset.totalSeconds) / 3600
            
            when {
                diffHours == 0 -> "То же время"
                diffHours > 0 -> "+$diffHours ч"
                else -> "$diffHours ч"
            }
        } catch (e: Exception) {
            ""
        }
    }
}

package org.openui.clock.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openui.clock.R

@Immutable
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val daysOfWeek: String = "", // Comma-separated days, e.g. "1,2,3,4,5" (1=Mon..7=Sun), empty = once
    val vibrate: Boolean = true,
    val vibratePattern: String = "default",
    val soundName: String = "default",
    val soundUri: String = "",
    val soundEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val snoozeTimes: Int = 3
) {
    fun getFormattedTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun getRepeatDaysText(context: Context): String {
        if (daysOfWeek.isBlank()) return context.getString(R.string.repeat_once)
        val daysList = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (daysList.size == 7) return context.getString(R.string.repeat_everyday)
        if (daysList.containsAll(listOf(1, 2, 3, 4, 5)) && daysList.size == 5) return context.getString(R.string.repeat_weekdays)
        if (daysList.containsAll(listOf(6, 7)) && daysList.size == 2) return context.getString(R.string.repeat_weekends)

        val dayNames = mapOf(
            1 to context.getString(R.string.day_mon),
            2 to context.getString(R.string.day_tue),
            3 to context.getString(R.string.day_wed),
            4 to context.getString(R.string.day_thu),
            5 to context.getString(R.string.day_fri),
            6 to context.getString(R.string.day_sat),
            7 to context.getString(R.string.day_sun)
        )
        return daysList.mapNotNull { dayNames[it] }.joinToString(", ")
    }
}

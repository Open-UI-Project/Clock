package org.openui.clock.util

import android.content.Context
import org.openui.clock.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object AlarmTimeUtils {

    fun parseDaysOfWeek(daysOfWeek: String?): Set<Int> {
        if (daysOfWeek.isNullOrBlank()) return emptySet()
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    fun getNextAlarmDateTime(hour: Int, minute: Int, daysOfWeek: Set<Int>): LocalDateTime {
        val now = LocalDateTime.now()
        val targetToday = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (daysOfWeek.isEmpty()) {
            // One-time alarm: if today's time hasn't passed, ring today; otherwise tomorrow
            return if (targetToday.isAfter(now)) {
                targetToday
            } else {
                targetToday.plusDays(1)
            }
        }

        // Repeating alarm on specific days of week (1=Mon..7=Sun)
        for (dayOffset in 0L..7L) {
            val candidate = targetToday.plusDays(dayOffset)
            val candidateDayOfWeek = candidate.dayOfWeek.value // 1=Mon..7=Sun
            if (daysOfWeek.contains(candidateDayOfWeek)) {
                if (candidate.isAfter(now)) {
                    return candidate
                }
            }
        }

        // Fallback in case of exact boundary condition
        return targetToday.plusDays(7)
    }

    fun getNextAlarmEpochMillis(hour: Int, minute: Int, daysOfWeek: Set<Int>): Long {
        val nextDateTime = getNextAlarmDateTime(hour, minute, daysOfWeek)
        return nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getAlarmDatePreviewText(hour: Int, minute: Int, daysOfWeek: Set<Int>): String {
        val nextDateTime = getNextAlarmDateTime(hour, minute, daysOfWeek)
        val today = LocalDate.now()
        val targetDate = nextDateTime.toLocalDate()

        val dayOfWeekRu = when (targetDate.dayOfWeek.value) {
            1 -> "пн"
            2 -> "вт"
            3 -> "ср"
            4 -> "чт"
            5 -> "пт"
            6 -> "сб"
            7 -> "вс"
            else -> ""
        }

        val dayPrefix = when {
            targetDate == today -> "Сегодня"
            targetDate == today.plusDays(1) -> "Завтра"
            else -> dayOfWeekRu.replaceFirstChar { it.uppercase() }
        }

        val formattedDay = targetDate.format(DateTimeFormatter.ofPattern("d MMM", Locale("ru")))

        return if (targetDate == today || targetDate == today.plusDays(1)) {
            "$dayPrefix-$dayOfWeekRu, $formattedDay."
        } else {
            "$dayPrefix, $formattedDay."
        }
    }

    fun getTimeRemainingText(
        context: Context,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>
    ): String {
        val now = LocalDateTime.now()
        val nextTrigger = getNextAlarmDateTime(hour, minute, daysOfWeek)
        val totalMinutes = ChronoUnit.MINUTES.between(now, nextTrigger).coerceAtLeast(0)
        val totalDays = totalMinutes / (24 * 60)
        val remainingHours = (totalMinutes % (24 * 60)) / 60
        val remainingMinutes = totalMinutes % 60

        return if (totalDays > 0) {
            context.getString(R.string.time_remaining_format_days_hours_mins, totalDays, remainingHours, remainingMinutes)
        } else if (remainingHours > 0) {
            context.getString(R.string.time_remaining_format_hours_mins, remainingHours, remainingMinutes)
        } else if (remainingMinutes > 0) {
            context.getString(R.string.time_remaining_format_mins, remainingMinutes)
        } else {
            context.getString(R.string.time_remaining_format_less_than_minute)
        }
    }

    fun getAlarmRingsInMessage(
        context: Context,
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>
    ): String {
        val remaining = getTimeRemainingText(context, hour, minute, daysOfWeek)
        return context.getString(R.string.alarm_rings_in, remaining)
    }
}

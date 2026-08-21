package org.openui.clock.data

import android.content.Context
import org.openui.clock.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class ClockRepository(context: Context) {

    private val db = ClockDatabase.getDatabase(context)
    private val dao = db.clockDao()
    private val scheduler = AlarmScheduler(context)

    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()
    val allCities: Flow<List<WorldClockCity>> = dao.getAllCities()

    suspend fun addAlarm(alarm: Alarm) {
        val id = dao.insertAlarm(alarm)
        val createdAlarm = alarm.copy(id = id.toInt())
        if (createdAlarm.isEnabled) {
            scheduler.schedule(createdAlarm)
        }
    }

    suspend fun updateAlarm(alarm: Alarm) {
        dao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            scheduler.schedule(alarm)
        } else {
            scheduler.cancel(alarm)
        }
    }

    suspend fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        val updated = alarm.copy(isEnabled = enabled)
        dao.updateAlarm(updated)
        if (enabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated)
        }
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        scheduler.cancel(alarm)
        dao.deleteAlarm(alarm)
    }

    suspend fun addCity(city: WorldClockCity) {
        dao.insertCity(city)
    }

    suspend fun deleteCity(city: WorldClockCity) {
        dao.deleteCity(city)
    }
}

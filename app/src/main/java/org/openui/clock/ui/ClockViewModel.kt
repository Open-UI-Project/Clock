package org.openui.clock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.openui.clock.data.Alarm
import org.openui.clock.data.ClockRepository
import org.openui.clock.data.WorldClockCity
import org.openui.clock.notification.ClockNotificationManager
import org.openui.clock.notification.NotificationReceiver

@androidx.compose.runtime.Stable
data class StopwatchState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList()
)

@androidx.compose.runtime.Stable
data class TimerState(
    val totalMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClockRepository(application)

    init {
        ClockNotificationManager.createNotificationChannels(application)
        NotificationReceiver.onActionReceived = { action ->
            when (action) {
                NotificationReceiver.ACTION_STOPWATCH_TOGGLE -> {
                    if (_stopwatchState.value.isRunning) pauseStopwatch() else startStopwatch()
                }
                NotificationReceiver.ACTION_STOPWATCH_LAP -> lapStopwatch()
                NotificationReceiver.ACTION_STOPWATCH_RESET -> resetStopwatch()
                NotificationReceiver.ACTION_TIMER_TOGGLE -> {
                    if (_timerState.value.isRunning) pauseTimer() else resumeTimer()
                }
                NotificationReceiver.ACTION_TIMER_RESET -> resetTimer()
            }
        }
    }

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cities: StateFlow<List<WorldClockCity>> = repository.allCities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Stopwatch State
    private val _stopwatchState = MutableStateFlow(StopwatchState())
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()
    private var stopwatchJob: Job? = null

    // Timer State
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    // Alarm actions
    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.addAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, enabled)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    // World Clock actions
    fun addCity(city: WorldClockCity) {
        viewModelScope.launch {
            repository.addCity(city)
        }
    }

    fun deleteCity(city: WorldClockCity) {
        viewModelScope.launch {
            repository.deleteCity(city)
        }
    }

    // Stopwatch logic
    fun startStopwatch() {
        if (_stopwatchState.value.isRunning) return
        _stopwatchState.value = _stopwatchState.value.copy(isRunning = true)
        stopwatchJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _stopwatchState.value.elapsedMillis
            var lastNotifTime = 0L
            while (_stopwatchState.value.isRunning) {
                val elapsed = System.currentTimeMillis() - startTime
                _stopwatchState.value = _stopwatchState.value.copy(elapsedMillis = elapsed)
                if (elapsed - lastNotifTime >= 500L) {
                    lastNotifTime = elapsed
                    ClockNotificationManager.updateStopwatchNotification(getApplication(), elapsed, true)
                }
                delay(10)
            }
        }
    }

    fun pauseStopwatch() {
        _stopwatchState.value = _stopwatchState.value.copy(isRunning = false)
        stopwatchJob?.cancel()
        ClockNotificationManager.updateStopwatchNotification(
            getApplication(),
            _stopwatchState.value.elapsedMillis,
            false
        )
    }

    fun resetStopwatch() {
        pauseStopwatch()
        _stopwatchState.value = StopwatchState()
        ClockNotificationManager.cancelStopwatchNotification(getApplication())
    }

    fun lapStopwatch() {
        val current = _stopwatchState.value
        if (current.elapsedMillis > 0) {
            _stopwatchState.value = current.copy(
                laps = listOf(current.elapsedMillis) + current.laps
            )
            ClockNotificationManager.updateStopwatchNotification(
                getApplication(),
                current.elapsedMillis,
                current.isRunning
            )
        }
    }

    // Timer logic
    fun startTimer(durationMillis: Long) {
        pauseTimer()
        _timerState.value = TimerState(
            totalMillis = durationMillis,
            remainingMillis = durationMillis,
            isRunning = true,
            isFinished = false
        )
        org.openui.clock.alarm.AlarmScheduler(getApplication()).scheduleTimer(durationMillis)
        runTimerLoop()
    }

    fun resumeTimer() {
        if (_timerState.value.remainingMillis <= 0) return
        _timerState.value = _timerState.value.copy(isRunning = true)
        org.openui.clock.alarm.AlarmScheduler(getApplication()).scheduleTimer(_timerState.value.remainingMillis)
        runTimerLoop()
    }

    private fun runTimerLoop() {
        timerJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            var lastNotifTime = 0L
            while (_timerState.value.isRunning && _timerState.value.remainingMillis > 0) {
                delay(50)
                val now = System.currentTimeMillis()
                val delta = now - lastTime
                lastTime = now

                val newRemaining = (_timerState.value.remainingMillis - delta).coerceAtLeast(0)
                if (newRemaining == 0L) {
                    _timerState.value = _timerState.value.copy(
                        remainingMillis = 0L,
                        isRunning = false,
                        isFinished = true
                    )
                    ClockNotificationManager.cancelTimerNotification(getApplication())
                    
                    // Trigger ringing service
                    val intent = android.content.Intent(getApplication(), org.openui.clock.alarm.AlarmReceiver::class.java).apply {
                        action = org.openui.clock.alarm.AlarmReceiver.ACTION_TIMER_FINISHED
                    }
                    getApplication<Application>().sendBroadcast(intent)
                } else {
                    _timerState.value = _timerState.value.copy(remainingMillis = newRemaining)
                    if (now - lastNotifTime >= 1000L) {
                        lastNotifTime = now
                        ClockNotificationManager.updateTimerNotification(getApplication(), newRemaining, true)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _timerState.value = _timerState.value.copy(isRunning = false)
        timerJob?.cancel()
        org.openui.clock.alarm.AlarmScheduler(getApplication()).cancelTimer()
        ClockNotificationManager.updateTimerNotification(
            getApplication(),
            _timerState.value.remainingMillis,
            false
        )
    }

    fun resetTimer() {
        pauseTimer()
        _timerState.value = TimerState()
        org.openui.clock.alarm.AlarmScheduler(getApplication()).cancelTimer()
        ClockNotificationManager.cancelTimerNotification(getApplication())
    }
}

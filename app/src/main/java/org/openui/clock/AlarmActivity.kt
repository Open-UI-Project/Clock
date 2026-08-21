package org.openui.clock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.openui.clock.alarm.AlarmReceiver
import org.openui.clock.alarm.AlarmScheduler
import org.openui.clock.alarm.AlarmService
import org.openui.clock.ui.theme.ClockAppTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    private var alarmId = -1
    private var alarmLabel = "Будильник"
    private var alarmSound = ""
    private var alarmVibrate = true

    private var closeReceiver: android.content.BroadcastReceiver? = null

    private var isTimer = false
    private var screenOffReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        closeReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "org.openui.clock.CLOSE_ALARM_ACTIVITY") {
                    finish()
                }
            }
        }
        val filter = android.content.IntentFilter("org.openui.clock.CLOSE_ALARM_ACTIVITY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        screenOffReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    if (isTimer) {
                        dismissAlarm()
                    } else {
                        snoozeAlarm(5)
                    }
                }
            }
        }
        val screenOffFilter = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, screenOffFilter)

        alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Будильник"
        alarmSound = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_SOUND) ?: ""
        alarmVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, true)
        isTimer = intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_TIMER, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        setContent {
            ClockAppTheme {
                AlarmScreenContent(
                    label = alarmLabel,
                    isTimer = isTimer,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { minutes -> snoozeAlarm(minutes) },
                    onRestartTimer = { restartTimer() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        alarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: alarmLabel
        alarmSound = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_SOUND) ?: alarmSound
        alarmVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, alarmVibrate)
        isTimer = intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_TIMER, isTimer)
    }

    override fun onDestroy() {
        super.onDestroy()
        closeReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        screenOffReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || 
            keyCode == KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == KeyEvent.KEYCODE_POWER) {
            if (isTimer) {
                dismissAlarm()
            } else {
                snoozeAlarm(5)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dismissAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ACTION_TYPE, AlarmReceiver.ACTION_STOP_RINGING)
        }
        startService(stopIntent)
        finish()
    }

    private fun restartTimer() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ACTION_TYPE, AlarmReceiver.ACTION_STOP_RINGING)
        }
        startService(stopIntent)

        val resetIntent = Intent(this, org.openui.clock.notification.NotificationReceiver::class.java).apply {
            action = org.openui.clock.notification.NotificationReceiver.ACTION_TIMER_RESET
        }
        sendBroadcast(resetIntent)
        finish()
    }

    private fun snoozeAlarm(minutes: Int = 5) {
        val scheduler = AlarmScheduler(this)
        scheduler.scheduleSnooze(alarmId, alarmLabel, alarmSound, alarmVibrate, minutes)

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ACTION_TYPE, AlarmReceiver.ACTION_STOP_RINGING)
        }
        startService(stopIntent)

        Toast.makeText(this, "Отложено на $minutes мин.", Toast.LENGTH_SHORT).show()
        finish()
    }
}

@Composable
private fun AlarmScreenContent(
    label: String,
    isTimer: Boolean,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit,
    onRestartTimer: () -> Unit
) {
    var snoozeMinutes by remember { mutableIntStateOf(5) }
    var currentTimeText by remember { mutableStateOf("") }
    var currentDateText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMMM d", Locale("ru"))
        while (true) {
            val now = LocalDateTime.now()
            currentTimeText = now.format(timeFormatter)
            currentDateText = now.format(dateFormatter)
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF130A2A),
                        Color(0xFF2F1839),
                        Color(0xFF5A3C38)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = if (isTimer) "00:00" else currentTimeText.ifBlank { "00:00" },
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentDateText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isTimer) "Таймер завершен" else label.ifBlank { "Будильник" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Circular Dismiss / Stop Button
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Остановить",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (!isTimer) {
                // Bottom Snooze Pill
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (snoozeMinutes > 1) snoozeMinutes -= 1
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Уменьшить время",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = "отложить на $snoozeMinutes минут",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .clickable { onSnooze(snoozeMinutes) }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )

                        IconButton(
                            onClick = {
                                if (snoozeMinutes < 60) snoozeMinutes += 1
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Увеличить время",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.clickable { onRestartTimer() }
                ) {
                    Text(
                        text = "Сброс таймера",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

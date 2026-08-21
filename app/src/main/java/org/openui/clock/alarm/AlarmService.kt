package org.openui.clock.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openui.clock.AlarmActivity
import org.openui.clock.MainActivity
import org.openui.clock.R
import org.openui.clock.data.ClockDatabase
import org.openui.clock.notification.ClockNotificationManager

class AlarmService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var currentAlarmId: Int = -1
    private var currentAlarmLabel: String = ""
    private var currentAlarmSound: String = ""
    private var currentAlarmVibrate: Boolean = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actionType = intent?.getStringExtra(AlarmReceiver.EXTRA_ACTION_TYPE) ?: return START_NOT_STICKY

        when (actionType) {
            AlarmReceiver.ACTION_RESTART_ALARMS -> {
                val notification = buildForegroundNotification(getString(R.string.timer_syncing))
                startForeground(NOTIFICATION_ID, notification)
                
                serviceScope.launch {
                    val db = ClockDatabase.getDatabase(applicationContext)
                    val scheduler = AlarmScheduler(applicationContext)
                    val alarms = db.clockDao().getAllAlarms().first()
                    alarms.filter { it.isEnabled }.forEach { scheduler.schedule(it) }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            AlarmReceiver.ACTION_START_RINGING -> {
                currentAlarmId = intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
                currentAlarmLabel = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: getString(R.string.notification_alarm_title)
                currentAlarmSound = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_SOUND) ?: ""
                currentAlarmVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, true)

                // Handle repeating vs non-repeating alarms
                if (currentAlarmId != -1) {
                    serviceScope.launch {
                        try {
                            val db = ClockDatabase.getDatabase(applicationContext)
                            val alarm = db.clockDao().getAlarmById(currentAlarmId)
                            if (alarm != null) {
                                if (alarm.daysOfWeek.isBlank()) {
                                    db.clockDao().updateAlarm(alarm.copy(isEnabled = false))
                                } else {
                                    // For repeating alarm, schedule the next occurrence
                                    AlarmScheduler(applicationContext).schedule(alarm)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val notification = buildRingingNotification(currentAlarmLabel, currentAlarmId)
                startForeground(NOTIFICATION_ID, notification)

                startRingingAndVibration(currentAlarmSound, currentAlarmVibrate)

                try {
                    val activityIntent = Intent(applicationContext, AlarmActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentAlarmId)
                        putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, currentAlarmLabel)
                        putExtra(AlarmReceiver.EXTRA_ALARM_SOUND, currentAlarmSound)
                        putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, currentAlarmVibrate)
                    }
                    startActivity(activityIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            AlarmReceiver.ACTION_TIMER_RINGING -> {
                val notification = buildRingingNotification(getString(R.string.timer_finished_title), -1, isTimer = true)
                startForeground(NOTIFICATION_ID, notification)

                startRingingAndVibration("", true)

                try {
                    val activityIntent = Intent(applicationContext, AlarmActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, getString(R.string.timer_finished_title))
                        putExtra(AlarmReceiver.EXTRA_IS_TIMER, true)
                    }
                    startActivity(activityIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            AlarmReceiver.ACTION_SNOOZE_SERVICE -> {
                stopRingingAndVibration()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            AlarmReceiver.ACTION_STOP_RINGING -> {
                stopRingingAndVibration()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startRingingAndVibration(soundUriStr: String, vibrate: Boolean) {
        stopRingingAndVibration()

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "AlarmApp:AlarmWakeLock"
            )
            wakeLock?.acquire(10 * 60 * 1000L) // Держать экран включенным
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var started = false

        if (soundUriStr.isNotBlank() && soundUriStr != getString(org.openui.clock.R.string.sound_default)) {
            try {
                val uri = Uri.parse(soundUriStr)
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}

                val mp = MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = mp
                started = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!started) {
            try {
                val defaultUri = RingtoneManager.getActualDefaultRingtoneUri(applicationContext, RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                if (defaultUri != null) {
                    val mp = MediaPlayer().apply {
                        setDataSource(applicationContext, defaultUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = mp
                    started = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (vibrate) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 500, 500)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRingingAndVibration() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildForegroundNotification(title: String) = NotificationCompat.Builder(this, ClockNotificationManager.CHANNEL_ALARM)
        .setContentTitle(title)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun buildRingingNotification(label: String, alarmId: Int = -1, isTimer: Boolean = false): android.app.Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_SOUND, currentAlarmSound)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, currentAlarmVibrate)
            putExtra(AlarmReceiver.EXTRA_IS_TIMER, isTimer)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, currentAlarmLabel)
            putExtra(AlarmReceiver.EXTRA_ALARM_SOUND, currentAlarmSound)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIBRATE, currentAlarmVibrate)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTimer) getString(org.openui.clock.R.string.timer_finished_title) else getString(org.openui.clock.R.string.notification_alarm_title)

        val builder = NotificationCompat.Builder(this, ClockNotificationManager.CHANNEL_ALARM)
            .setContentTitle(title)
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отключить", dismissPendingIntent)

        if (!isTimer) {
            builder.addAction(android.R.drawable.ic_menu_recent_history, "Отложить (5 мин)", snoozePendingIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        ClockNotificationManager.createNotificationChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRingingAndVibration()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "alarm_service_channel"
        const val NOTIFICATION_ID = 1001
    }
}

package org.openui.clock.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.openui.clock.MainActivity
import org.openui.clock.R

object ClockNotificationManager {

    const val CHANNEL_ALARM = "alarm_channel"
    const val CHANNEL_TIMER = "timer_channel"
    const val CHANNEL_WORLD_CLOCK = "world_clock_channel"
    const val CHANNEL_STOPWATCH = "stopwatch_channel"

    const val NOTIF_ID_STOPWATCH = 2001
    const val NOTIF_ID_TIMER = 2002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Alarm Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                enableVibration(true)
                enableLights(true)
            }

            // 2. Timer Channel
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                context.getString(R.string.channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_timer_desc)
            }

            // 3. World Clock Channel
            val worldClockChannel = NotificationChannel(
                CHANNEL_WORLD_CLOCK,
                context.getString(R.string.channel_world_clock_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_world_clock_desc)
            }

            // 4. Stopwatch Channel
            val stopwatchChannel = NotificationChannel(
                CHANNEL_STOPWATCH,
                context.getString(R.string.channel_stopwatch_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_stopwatch_desc)
            }

            manager.createNotificationChannels(
                listOf(alarmChannel, timerChannel, worldClockChannel, stopwatchChannel)
            )
        }
    }

    fun updateStopwatchNotification(context: Context, elapsedMillis: Long, isRunning: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val toggleActionText = context.getString(
            if (isRunning) R.string.notification_action_pause else R.string.notification_action_resume
        )
        val toggleIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_STOPWATCH_TOGGLE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lapIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_STOPWATCH_LAP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_STOPWATCH_RESET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val min = (elapsedMillis / 60000) % 60
        val sec = (elapsedMillis / 1000) % 60
        val ms = (elapsedMillis % 1000) / 10
        val formattedTime = String.format("%02d:%02d.%02d", min, sec, ms)

        val notification = NotificationCompat.Builder(context, CHANNEL_STOPWATCH)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(context.getString(R.string.notification_stopwatch_title))
            .setContentText(formattedTime)
            .setContentIntent(contentIntent)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .addAction(0, toggleActionText, toggleIntent)
            .addAction(0, context.getString(R.string.notification_action_lap), lapIntent)
            .addAction(0, context.getString(R.string.notification_action_reset), resetIntent)
            .build()

        manager.notify(NOTIF_ID_STOPWATCH, notification)
    }

    fun cancelStopwatchNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_STOPWATCH)
    }

    fun updateTimerNotification(context: Context, remainingMillis: Long, isRunning: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionText = context.getString(
            if (isRunning) R.string.notification_action_pause else R.string.notification_action_resume
        )
        val toggleIntent = PendingIntent.getBroadcast(
            context,
            4,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_TIMER_TOGGLE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getBroadcast(
            context,
            5,
            Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_TIMER_RESET
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hrs = (remainingMillis / 3600000) % 24
        val min = (remainingMillis / 60000) % 60
        val sec = (remainingMillis / 1000) % 60
        val formattedTime = if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, min, sec)
        } else {
            String.format("%02d:%02d", min, sec)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_timer_title))
            .setContentText(formattedTime)
            .setContentIntent(contentIntent)
            .setOngoing(isRunning)
            .setOnlyAlertOnce(true)
            .addAction(0, toggleActionText, toggleIntent)
            .addAction(0, context.getString(R.string.notification_action_reset), resetIntent)
            .build()

        manager.notify(NOTIF_ID_TIMER, notification)
    }

    fun cancelTimerNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIF_ID_TIMER)
    }
}

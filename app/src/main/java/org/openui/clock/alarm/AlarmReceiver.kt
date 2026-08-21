package org.openui.clock.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_RESTART_ALARMS)
                }
                startServiceCompat(context, serviceIntent)
            }
            ACTION_ALARM_TRIGGER -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Будильник"
                val soundUri = intent.getStringExtra(EXTRA_ALARM_SOUND) ?: ""
                val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_START_RINGING)
                    putExtra(EXTRA_ALARM_ID, alarmId)
                    putExtra(EXTRA_ALARM_LABEL, label)
                    putExtra(EXTRA_ALARM_SOUND, soundUri)
                    putExtra(EXTRA_ALARM_VIBRATE, vibrate)
                }
                startServiceCompat(context, serviceIntent)
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Будильник"
                val soundUri = intent.getStringExtra(EXTRA_ALARM_SOUND) ?: ""
                val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)

                val scheduler = AlarmScheduler(context)
                scheduler.scheduleSnooze(alarmId, label, soundUri, vibrate, 5)

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_SNOOZE_SERVICE)
                    putExtra(EXTRA_ALARM_ID, alarmId)
                }
                startServiceCompat(context, serviceIntent)

                val closeIntent = Intent("org.openui.clock.CLOSE_ALARM_ACTIVITY")
                context.sendBroadcast(closeIntent)
            }
            ACTION_DISMISS -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_STOP_RINGING)
                }
                startServiceCompat(context, serviceIntent)

                val closeIntent = Intent("org.openui.clock.CLOSE_ALARM_ACTIVITY")
                context.sendBroadcast(closeIntent)
            }
            ACTION_TIMER_FINISHED -> {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_TIMER_RINGING)
                }
                startServiceCompat(context, serviceIntent)
            }
        }
    }

    private fun startServiceCompat(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "org.openui.clock.ALARM_TRIGGER"
        const val ACTION_SNOOZE = "org.openui.clock.SNOOZE_ALARM"
        const val ACTION_DISMISS = "org.openui.clock.DISMISS_ALARM"
        const val ACTION_TIMER_FINISHED = "org.openui.clock.TIMER_FINISHED"

        const val ACTION_RESTART_ALARMS = "RESTART_ALARMS"
        const val ACTION_START_RINGING = "START_RINGING"
        const val ACTION_SNOOZE_SERVICE = "SNOOZE_SERVICE"
        const val ACTION_STOP_RINGING = "STOP_RINGING"
        const val ACTION_TIMER_RINGING = "TIMER_RINGING"

        const val EXTRA_ACTION_TYPE = "ACTION_TYPE"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val EXTRA_ALARM_LABEL = "ALARM_LABEL"
        const val EXTRA_ALARM_SOUND = "ALARM_SOUND"
        const val EXTRA_ALARM_VIBRATE = "ALARM_VIBRATE"
        const val EXTRA_IS_TIMER = "IS_TIMER"
    }
}

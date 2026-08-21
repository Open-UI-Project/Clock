package org.openui.clock.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        onActionReceived?.invoke(action)
    }

    companion object {
        const val ACTION_STOPWATCH_TOGGLE = "org.openui.clock.ACTION_STOPWATCH_TOGGLE"
        const val ACTION_STOPWATCH_LAP = "org.openui.clock.ACTION_STOPWATCH_LAP"
        const val ACTION_STOPWATCH_RESET = "org.openui.clock.ACTION_STOPWATCH_RESET"

        const val ACTION_TIMER_TOGGLE = "org.openui.clock.ACTION_TIMER_TOGGLE"
        const val ACTION_TIMER_RESET = "org.openui.clock.ACTION_TIMER_RESET"

        var onActionReceived: ((String) -> Unit)? = null
    }
}

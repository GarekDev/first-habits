package app.firstyou.habits.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.firstyou.habits.HabitsApp
import app.firstyou.habits.widget.refreshTodayWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as HabitsApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == ReminderScheduler.ACTION_NEW_DAY) {
                    app.container.reminders.onNewDay()
                    return@launch
                }
                val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
                val day = intent.getStringExtra(ReminderScheduler.EXTRA_DAY)
                if (habitId >= 0) {
                    app.container.reminders.onAlarm(habitId, day)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val pending = goAsync()
        val app = context.applicationContext as HabitsApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.reminders.rescheduleAll()
                refreshTodayWidgets(app)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}

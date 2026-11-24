package app.firstyou.habits.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.firstyou.habits.MainActivity
import app.firstyou.habits.data.Habit
import app.firstyou.habits.data.HabitRepository
import app.firstyou.habits.data.ReminderTime
import app.firstyou.habits.data.WeekDays
import app.firstyou.habits.widget.refreshTodayWidgets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderScheduler(
    private val context: Context,
    private val repository: HabitRepository,
) {
    suspend fun rescheduleAll() {
        withContext(Dispatchers.IO) {
            repository.allHabits().forEach { habit ->
                val minutes = habit.reminderMinutes
                if (habit.archived || minutes == null) {
                    cancel(habit.id)
                } else {
                    val whenDate = ReminderTime.next(
                        daysMask = habit.days,
                        minutesFromMidnight = minutes,
                        completed = repository.completedDays(habit.id),
                        now = LocalDateTime.now(),
                    )
                    arm(habit.id, whenDate)
                }
            }
            armNewDay()
        }
    }

    suspend fun onAlarm(habitId: Long, day: String?) {
        withContext(Dispatchers.IO) {
            val habit = repository.get(habitId)
            val today = LocalDate.now()
            val firedFor = day?.let(LocalDate::parse)
            val shouldNotify = habit != null &&
                !habit.archived &&
                habit.reminderMinutes != null &&
                firedFor == today &&
                WeekDays.isScheduled(habit.days, today) &&
                today !in repository.completedDays(habitId)
            if (shouldNotify) {
                notify(habit)
            }
            rescheduleAll()
        }
    }

    suspend fun onNewDay() {
        rescheduleAll()
        refreshTodayWidgets(context)
    }

    private fun armNewDay() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val trigger = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(1)
            .toInstant()
            .toEpochMilli()
        val pending = PendingIntent.getBroadcast(
            context,
            DAY_REQUEST,
            Intent(context, ReminderReceiver::class.java).apply { action = ACTION_NEW_DAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }

    private fun arm(habitId: Long, whenDate: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pending(habitId, whenDate.toLocalDate())
        val millis = whenDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    private fun cancel(habitId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pending(habitId, LocalDate.now()))
    }

    private fun pending(habitId: Long, day: LocalDate): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, habitId)
            putExtra(EXTRA_DAY, day.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(habit: Habit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            habit.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(habit.name)
            .setContentText("Toca para abrir First Habits")
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        manager.notify(habit.id.toInt(), notification)
    }

    companion object {
        const val EXTRA_ID = "habitId"
        const val EXTRA_DAY = "day"
        const val CHANNEL_ID = "habit_reminders"
        const val ACTION_NEW_DAY = "app.firstyou.habits.NEW_DAY"
        private const val DAY_REQUEST = 0
    }
}

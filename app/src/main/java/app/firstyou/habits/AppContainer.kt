package app.firstyou.habits

import android.content.Context
import app.firstyou.habits.data.AppSettings
import app.firstyou.habits.data.HabitRepository
import app.firstyou.habits.data.local.AppDatabase
import app.firstyou.habits.reminder.ReminderScheduler
import app.firstyou.habits.widget.refreshTodayWidgets

class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    val repository = HabitRepository(database, database.habitDao(), database.completionDao())
    val reminders = ReminderScheduler(context.applicationContext, repository)
    val settings = AppSettings(context)

    init {
        repository.afterChange = {
            reminders.rescheduleAll()
            refreshTodayWidgets(context.applicationContext)
        }
    }
}

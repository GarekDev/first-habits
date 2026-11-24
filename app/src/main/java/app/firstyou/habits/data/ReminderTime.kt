package app.firstyou.habits.data

import java.time.LocalDate
import java.time.LocalDateTime

object ReminderTime {
    fun format(minutes: Int): String {
        return "%02d:%02d".format(minutes / 60, minutes % 60)
    }

    fun next(
        daysMask: Int,
        minutesFromMidnight: Int,
        completed: Set<LocalDate>,
        now: LocalDateTime,
    ): LocalDateTime {
        val hour = minutesFromMidnight / 60
        val minute = minutesFromMidnight % 60
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (!WeekDays.isScheduled(daysMask, date)) continue
            if (date in completed) continue
            val trigger = date.atTime(hour, minute)
            if (trigger.isAfter(now)) return trigger
        }
        return now.toLocalDate().plusDays(7).atTime(hour, minute)
    }
}

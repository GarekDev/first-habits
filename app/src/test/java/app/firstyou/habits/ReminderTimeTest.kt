package app.firstyou.habits

import app.firstyou.habits.data.ReminderTime
import app.firstyou.habits.data.WeekDays
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeTest {
    @Test
    fun laterToday_staysToday() {
        val now = LocalDateTime.of(2026, 11, 23, 10, 0)
        val next = ReminderTime.next(WeekDays.ALL, 10 * 60 + 2, emptySet(), now)
        assertEquals(LocalDateTime.of(2026, 11, 23, 10, 2), next)
    }

    @Test
    fun alreadyDoneToday_movesToTomorrow() {
        val now = LocalDateTime.of(2026, 11, 23, 10, 0)
        val next = ReminderTime.next(WeekDays.ALL, 18 * 60, setOf(now.toLocalDate()), now)
        assertEquals(LocalDateTime.of(2026, 11, 24, 18, 0), next)
    }

    @Test
    fun timeAlreadyPassed_movesToTomorrow() {
        val now = LocalDateTime.of(2026, 11, 23, 19, 0)
        val next = ReminderTime.next(WeekDays.ALL, 8 * 60, emptySet(), now)
        assertEquals(LocalDateTime.of(2026, 11, 24, 8, 0), next)
    }

    @Test
    fun skipsDaysThatAreNotScheduled() {
        val now = LocalDateTime.of(2026, 11, 23, 9, 0)
        val thursday = 1 shl 3
        val next = ReminderTime.next(thursday, 8 * 60, emptySet(), now)
        assertEquals(LocalDateTime.of(2026, 11, 26, 8, 0), next)
    }
}

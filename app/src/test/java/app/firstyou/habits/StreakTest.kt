package app.firstyou.habits

import app.firstyou.habits.data.Streak
import app.firstyou.habits.data.WeekDays
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    @Test
    fun everyDay_todayOpen_keepsYesterdayAndTheDayBefore() {
        val today = LocalDate.of(2026, 11, 25)
        val done = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, Streak.current(WeekDays.schedule(WeekDays.ALL), done, today))
    }

    @Test
    fun everyDay_missedYesterday_breaksTheStreak() {
        val today = LocalDate.of(2026, 11, 25)
        val done = setOf(today.minusDays(2))
        assertEquals(0, Streak.current(WeekDays.schedule(WeekDays.ALL), done, today))
    }

    @Test
    fun mondaysOnly_todayNotDone_stillCountsLastMonday() {
        val today = LocalDate.of(2026, 11, 23)
        val done = setOf(LocalDate.of(2026, 11, 16))
        val mondays = 1 shl 0
        assertEquals(1, Streak.current(WeekDays.schedule(mondays), done, today))
    }

    @Test
    fun mondayAndThursday_aMissedTuesdayDoesNotCount() {
        val today = LocalDate.of(2026, 11, 27)
        val mondayAndThursday = (1 shl 0) or (1 shl 3)
        val done = setOf(LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 26))
        assertEquals(2, Streak.current(WeekDays.schedule(mondayAndThursday), done, today))
    }

    @Test
    fun everyDay_unmarkingYesterday_leavesOnlyToday() {
        val today = LocalDate.of(2026, 11, 25)
        val scheduled = WeekDays.schedule(WeekDays.ALL)
        val both = setOf(today, today.minusDays(1))
        val onlyToday = setOf(today)
        assertEquals(2, Streak.current(scheduled, both, today))
        assertEquals(1, Streak.current(scheduled, onlyToday, today))
    }

    @Test
    fun bestStreak_keepsTheLongestRun() {
        val today = LocalDate.of(2026, 11, 25)
        val done = setOf(
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 21),
            LocalDate.of(2026, 11, 22),
            LocalDate.of(2026, 11, 24),
        )
        assertEquals(3, Streak.best(WeekDays.schedule(WeekDays.ALL), done, today))
        assertEquals(1, Streak.current(WeekDays.schedule(WeekDays.ALL), done, today))
    }
}

package app.firstyou.habits

import app.firstyou.habits.data.Score
import app.firstyou.habits.data.Streak
import app.firstyou.habits.data.WeekDays
import app.firstyou.habits.data.weekdayRates
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreTest {
    @Test
    fun risesWhenDoneAndFallsWhenMissed() {
        val start = LocalDate.of(2026, 11, 23)
        val done = setOf(start, start.plusDays(1))
        val points = Score.series(WeekDays.schedule(WeekDays.ALL), done, start, start.plusDays(2))
        assertEquals(0.10, points[0].second, 0.0001)
        assertEquals(0.19, points[1].second, 0.0001)
        assertEquals(0.19, points[2].second, 0.0001)
    }

    @Test
    fun weekdayRateIgnoresToday() {
        val monday = LocalDate.of(2026, 11, 23)
        val nextMonday = monday.plusWeeks(1)
        val rates = weekdayRates(
            WeekDays.schedule(WeekDays.ALL),
            setOf(monday),
            monday,
            nextMonday.plusDays(1),
        )
        assertEquals(0.5, rates[0], 0.0001)
        assertEquals(0.0, rates[1], 0.0001)
    }

    @Test
    fun listsSeparateRunsByLength() {
        val monday = LocalDate.of(2026, 11, 16)
        val done = setOf(monday, monday.plusDays(1), monday.plusDays(3))
        val runs = Streak.runs(WeekDays.schedule(WeekDays.ALL), done, monday.plusDays(4))
        assertEquals(2, runs.size)
        assertEquals(2, runs.first().days)
        assertTrue(runs[1].days == 1)
    }
}

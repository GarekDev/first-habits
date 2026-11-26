package app.firstyou.habits.data

import java.time.LocalDate

object Streak {
    fun current(
        scheduled: (LocalDate) -> Boolean,
        completed: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        var streak = 0
        var day = today
        repeat(MAX_DAYS) {
            if (scheduled(day)) {
                if (day in completed) {
                    streak += 1
                } else if (day != today) {
                    return streak
                }
            }
            day = day.minusDays(1)
        }
        return streak
    }

    fun best(
        scheduled: (LocalDate) -> Boolean,
        completed: Set<LocalDate>,
        today: LocalDate,
    ): Int {
        if (completed.isEmpty()) return 0
        var best = 0
        var run = 0
        var day = completed.min()
        while (!day.isAfter(today)) {
            if (scheduled(day)) {
                if (day in completed) {
                    run += 1
                    if (run > best) best = run
                } else if (day != today) {
                    run = 0
                }
            }
            day = day.plusDays(1)
        }
        return best
    }

    fun runs(
        scheduled: (LocalDate) -> Boolean,
        completed: Set<LocalDate>,
        today: LocalDate,
    ): List<StreakRun> {
        if (completed.isEmpty()) return emptyList()
        val found = mutableListOf<StreakRun>()
        var runStart: LocalDate? = null
        var runEnd: LocalDate? = null
        var runDays = 0
        var day = completed.min()
        while (!day.isAfter(today)) {
            val counts = scheduled(day) && day in completed
            val missed = scheduled(day) && day !in completed && day != today
            if (counts) {
                if (runStart == null) runStart = day
                runEnd = day
                runDays += 1
            } else if (missed && runStart != null && runEnd != null) {
                found += StreakRun(runStart, runEnd, runDays)
                runStart = null
                runEnd = null
                runDays = 0
            }
            day = day.plusDays(1)
        }
        if (runStart != null && runEnd != null) {
            found += StreakRun(runStart, runEnd, runDays)
        }
        return found.sortedWith(compareByDescending<StreakRun> { it.days }.thenByDescending { it.start })
    }

    private const val MAX_DAYS = 3660
}

data class StreakRun(
    val start: LocalDate,
    val end: LocalDate,
    val days: Int,
)

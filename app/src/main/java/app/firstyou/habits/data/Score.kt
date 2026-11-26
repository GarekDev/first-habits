package app.firstyou.habits.data

import java.time.LocalDate

object Score {
    fun series(
        scheduled: (LocalDate) -> Boolean,
        completed: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate,
    ): List<Pair<LocalDate, Double>> {
        var score = 0.0
        val points = mutableListOf<Pair<LocalDate, Double>>()
        var day = from
        while (!day.isAfter(to)) {
            if (scheduled(day)) {
                val done = day in completed
                score = when {
                    done -> score + (1.0 - score) * RISE
                    day != to -> score * DECAY
                    else -> score
                }
            }
            points += day to score
            day = day.plusDays(1)
        }
        return points
    }

    fun weekly(
        scheduled: (LocalDate) -> Boolean,
        completed: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate,
    ): List<Pair<LocalDate, Double>> {
        val daily = series(scheduled, completed, from, to)
        if (daily.isEmpty()) return emptyList()
        val sampled = daily.filter { (day, _) ->
            day.dayOfWeek.value == 7 || day == to
        }
        return if (sampled.first().first == daily.first().first) sampled else listOf(daily.first()) + sampled
    }

    private const val RISE = 0.10
    private const val DECAY = 0.90
}

fun weekdayRates(
    scheduled: (LocalDate) -> Boolean,
    completed: Set<LocalDate>,
    from: LocalDate,
    today: LocalDate,
): List<Double> {
    val done = IntArray(7)
    val total = IntArray(7)
    var day = from
    while (!day.isAfter(today)) {
        if (scheduled(day) && day != today) {
            val index = day.dayOfWeek.value - 1
            total[index] += 1
            if (day in completed) done[index] += 1
        }
        day = day.plusDays(1)
    }
    return List(7) { index ->
        if (total[index] == 0) 0.0 else done[index].toDouble() / total[index]
    }
}

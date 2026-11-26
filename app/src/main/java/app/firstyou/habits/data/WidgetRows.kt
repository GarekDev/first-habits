package app.firstyou.habits.data

import java.time.LocalDate

data class WidgetRow(
    val id: Long,
    val name: String,
    val color: Int,
    val done: Boolean,
    val mark: String,
)

fun widgetRows(
    habits: List<Habit>,
    completions: Map<Long, Map<LocalDate, Double>>,
    today: LocalDate,
    limit: Int = 5,
    todayOnly: Boolean = true,
): List<WidgetRow> {
    return habits
        .asSequence()
        .filter { habit ->
            !habit.archived && (!todayOnly || WeekDays.isScheduled(habit.days, today))
        }
        .sortedWith(compareBy({ it.position }, { it.id }))
        .take(limit)
        .map { habit ->
            val value = completions[habit.id]?.get(today)
            WidgetRow(
                id = habit.id,
                name = habit.name,
                color = habit.color,
                done = habit.countsAsDone(value),
                mark = widgetMark(habit, value),
            )
        }
        .toList()
}

private fun widgetMark(habit: Habit, value: Double?): String {
    if (habit.kind == HabitKind.MEASURABLE) {
        if (value == null) return ""
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    }
    return if ((value ?: 0.0) > 0.0) "✓" else "○"
}

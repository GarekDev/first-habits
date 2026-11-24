package app.firstyou.habits.data

import java.time.LocalDate

data class WidgetRow(
    val id: Long,
    val name: String,
    val color: Int,
    val done: Boolean,
)

fun widgetRows(
    habits: List<Habit>,
    completions: Map<Long, Set<LocalDate>>,
    today: LocalDate,
    limit: Int = 5,
): List<WidgetRow> {
    return habits
        .asSequence()
        .filter { !it.archived && WeekDays.isScheduled(it.days, today) }
        .sortedWith(compareBy({ it.position }, { it.id }))
        .take(limit)
        .map { habit ->
            WidgetRow(
                id = habit.id,
                name = habit.name,
                color = habit.color,
                done = today in completions[habit.id].orEmpty(),
            )
        }
        .toList()
}

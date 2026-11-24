package app.firstyou.habits

import app.firstyou.habits.data.Habit
import app.firstyou.habits.data.WeekDays
import app.firstyou.habits.data.widgetRows
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRowsTest {
    private val monday = LocalDate.of(2026, 11, 23)

    @Test
    fun keepsTodayAndDropsArchivedOrOtherDays() {
        val habits = listOf(
            habit(1, "Agua", days = WeekDays.ALL, position = 1),
            habit(2, "Archivado", days = WeekDays.ALL, position = 0, archived = true),
            habit(3, "Jueves", days = 1 shl 3, position = 2),
        )
        val rows = widgetRows(habits, emptyMap(), monday)
        assertEquals(listOf("Agua"), rows.map { it.name })
    }

    @Test
    fun marksDoneAndStopsAtFive() {
        val habits = (1..6).map { habit(it.toLong(), "H$it", position = it) }
        val done = mapOf(1L to setOf(monday))
        val rows = widgetRows(habits, done, monday)
        assertEquals(listOf("H1", "H2", "H3", "H4", "H5"), rows.map { it.name })
        assertEquals(true, rows.first().done)
        assertEquals(false, rows[1].done)
    }

    private fun habit(
        id: Long,
        name: String,
        days: Int = WeekDays.ALL,
        position: Int,
        archived: Boolean = false,
    ) = Habit(
        id = id,
        name = name,
        color = 0xFF1F6B4A.toInt(),
        days = days,
        position = position,
        archived = archived,
    )
}

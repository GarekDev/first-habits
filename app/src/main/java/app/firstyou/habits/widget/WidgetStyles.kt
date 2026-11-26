package app.firstyou.habits.widget

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

private const val WidgetCard = 0xFF2C2C2C.toInt()
private const val OldListCard = 0xFF1C1C1C.toInt()

object WidgetRefresh {
    val tick = MutableStateFlow(0)

    fun bump() {
        tick.value += 1
    }
}

data class ListWidgetStyle(
    val background: Int = WidgetCard,
    val text: Int = 0xFFF2F2F2.toInt(),
    val useHabitColor: Boolean = false,
    val fontSize: Int = 15,
    val bold: Boolean = false,
    val corners: Int = 16,
    val padding: Int = 14,
    val todayOnly: Boolean = true,
    val showTitle: Boolean = true,
    val maxRows: Int = 8,
)

data class ChartWidgetStyle(
    val habitId: Long = 0L,
    val background: Int = WidgetCard,
    val text: Int = 0xFFF2F2F2.toInt(),
    val line: Int = 0,
    val showTitle: Boolean = true,
    val showAxis: Boolean = true,
    val corners: Int = 16,
    val titleSize: Int = 18,
)

object WidgetStyles {
    private const val FILE = "first_habits_widgets"

    val backgrounds = listOf(
        WidgetCard,
        0xFF000000.toInt(),
        0xFF121212.toInt(),
        0xFFF4F1EC.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF1F6B4A.toInt(),
        0xFF243E66.toInt(),
        0xFF3A2A4A.toInt(),
        0xFF4A2E2E.toInt(),
    )

    val inks = listOf(
        0xFFFFFFFF.toInt(),
        0xFF1C1B1A.toInt(),
        0xFFB5B5B5.toInt(),
        0xFFD0BCFF.toInt(),
        0xFF81C784.toInt(),
        0xFF90CAF9.toInt(),
        0xFFFFC857.toInt(),
        0xFFFF8A80.toInt(),
    )

    fun list(context: Context, id: Int): ListWidgetStyle {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        var background = prefs.getInt(key(id, "bg"), WidgetCard)
        var habitColor = prefs.getBoolean(key(id, "habit_color"), false)
        var font = prefs.getInt(key(id, "font"), 15)
        var bold = prefs.getBoolean(key(id, "bold"), false)
        var corners = prefs.getInt(key(id, "corners"), 16)
        if (!prefs.contains(key(id, "look"))) {
            if (background == OldListCard) background = WidgetCard
            habitColor = false
            if (font == 16 || !prefs.contains(key(id, "font"))) font = 15
            if (!prefs.contains(key(id, "bold")) || prefs.getBoolean(key(id, "bold"), true)) {
                if (prefs.getInt(key(id, "font"), 16) == 16) bold = false
            }
            if (corners == 18) corners = 16
            prefs.edit()
                .putInt(key(id, "bg"), background)
                .putBoolean(key(id, "habit_color"), habitColor)
                .putInt(key(id, "font"), font)
                .putBoolean(key(id, "bold"), bold)
                .putInt(key(id, "corners"), corners)
                .putInt(key(id, "look"), 2)
                .commit()
        }
        return ListWidgetStyle(
            background = background,
            text = prefs.getInt(key(id, "text"), 0xFFF2F2F2.toInt()),
            useHabitColor = habitColor,
            fontSize = font,
            bold = bold,
            corners = corners,
            padding = prefs.getInt(key(id, "pad"), 14),
            todayOnly = prefs.getBoolean(key(id, "today"), true),
            showTitle = prefs.getBoolean(key(id, "title"), true),
            maxRows = prefs.getInt(key(id, "rows"), 8),
        )
    }

    fun saveList(context: Context, id: Int, style: ListWidgetStyle) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(key(id, "bg"), style.background)
            .putInt(key(id, "text"), style.text)
            .putBoolean(key(id, "habit_color"), style.useHabitColor)
            .putInt(key(id, "font"), style.fontSize)
            .putBoolean(key(id, "bold"), style.bold)
            .putInt(key(id, "corners"), style.corners)
            .putInt(key(id, "pad"), style.padding)
            .putBoolean(key(id, "today"), style.todayOnly)
            .putBoolean(key(id, "title"), style.showTitle)
            .putInt(key(id, "rows"), style.maxRows)
            .commit()
    }

    fun chart(context: Context, id: Int): ChartWidgetStyle {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        var background = prefs.getInt(key(id, "cbg"), WidgetCard)
        var text = prefs.getInt(key(id, "ctext"), 0xFFF2F2F2.toInt())
        var titleSize = prefs.getInt(key(id, "tsize"), 18)
        var corners = prefs.getInt(key(id, "ccorners"), 16)
        if (!prefs.contains(key(id, "clook"))) {
            if (background == 0xFF2B2B2B.toInt()) background = WidgetCard
            if (text == 0xFFE6E6E6.toInt()) text = 0xFFF2F2F2.toInt()
            if (titleSize == 22) titleSize = 18
            if (corners == 18) corners = 16
            prefs.edit()
                .putInt(key(id, "cbg"), background)
                .putInt(key(id, "ctext"), text)
                .putInt(key(id, "tsize"), titleSize)
                .putInt(key(id, "ccorners"), corners)
                .putInt(key(id, "clook"), 2)
                .commit()
        }
        return ChartWidgetStyle(
            habitId = prefs.getLong(key(id, "habit"), 0L),
            background = background,
            text = text,
            line = prefs.getInt(key(id, "line"), 0),
            showTitle = prefs.getBoolean(key(id, "ctitle"), true),
            showAxis = prefs.getBoolean(key(id, "axis"), true),
            corners = corners,
            titleSize = titleSize,
        )
    }

    fun saveChart(context: Context, id: Int, style: ChartWidgetStyle) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putLong(key(id, "habit"), style.habitId)
            .putInt(key(id, "cbg"), style.background)
            .putInt(key(id, "ctext"), style.text)
            .putInt(key(id, "line"), style.line)
            .putBoolean(key(id, "ctitle"), style.showTitle)
            .putBoolean(key(id, "axis"), style.showAxis)
            .putInt(key(id, "ccorners"), style.corners)
            .putInt(key(id, "tsize"), style.titleSize)
            .commit()
    }

    fun clear(context: Context, ids: IntArray) {
        val editor = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        ids.forEach { id ->
            listOf(
                "bg", "text", "habit_color", "font", "bold", "corners", "pad", "today", "title", "rows",
                "habit", "cbg", "ctext", "line", "ctitle", "axis", "ccorners", "tsize", "look", "clook",
            ).forEach { suffix -> editor.remove(key(id, suffix)) }
        }
        editor.apply()
    }

    private fun key(id: Int, name: String) = "w${id}_$name"
}

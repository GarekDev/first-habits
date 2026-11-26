package app.firstyou.habits.data

enum class HabitKind(val storage: String) {
    YES_NO("yes_no"),
    MEASURABLE("measurable"),
    ;

    companion object {
        fun fromStorage(value: String): HabitKind {
            return entries.firstOrNull { it.storage == value } ?: YES_NO
        }
    }
}

data class Habit(
    val id: Long,
    val name: String,
    val color: Int,
    val days: Int,
    val position: Int,
    val reminderMinutes: Int? = null,
    val archived: Boolean = false,
    val kind: HabitKind = HabitKind.YES_NO,
    val unit: String? = null,
    val target: Double? = null,
) {
    fun countsAsDone(value: Double?): Boolean {
        if (value == null) return false
        return when (kind) {
            HabitKind.YES_NO -> value > 0.0
            HabitKind.MEASURABLE -> value + 0.000001 >= (target ?: 1.0)
        }
    }
}

object HabitColors {
    val all = listOf(
        0xFFD0BCFF.toInt(),
        0xFFF48FB1.toInt(),
        0xFF90CAF9.toInt(),
        0xFF81C784.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF1F6B4A.toInt(),
        0xFF2F5D8C.toInt(),
        0xFF8C4A2F.toInt(),
        0xFF6B3FA0.toInt(),
        0xFFA33B3B.toInt(),
        0xFFB8860B.toInt(),
    )
    val default = all.first()
}

object WeekDays {
    val labels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    const val ALL = 0b1111111

    fun has(mask: Int, index: Int): Boolean = mask and (1 shl index) != 0

    fun toggle(mask: Int, index: Int): Int {
        val next = mask xor (1 shl index)
        return if (next == 0) mask else next
    }

    fun summary(mask: Int): String {
        if (mask == ALL) return "Todos los días"
        return labels.filterIndexed { index, _ -> has(mask, index) }.joinToString(", ")
    }

    fun isScheduled(mask: Int, date: java.time.LocalDate): Boolean {
        return has(mask, date.dayOfWeek.value - 1)
    }

    fun schedule(mask: Int): (java.time.LocalDate) -> Boolean = { isScheduled(mask, it) }
}

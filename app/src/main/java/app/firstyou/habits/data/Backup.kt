package app.firstyou.habits.data

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val version: Int,
    val exportedAt: String,
    val habits: List<BackupHabit>,
    val days: List<BackupDay>,
)

@Serializable
data class BackupHabit(
    val id: Long,
    val name: String,
    val color: Int,
    val days: Int,
    val position: Int,
    val reminderMinutes: Int? = null,
    val archived: Boolean = false,
    val createdAt: Long,
    val kind: String = HabitKind.YES_NO.storage,
    val unit: String? = null,
    val target: Double? = null,
)

@Serializable
data class BackupDay(
    val habitId: Long,
    val day: String,
    val value: Double = 1.0,
)

private val json = Json { prettyPrint = true }

fun encodeBackup(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

fun decodeBackup(text: String): BackupFile? {
    val parsed = try {
        json.decodeFromString(BackupFile.serializer(), text)
    } catch (_: Exception) {
        return null
    }
    return parsed.takeIf { (it.version == 1 || it.version == 2) && backupIsReadable(it) }
}

fun backupIsReadable(file: BackupFile): Boolean {
    if (file.exportedAt.isBlank()) return false
    val ids = file.habits.map { it.id }
    if (ids.size != ids.toSet().size) return false
    if (file.habits.any { !habitIsReadable(it) }) return false
    val known = ids.toSet()
    val seen = mutableSetOf<Pair<Long, String>>()
    return file.days.all { day ->
        day.habitId in known &&
            seen.add(day.habitId to day.day) &&
            day.day.length == 10 &&
            try {
                LocalDate.parse(day.day)
                true
            } catch (_: DateTimeParseException) {
                false
            }
    }
}

private fun habitIsReadable(habit: BackupHabit): Boolean {
    val minutes = habit.reminderMinutes
    val measurable = habit.kind == HabitKind.MEASURABLE.storage
    val targetOk = !measurable || ((habit.unit?.isNotBlank() == true) && (habit.target ?: 0.0) > 0.0)
    val valueOk = habit.days != 0
    return habit.id > 0 &&
        habit.name.isNotBlank() &&
        valueOk &&
        targetOk &&
        (minutes == null || minutes in 0 until 24 * 60) &&
        (habit.kind == HabitKind.YES_NO.storage || measurable)
}

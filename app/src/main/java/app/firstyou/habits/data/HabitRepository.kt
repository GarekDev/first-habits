package app.firstyou.habits.data

import app.firstyou.habits.data.local.AppDatabase
import app.firstyou.habits.data.local.CompletionDao
import app.firstyou.habits.data.local.CompletionEntity
import app.firstyou.habits.data.local.HabitDao
import app.firstyou.habits.data.local.HabitEntity
import app.firstyou.habits.data.local.toModel
import androidx.room.withTransaction
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitRepository(
    private val database: AppDatabase,
    private val dao: HabitDao,
    private val completions: CompletionDao,
) {
    var afterChange: (suspend () -> Unit)? = null

    private suspend fun changed() {
        afterChange?.invoke()
    }
    fun observeActive(): Flow<List<Habit>> {
        return dao.observeActive().map { habits -> habits.map { it.toModel() } }
    }

    fun observeArchived(): Flow<List<Habit>> {
        return dao.observeArchived().map { habits -> habits.map { it.toModel() } }
    }

    suspend fun get(id: Long): Habit? = dao.get(id)?.toModel()

    fun observe(id: Long): Flow<Habit?> = dao.observe(id).map { it?.toModel() }

    fun observeEntries(): Flow<Map<Long, Map<LocalDate, Double>>> {
        return completions.observeAll().map { rows ->
            rows.groupBy { it.habitId }.mapValues { (_, days) ->
                days.associate { LocalDate.parse(it.day) to it.value }
            }
        }
    }

    suspend fun setDone(habitId: Long, day: LocalDate, done: Boolean) {
        setEntry(habitId, day, if (done) 1.0 else null)
    }

    suspend fun setEntry(habitId: Long, day: LocalDate, value: Double?) {
        val key = day.toString()
        if (value == null) {
            completions.clear(habitId, key)
        } else {
            completions.upsert(CompletionEntity(habitId = habitId, day = key, value = value))
        }
        changed()
    }

    suspend fun entry(habitId: Long, day: LocalDate): Double? {
        return completions.days(habitId).firstOrNull { it.day == day.toString() }?.value
    }

    suspend fun completedDays(habitId: Long): Set<LocalDate> {
        val habit = get(habitId) ?: return emptySet()
        return completions.days(habitId).mapNotNull { row ->
            val date = LocalDate.parse(row.day)
            if (habit.countsAsDone(row.value)) date else null
        }.toSet()
    }

    suspend fun allHabits(): List<Habit> = dao.all().map { it.toModel() }

    suspend fun save(
        id: Long?,
        name: String,
        color: Int,
        days: Int,
        reminderMinutes: Int?,
        kind: HabitKind = HabitKind.YES_NO,
        unit: String? = null,
        target: Double? = null,
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || days == 0) return
        val cleanUnit = unit?.trim()?.ifBlank { null }
        val storedKind = if (kind == HabitKind.MEASURABLE && (cleanUnit == null || target == null || target <= 0.0)) {
            HabitKind.YES_NO
        } else {
            kind
        }
        if (id == null) {
            dao.insert(
                HabitEntity(
                    name = cleanName,
                    color = color,
                    days = days,
                    archived = false,
                    position = dao.maxPosition() + 1,
                    createdAt = System.currentTimeMillis(),
                    reminderMinutes = reminderMinutes,
                    kind = storedKind.storage,
                    unit = if (storedKind == HabitKind.MEASURABLE) cleanUnit else null,
                    target = if (storedKind == HabitKind.MEASURABLE) target else null,
                ),
            )
        } else {
            val current = dao.get(id) ?: return
            dao.update(
                current.copy(
                    name = cleanName,
                    color = color,
                    days = days,
                    reminderMinutes = reminderMinutes,
                    kind = storedKind.storage,
                    unit = if (storedKind == HabitKind.MEASURABLE) cleanUnit else null,
                    target = if (storedKind == HabitKind.MEASURABLE) target else null,
                ),
            )
        }
        changed()
    }

    suspend fun archive(id: Long) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(archived = true))
        changed()
    }

    suspend fun restore(id: Long) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(archived = false))
        changed()
    }

    suspend fun reorder(ids: List<Long>) {
        if (ids.isEmpty()) return
        database.withTransaction {
            ids.forEachIndexed { index, id ->
                val current = dao.get(id) ?: return@forEachIndexed
                if (current.position != index) dao.update(current.copy(position = index))
            }
        }
        changed()
    }

    suspend fun delete(id: Long) {
        val current = dao.get(id) ?: return
        dao.delete(current)
        changed()
    }

    suspend fun snapshot(exportedAt: String): BackupFile {
        val habits = dao.all()
            .sortedWith(compareBy({ it.position }, { it.id }))
            .map { habit ->
                BackupHabit(
                    id = habit.id,
                    name = habit.name,
                    color = habit.color,
                    days = habit.days,
                    position = habit.position,
                    reminderMinutes = habit.reminderMinutes,
                    archived = habit.archived,
                    createdAt = habit.createdAt,
                    kind = habit.kind,
                    unit = habit.unit,
                    target = habit.target,
                )
            }
        val days = completions.all()
            .sortedWith(compareBy({ it.habitId }, { it.day }))
            .map { BackupDay(habitId = it.habitId, day = it.day, value = it.value) }
        return BackupFile(version = 2, exportedAt = exportedAt, habits = habits, days = days)
    }

    suspend fun replaceWith(file: BackupFile) {
        database.withTransaction {
            dao.deleteAll()
            file.habits.forEach { habit ->
                dao.insert(
                    HabitEntity(
                        id = habit.id,
                        name = habit.name,
                        color = habit.color,
                        days = habit.days,
                        archived = habit.archived,
                        position = habit.position,
                        createdAt = habit.createdAt,
                        reminderMinutes = habit.reminderMinutes,
                        kind = habit.kind,
                        unit = habit.unit,
                        target = habit.target,
                    ),
                )
            }
            file.days.forEach { day ->
                completions.insert(
                    CompletionEntity(habitId = day.habitId, day = day.day, value = day.value),
                )
            }
        }
        changed()
    }
}

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

    fun observeCompletions(): Flow<Map<Long, Set<LocalDate>>> {
        return completions.observeAll().map { rows ->
            rows.groupBy { it.habitId }.mapValues { (_, days) ->
                days.map { LocalDate.parse(it.day) }.toSet()
            }
        }
    }

    suspend fun setDone(habitId: Long, day: LocalDate, done: Boolean) {
        val key = day.toString()
        if (done) {
            completions.insert(CompletionEntity(habitId = habitId, day = key))
        } else {
            completions.clear(habitId, key)
        }
        changed()
    }

    suspend fun completedDays(habitId: Long): Set<LocalDate> {
        return completions.days(habitId).map { LocalDate.parse(it.day) }.toSet()
    }

    suspend fun allHabits(): List<Habit> = dao.all().map { it.toModel() }

    suspend fun save(id: Long?, name: String, color: Int, days: Int, reminderMinutes: Int?) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || days == 0) return
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
                ),
            )
        } else {
            val current = dao.get(id) ?: return
            dao.update(current.copy(name = cleanName, color = color, days = days, reminderMinutes = reminderMinutes))
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
                )
            }
        val days = completions.all()
            .sortedWith(compareBy({ it.habitId }, { it.day }))
            .map { BackupDay(habitId = it.habitId, day = it.day) }
        return BackupFile(version = 1, exportedAt = exportedAt, habits = habits, days = days)
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
                    ),
                )
            }
            file.days.forEach { day ->
                completions.insert(CompletionEntity(habitId = day.habitId, day = day.day))
            }
        }
        changed()
    }
}

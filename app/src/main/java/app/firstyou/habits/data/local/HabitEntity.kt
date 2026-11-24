package app.firstyou.habits.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.firstyou.habits.data.Habit

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val days: Int,
    val archived: Boolean,
    val position: Int,
    val createdAt: Long,
    val reminderMinutes: Int? = null,
)

fun HabitEntity.toModel() = Habit(
    id = id,
    name = name,
    color = color,
    days = days,
    position = position,
    reminderMinutes = reminderMinutes,
    archived = archived,
)

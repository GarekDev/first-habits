package app.firstyou.habits

import app.firstyou.habits.data.BackupDay
import app.firstyou.habits.data.BackupFile
import app.firstyou.habits.data.BackupHabit
import app.firstyou.habits.data.decodeBackup
import app.firstyou.habits.data.encodeBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    @Test
    fun roundTripKeepsHabitsAndDays() {
        val file = BackupFile(
            version = 1,
            exportedAt = "2026-11-23T17:00:00",
            habits = listOf(
                BackupHabit(
                    id = 3,
                    name = "Leer \"hoy\"",
                    color = 1,
                    days = 127,
                    position = 1,
                    reminderMinutes = 450,
                    archived = false,
                    createdAt = 10,
                ),
            ),
            days = listOf(BackupDay(habitId = 3, day = "2026-11-23")),
        )
        assertEquals(file, decodeBackup(encodeBackup(file)))
    }

    @Test
    fun rejectsAnotherVersionGarbageAndADayWithoutItsHabit() {
        val habit = BackupHabit(1, "Agua", 1, 127, 1, null, false, 10)
        val good = BackupFile(1, "2026-11-23T17:00:00", listOf(habit), emptyList())
        assertNull(decodeBackup(encodeBackup(good.copy(version = 3))))
        assertNull(decodeBackup("{"))
        assertNull(
            decodeBackup(
                encodeBackup(good.copy(days = listOf(BackupDay(9, "2026-11-23")))),
            ),
        )
        assertNull(
            decodeBackup(
                encodeBackup(good.copy(days = listOf(BackupDay(1, "ayer")))),
            ),
        )
    }
}

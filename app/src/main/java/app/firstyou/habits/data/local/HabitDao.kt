package app.firstyou.habits.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY position ASC, id ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archived = 1 ORDER BY name COLLATE NOCASE ASC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observe(id: Long): Flow<HabitEntity?>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("SELECT COALESCE(MAX(position), 0) FROM habits")
    suspend fun maxPosition(): Int

    @Query("SELECT * FROM habits")
    suspend fun all(): List<HabitEntity>

    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}

package app.firstyou.habits.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: CompletionEntity)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND day = :day")
    suspend fun clear(habitId: Long, day: String)

    @Query("SELECT * FROM completions WHERE habitId = :habitId")
    suspend fun days(habitId: Long): List<CompletionEntity>

    @Query("SELECT * FROM completions")
    suspend fun all(): List<CompletionEntity>
}


package app.retra.emulator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY COALESCE(lastPlayedAtEpochMillis, importedAtEpochMillis) DESC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT COUNT(*) FROM games WHERE sha256 = :sha256")
    suspend fun countBySha256(sha256: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(game: GameEntity): Long

    @Query("UPDATE games SET lastPlayedAtEpochMillis = :timestamp WHERE id = :id")
    suspend fun markPlayed(id: Long, timestamp: Long)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)
}

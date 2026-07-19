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

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE sha256 = :sha256 LIMIT 1")
    suspend fun getBySha256(sha256: String): GameEntity?

    @Query("SELECT * FROM games WHERE crc32 = :crc32")
    suspend fun findByCrc32(crc32: Long): List<GameEntity>

    @Query("SELECT COUNT(*) FROM games WHERE sha256 = :sha256")
    suspend fun countBySha256(sha256: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(game: GameEntity): Long

    @Query("UPDATE games SET lastPlayedAtEpochMillis = :timestamp WHERE id = :id")
    suspend fun markPlayed(id: Long, timestamp: Long)

    @Query("UPDATE games SET coverArtPath = :path WHERE id = :id")
    suspend fun setCoverArt(id: Long, path: String?)

    @Query("UPDATE games SET title = :title, notes = :notes WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String, notes: String?)

    @Query("UPDATE games SET collectionsCsv = :collectionsCsv, tagsCsv = :tagsCsv WHERE id = :id")
    suspend fun updateOrganization(id: Long, collectionsCsv: String, tagsCsv: String)

    @Query("UPDATE games SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE games SET uri = :uri, managedPath = :managedPath, crc32 = :crc32 WHERE id = :id")
    suspend fun updateManagedStorage(id: Long, uri: String, managedPath: String?, crc32: Long?)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)
}

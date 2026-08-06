package app.axolotl.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY isPinned DESC, timestamp DESC")
    fun getAllClips(): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE isPinned = 0 AND content = :content")
    suspend fun deleteUnpinnedDuplicates(content: String)

    @Query("SELECT COUNT(*) FROM clips WHERE isPinned = 1 AND content = :content")
    suspend fun countPinnedClips(content: String): Int

    @Query(
        """DELETE FROM clips
           WHERE isPinned = 0 AND id NOT IN (
               SELECT id FROM clips WHERE isPinned = 0
               ORDER BY timestamp DESC, id DESC LIMIT :limit
           )"""
    )
    suspend fun trimUnpinnedClips(limit: Int)

    @Transaction
    suspend fun insertDeduplicatedAndTrim(clip: ClipEntity, limit: Int) {
        deleteUnpinnedDuplicates(clip.content)
        if (clip.isPinned || countPinnedClips(clip.content) == 0) {
            insertClip(clip)
        }
        trimUnpinnedClips(limit)
    }

    @Delete
    suspend fun deleteClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE isPinned = 0")
    suspend fun clearUnpinnedClips()
}

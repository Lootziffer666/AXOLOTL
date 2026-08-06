package app.axolotl.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY isPinned DESC, timestamp DESC LIMIT 50")
    fun getAllClips(): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Delete
    suspend fun deleteClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE isPinned = 0")
    suspend fun clearUnpinnedClips()
}

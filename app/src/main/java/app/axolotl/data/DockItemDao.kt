package app.axolotl.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DockItemDao {
    @Query("SELECT * FROM dock_items ORDER BY orderIndex ASC")
    fun getAllDockItems(): Flow<List<DockItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDockItem(item: DockItemEntity)

    @Delete
    suspend fun deleteDockItem(item: DockItemEntity)

    @Query("DELETE FROM dock_items WHERE itemKey = :key")
    suspend fun deleteByKey(key: String)
}

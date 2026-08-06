package app.axolotl.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DockAppDao {
    @Query("SELECT * FROM dock_apps ORDER BY orderIndex ASC")
    fun getAllDockApps(): Flow<List<DockAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDockApp(dockApp: DockAppEntity)

    @Delete
    suspend fun deleteDockApp(dockApp: DockAppEntity)
}

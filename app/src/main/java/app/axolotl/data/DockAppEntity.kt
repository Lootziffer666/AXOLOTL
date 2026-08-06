package app.axolotl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dock_apps")
data class DockAppEntity(
    @PrimaryKey
    val packageName: String,
    val orderIndex: Int = 0
)

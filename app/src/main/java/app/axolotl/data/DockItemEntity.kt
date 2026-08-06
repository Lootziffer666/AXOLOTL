package app.axolotl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dock_items")
data class DockItemEntity(
    @PrimaryKey
    val itemKey: String, // e.g. "app:com.whatsapp" or "feature:snippets" or "feature:clipboard" or "feature:appendix" or "feature:handoff"
    val itemType: String, // "APP" or "FEATURE"
    val title: String,
    val packageNameOrAction: String,
    val orderIndex: Int = 0
)

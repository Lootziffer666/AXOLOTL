package app.axolotl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val contentType: String = "TEXT", // TEXT, MARKDOWN, LINK
    val charCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

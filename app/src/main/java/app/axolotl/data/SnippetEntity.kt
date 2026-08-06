package app.axolotl.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Prompt", // Prompt, Markdown, Rule, Template, Recovery
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

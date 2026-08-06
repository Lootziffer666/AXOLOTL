package app.axolotl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import app.axolotl.data.DockDatabase
import app.axolotl.data.DockRepository
import app.axolotl.data.SettingsManager
import app.axolotl.data.SnippetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BorderlineShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val database = DockDatabase.getDatabase(context)
        val repository = DockRepository(
            database.dockAppDao(),
            database.dockItemDao(),
            database.snippetDao(),
            database.clipDao()
        )
        val settingsManager = SettingsManager.getInstance(context)
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            "app.axolotl.borderline.OPEN_MENU" -> {
                val menuIndex = intent.getIntExtra("menu_index", 0)
                val serviceIntent = Intent(context, DockOverlayService::class.java).apply {
                    action = DockOverlayService.ACTION_OPEN_MENU
                    putExtra(DockOverlayService.EXTRA_MENU_INDEX, menuIndex)
                }
                context.startForegroundService(serviceIntent)
            }
            "app.axolotl.borderline.ADD_SNIPPET" -> {
                val title = intent.getStringExtra("title") ?: "External Snippet"
                val content = intent.getStringExtra("content") ?: ""
                val category = intent.getStringExtra("category") ?: "Prompt"
                if (content.isNotBlank()) {
                    scope.launch {
                        repository.insertSnippet(SnippetEntity(title = title, content = content, category = category))
                    }
                    Toast.makeText(context, "Snippet added to Borderline!", Toast.LENGTH_SHORT).show()
                }
            }
            "app.axolotl.borderline.APPEND_APPENDIX" -> {
                val text = intent.getStringExtra("text") ?: ""
                if (text.isNotBlank()) {
                    settingsManager.appendToAppendix(text)
                    Toast.makeText(context, "Appended to Borderline Appendix!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

package app.axolotl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.axolotl.ui.theme.MyApplicationTheme

class ShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = ShortcutRequest.from(intent)
        if (request == null) {
            finish()
            return
        }

        if (!request.requiresConfirmation) {
            openBorderlineMenu(this, request.menuIndex)
            finish()
            return
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AlertDialog(
                    onDismissRequest = ::finish,
                    title = { Text("Open Borderline overlay?") },
                    text = { Text("A link requested menu ${request.menuIndex + 1}. Continue only if you trust its source.") },
                    confirmButton = {
                        TextButton(onClick = {
                            openBorderlineMenu(this, request.menuIndex)
                            finish()
                        }) { Text("Open") }
                    },
                    dismissButton = {
                        TextButton(onClick = ::finish) { Text("Cancel") }
                    }
                )
            }
        }
    }

    private fun openBorderlineMenu(context: Context, menuIndex: Int) {
        val serviceIntent = Intent(context, DockOverlayService::class.java).apply {
            action = DockOverlayService.ACTION_OPEN_MENU
            putExtra(DockOverlayService.EXTRA_MENU_INDEX, menuIndex)
        }
        context.startForegroundService(serviceIntent)
    }
}

internal data class ShortcutRequest(
    val menuIndex: Int,
    val requiresConfirmation: Boolean
) {
    companion object {
        fun from(intent: Intent): ShortcutRequest? {
            val data = intent.data
            if (intent.action == Intent.ACTION_VIEW && data?.scheme == "borderline") {
                if (data.host != "open") return null
                val menuIndex = data.getQueryParameter("menu")?.toIntOrNull() ?: 0
                return ShortcutRequest(menuIndex.coerceIn(0, 3), requiresConfirmation = true)
            }

            if (intent.action == ACTION_SHORTCUT) {
                return ShortcutRequest(
                    intent.getIntExtra("menu_index", 0).coerceIn(0, 3),
                    requiresConfirmation = false
                )
            }
            return null
        }

        const val ACTION_SHORTCUT = "app.axolotl.borderline.ACTION_SHORTCUT"
    }
}

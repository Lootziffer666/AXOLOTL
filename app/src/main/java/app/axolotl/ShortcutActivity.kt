package app.axolotl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent.data

        if (data != null && data.scheme == "borderline") {
            when (data.host) {
                "open" -> {
                    val menuStr = data.getQueryParameter("menu") ?: "0"
                    val menuIndex = menuStr.toIntOrNull() ?: 0
                    openBorderlineMenu(this, menuIndex)
                }
            }
        } else {
            val menuIndex = intent.getIntExtra("menu_index", 0)
            openBorderlineMenu(this, menuIndex)
        }

        finish()
    }

    private fun openBorderlineMenu(context: Context, menuIndex: Int) {
        val intent = Intent(context, DockOverlayService::class.java).apply {
            action = DockOverlayService.ACTION_OPEN_MENU
            putExtra(DockOverlayService.EXTRA_MENU_INDEX, menuIndex)
        }
        context.startForegroundService(intent)
    }
}

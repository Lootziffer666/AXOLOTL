package app.axolotl

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged

/** Real installed-app catalog. No fabricated clusters or privileged uninstall claims. */
class AppsActivity : ComponentActivity() {
    private data class InstalledApp(val label: String, val packageName: String) {
        override fun toString(): String = "$label\n$packageName"
    }

    private lateinit var allApps: List<InstalledApp>
    private lateinit var adapter: ArrayAdapter<InstalledApp>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val search = EditText(this).apply { hint = "Search installed apps" }
        val help = TextView(this).apply {
            text = "Tap to open · Long press for Android app settings"
        }
        val list = ListView(this)
        allApps = loadLauncherApps()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, allApps)
        list.adapter = adapter
        list.emptyView = TextView(this).apply { text = "No matching launcher apps"; visibility = View.VISIBLE }
        search.doAfterTextChanged { query -> showMatches(query?.toString().orEmpty()) }
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            packageManager.getLaunchIntentForPackage(adapter.getItem(position)!!.packageName)?.let(::startActivity)
        }
        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${adapter.getItem(position)!!.packageName}")
                },
            )
            true
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            addView(search)
            addView(help)
            addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        })
    }

    private fun loadLauncherApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map {
                InstalledApp(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun showMatches(query: String) {
        val normalized = query.trim().lowercase()
        val matches = if (normalized.isEmpty()) allApps else allApps.filter {
            normalized in it.label.lowercase() || normalized in it.packageName.lowercase()
        }
        adapter.clear()
        adapter.addAll(matches)
        adapter.notifyDataSetChanged()
    }
}

package app.axolotl.modules

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.axolotl.evolver.ModuleAction
import app.axolotl.evolver.ModuleCapability
import app.axolotl.evolver.ModuleIcon
import app.axolotl.evolver.ModuleManifest

/** Discovers optional module Activities from this or separately installed APKs. */
class InstalledModuleDiscovery(private val context: Context) {
    data class DiscoveredModule(
        val module: NativeFeatureModule,
        val component: ComponentName,
    )

    fun discover(): List<DiscoveredModule> {
        val intent = Intent(ACTION_MODULE).addCategory(Intent.CATEGORY_DEFAULT)
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .mapNotNull { info ->
                val metadata = info.activityInfo.metaData ?: return@mapNotNull null
                val id = metadata.getString(META_ID) ?: return@mapNotNull null
                if (!MODULE_ID.matches(id)) return@mapNotNull null
                val title = metadata.getString(META_TITLE) ?: return@mapNotNull null
                val description = metadata.getString(META_DESCRIPTION).orEmpty()
                val version = metadata.getInt(META_VERSION, 1)
                val icon = runCatching {
                    ModuleIcon.valueOf(metadata.getString(META_ICON).orEmpty())
                }.getOrDefault(ModuleIcon.AUTOMATE)
                val capabilities = metadata.getString(META_CAPABILITIES).orEmpty()
                    .split(',')
                    .mapNotNull { value ->
                        runCatching { ModuleCapability.valueOf(value.trim()) }.getOrNull()
                    }
                    .toSet()
                DiscoveredModule(
                    module = NativeFeatureModule(
                        ModuleManifest(id, title, description, version, icon, capabilities),
                        ModuleAction("$id.open", "Open $title"),
                    ),
                    component = ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                )
            }
            .sortedByDescending { it.component.packageName == context.packageName }
            .distinctBy { it.module.manifest.id }
            .sortedBy { it.module.manifest.id }
    }

    companion object {
        const val ACTION_MODULE = "app.axolotl.action.MODULE"
        const val META_ID = "app.axolotl.module.ID"
        const val META_TITLE = "app.axolotl.module.TITLE"
        const val META_DESCRIPTION = "app.axolotl.module.DESCRIPTION"
        const val META_VERSION = "app.axolotl.module.VERSION"
        const val META_ICON = "app.axolotl.module.ICON"
        const val META_CAPABILITIES = "app.axolotl.module.CAPABILITIES"
        private val MODULE_ID = Regex("[a-z][a-z0-9-]{2,63}")
    }
}

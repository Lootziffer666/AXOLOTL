package app.axolotl.modules

import android.content.Context
import android.content.Intent
import app.axolotl.evolver.EvolverEngine
import app.axolotl.evolver.ModuleRegistry

/** Runtime composed from the required Borderline frame and optional installed modules. */
object AxolotlRuntime {
    private var initialized = false
    private val components = mutableMapOf<String, android.content.ComponentName>()

    lateinit var registry: ModuleRegistry
        private set
    lateinit var evolver: EvolverEngine
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        registry = createCoreModuleRegistry()
        InstalledModuleDiscovery(context.applicationContext).discover().forEach { discovered ->
            if (registry.find(discovered.module.manifest.id) != null) return@forEach
            registry.register(discovered.module)
            components[discovered.module.manifest.id] = discovered.component
        }
        evolver = EvolverEngine(registry)
        initialized = true
    }

    fun open(context: Context, moduleId: String): Boolean {
        val component = components[moduleId] ?: return false
        context.startActivity(Intent().setComponent(component))
        return true
    }
}

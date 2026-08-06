package app.axolotl.evolver

/** Immutable-at-read registry used as the single module source of truth. */
class ModuleRegistry(modules: Iterable<AxolotlModule> = emptyList()) {
    private val modulesById = linkedMapOf<String, AxolotlModule>()

    init {
        modules.forEach(::register)
    }

    fun register(module: AxolotlModule) {
        require(MODULE_ID.matches(module.manifest.id)) {
            "Invalid module id: ${module.manifest.id}"
        }
        require(module.manifest.version > 0) { "Module version must be positive" }
        require(modulesById.putIfAbsent(module.manifest.id, module) == null) {
            "Duplicate module id: ${module.manifest.id}"
        }
    }

    fun all(): List<AxolotlModule> = modulesById.values.toList()

    fun available(): List<AxolotlModule> = all().filter {
        it.availability == ModuleAvailability.AVAILABLE
    }

    fun find(id: String): AxolotlModule? = modulesById[id]

    companion object {
        private val MODULE_ID = Regex("[a-z][a-z0-9-]{2,63}")
    }
}

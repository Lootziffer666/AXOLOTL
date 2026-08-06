package app.axolotl.modules

import app.axolotl.evolver.EvolverEngine

/** Process-wide module runtime. Persistent snapshots will replace in-memory history later. */
object AxolotlRuntime {
    val registry = createCoreModuleRegistry()
    val evolver = EvolverEngine(registry)
}

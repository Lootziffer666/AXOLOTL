package app.axolotl.evolver

/** A reviewable change to one module surface; it contains data, never code. */
data class EvolutionPatch(
    val moduleId: String,
    val baseRevision: Long,
    val description: String,
    val nodes: List<UiNode>,
)

data class EvolutionSnapshot(
    val moduleId: String,
    val revision: Long,
    val description: String,
    val surface: ModuleSurface,
)

sealed interface EvolutionResult {
    data class Applied(val snapshot: EvolutionSnapshot) : EvolutionResult
    data class Rejected(val reasons: List<String>) : EvolutionResult
}

/**
 * Controlled evolution runtime.
 *
 * An LLM may propose [EvolutionPatch] data later, but only registered action
 * ids and allowlisted [UiNode] values can pass this deterministic gate.
 */
class EvolverEngine(
    private val registry: ModuleRegistry,
    private val maxNodes: Int = 100,
    private val maxDepth: Int = 5,
) {
    private val history = mutableMapOf<String, MutableList<EvolutionSnapshot>>()

    fun currentSurface(moduleId: String): ModuleSurface? {
        val module = registry.find(moduleId) ?: return null
        return history[moduleId]?.lastOrNull()?.surface ?: module.initialSurface()
    }

    fun propose(patch: EvolutionPatch): EvolutionResult {
        val module = registry.find(patch.moduleId)
            ?: return EvolutionResult.Rejected(listOf("Unknown module: ${patch.moduleId}"))
        if (module.availability != ModuleAvailability.AVAILABLE) {
            return EvolutionResult.Rejected(listOf("Module is not available"))
        }

        val current = currentSurface(patch.moduleId)!!
        val errors = buildList {
            if (patch.baseRevision != current.revision) add("Stale base revision")
            if (patch.description.isBlank()) add("Patch description is required")
            validateNodes(patch.nodes, module.actions.map { it.id }.toSet(), this)
        }
        if (errors.isNotEmpty()) return EvolutionResult.Rejected(errors.distinct())

        val surface = ModuleSurface(patch.moduleId, current.revision + 1, patch.nodes)
        val snapshot = EvolutionSnapshot(
            moduleId = patch.moduleId,
            revision = surface.revision,
            description = patch.description,
            surface = surface,
        )
        history.getOrPut(patch.moduleId) { mutableListOf() }.add(snapshot)
        return EvolutionResult.Applied(snapshot)
    }

    fun rollback(moduleId: String): ModuleSurface? {
        val snapshots = history[moduleId] ?: return registry.find(moduleId)?.initialSurface()
        if (snapshots.isNotEmpty()) snapshots.removeLast()
        return snapshots.lastOrNull()?.surface ?: registry.find(moduleId)?.initialSurface()
    }

    fun history(moduleId: String): List<EvolutionSnapshot> = history[moduleId].orEmpty().toList()

    private fun validateNodes(
        nodes: List<UiNode>,
        allowedActions: Set<String>,
        errors: MutableList<String>,
    ) {
        val flattened = mutableListOf<Pair<UiNode, Int>>()
        fun visit(node: UiNode, depth: Int) {
            flattened += node to depth
            if (node is UiNode.Section) node.children.forEach { visit(it, depth + 1) }
        }
        nodes.forEach { visit(it, 1) }

        if (flattened.size > maxNodes) errors += "Surface exceeds $maxNodes nodes"
        if (flattened.any { it.second > maxDepth }) errors += "Surface exceeds depth $maxDepth"
        val ids = flattened.map { it.first.id }
        if (ids.any { it.isBlank() }) errors += "Node ids must not be blank"
        if (ids.size != ids.distinct().size) errors += "Node ids must be unique"
        flattened.map { it.first }.filterIsInstance<UiNode.Action>().forEach {
            if (it.actionId !in allowedActions) errors += "Unknown action: ${it.actionId}"
        }
    }
}

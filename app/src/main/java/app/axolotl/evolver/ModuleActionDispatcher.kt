package app.axolotl.evolver

sealed interface ActionDispatchResult {
    data object Executed : ActionDispatchResult
    data class ConfirmationRequired(val action: ModuleAction) : ActionDispatchResult
    data class Rejected(val reason: String) : ActionDispatchResult
}

/** Executes only actions declared by an available registered module. */
class ModuleActionDispatcher(private val registry: ModuleRegistry) {
    private val handlers = mutableMapOf<String, () -> Unit>()

    fun bind(actionId: String, handler: () -> Unit) {
        require(actionId !in handlers) { "Duplicate action handler: $actionId" }
        handlers[actionId] = handler
    }

    fun dispatch(moduleId: String, actionId: String, confirmed: Boolean = false): ActionDispatchResult {
        val module = registry.find(moduleId)
            ?: return ActionDispatchResult.Rejected("Unknown module: $moduleId")
        if (module.availability != ModuleAvailability.AVAILABLE) {
            return ActionDispatchResult.Rejected("Module is not available")
        }
        val action = module.actions.singleOrNull { it.id == actionId }
            ?: return ActionDispatchResult.Rejected("Action is not declared by module")
        if (action.requiresConfirmation && !confirmed) {
            return ActionDispatchResult.ConfirmationRequired(action)
        }
        val handler = handlers[actionId]
            ?: return ActionDispatchResult.Rejected("No action handler is bound")
        handler()
        return ActionDispatchResult.Executed
    }
}

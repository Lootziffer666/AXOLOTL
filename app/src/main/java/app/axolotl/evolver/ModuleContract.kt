package app.axolotl.evolver

/** Stable metadata used by the shell, Evolver and future persistence. */
data class ModuleManifest(
    val id: String,
    val title: String,
    val description: String,
    val version: Int,
    val icon: ModuleIcon,
    val capabilities: Set<ModuleCapability> = emptySet(),
)

enum class ModuleIcon { BORDERLINE, APPS, FILES, BROWSER, AUTOMATE, AI }

enum class ModuleCapability {
    OVERLAY,
    CLIPBOARD,
    APP_CATALOG,
    FILE_INDEX,
    WEB_CONTENT,
    AI_GATEWAY,
    GENERATIVE_UI,
}

enum class ModuleAvailability { AVAILABLE, PLANNED, DISABLED }

/**
 * Feature boundary for everything mounted into AXOLOTL.
 *
 * Modules publish actions and a declarative surface. They never hand arbitrary
 * Kotlin, HTML or JavaScript to Evolver for execution.
 */
interface AxolotlModule {
    val manifest: ModuleManifest
    val availability: ModuleAvailability
    val actions: Set<ModuleAction>
    fun initialSurface(): ModuleSurface
}

data class ModuleAction(
    val id: String,
    val title: String,
    val requiresConfirmation: Boolean = false,
)

data class ModuleSurface(
    val moduleId: String,
    val revision: Long,
    val nodes: List<UiNode>,
)

/** Closed UI allowlist. New node types require a reviewed app release. */
sealed interface UiNode {
    val id: String

    data class Heading(override val id: String, val text: String) : UiNode
    data class Paragraph(override val id: String, val text: String) : UiNode
    data class Action(override val id: String, val actionId: String, val label: String) : UiNode
    data class Section(override val id: String, val title: String, val children: List<UiNode>) : UiNode
}

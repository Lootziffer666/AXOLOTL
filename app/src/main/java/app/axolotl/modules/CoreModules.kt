package app.axolotl.modules

import app.axolotl.evolver.AxolotlModule
import app.axolotl.evolver.ModuleAction
import app.axolotl.evolver.ModuleAvailability
import app.axolotl.evolver.ModuleCapability
import app.axolotl.evolver.ModuleIcon
import app.axolotl.evolver.ModuleManifest
import app.axolotl.evolver.ModuleRegistry
import app.axolotl.evolver.ModuleSurface
import app.axolotl.evolver.UiNode

class BorderlineModule : AxolotlModule {
    override val manifest = ModuleManifest(
        id = "borderline",
        title = "Borderline",
        description = "Dock, clipboard, snippets and handoffs",
        version = 1,
        icon = ModuleIcon.BORDERLINE,
        capabilities = setOf(ModuleCapability.OVERLAY, ModuleCapability.CLIPBOARD),
    )
    override val availability = ModuleAvailability.AVAILABLE
    override val actions = setOf(ModuleAction("borderline.open", "Open control center"))
    override fun initialSurface() = ModuleSurface(
        moduleId = manifest.id,
        revision = 0,
        nodes = listOf(
            UiNode.Heading("title", "Borderline"),
            UiNode.Paragraph("summary", manifest.description),
            UiNode.Action("open", "borderline.open", "Open control center"),
        ),
    )
}

class PlannedModule(
    override val manifest: ModuleManifest,
) : AxolotlModule {
    override val availability = ModuleAvailability.PLANNED
    override val actions = emptySet<ModuleAction>()
    override fun initialSurface() = ModuleSurface(
        moduleId = manifest.id,
        revision = 0,
        nodes = listOf(
            UiNode.Heading("title", manifest.title),
            UiNode.Paragraph("status", "Migration is planned but not installed."),
        ),
    )
}

class NativeFeatureModule(
    override val manifest: ModuleManifest,
    private val action: ModuleAction,
) : AxolotlModule {
    override val availability = ModuleAvailability.AVAILABLE
    override val actions = setOf(action)
    override fun initialSurface() = ModuleSurface(
        moduleId = manifest.id,
        revision = 0,
        nodes = listOf(
            UiNode.Heading("title", manifest.title),
            UiNode.Paragraph("summary", manifest.description),
            UiNode.Action("open", action.id, action.title),
        ),
    )
}

fun createCoreModuleRegistry(): ModuleRegistry = ModuleRegistry(
    listOf(
        BorderlineModule(),
        PlannedModule(ModuleManifest("apps", "Apps", "Cluster and manage installed apps", 1, ModuleIcon.APPS, setOf(ModuleCapability.APP_CATALOG))),
        NativeFeatureModule(ModuleManifest("files", "Files", "Browse real document-provider folders", 1, ModuleIcon.FILES, setOf(ModuleCapability.FILE_INDEX)), ModuleAction("files.open", "Open files")),
        NativeFeatureModule(ModuleManifest("browser", "Browser", "Hardened HTTP/HTTPS WebView", 1, ModuleIcon.BROWSER, setOf(ModuleCapability.WEB_CONTENT)), ModuleAction("browser.open", "Open browser")),
        PlannedModule(ModuleManifest("automate", "Automate", "Reviewed generative workflows", 1, ModuleIcon.AUTOMATE, setOf(ModuleCapability.GENERATIVE_UI))),
        PlannedModule(ModuleManifest("ai-models", "AI & Models", "BELLOWS routing, memory and providers", 1, ModuleIcon.AI, setOf(ModuleCapability.AI_GATEWAY))),
    ),
)

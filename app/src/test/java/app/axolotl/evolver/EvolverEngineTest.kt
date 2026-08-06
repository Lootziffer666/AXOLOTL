package app.axolotl.evolver

import app.axolotl.modules.createCoreModuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvolverEngineTest {
    private val registry = createCoreModuleRegistry()
    private val engine = EvolverEngine(registry)

    @Test
    fun `core registry exposes all implemented modules`() {
        assertEquals(6, registry.available().size)
        assertEquals(6, registry.all().size)
    }

    @Test
    fun `valid declarative patch is applied and can be rolled back`() {
        val result = engine.propose(
            EvolutionPatch(
                moduleId = "borderline",
                baseRevision = 0,
                description = "Clarify the Borderline entry point",
                nodes = listOf(
                    UiNode.Heading("heading", "Borderline workspace"),
                    UiNode.Action("open", "borderline.open", "Open"),
                ),
            ),
        )

        assertTrue(result is EvolutionResult.Applied)
        assertEquals(1, engine.currentSurface("borderline")?.revision)
        assertEquals(0, engine.rollback("borderline")?.revision)
    }

    @Test
    fun `unknown actions and modules are rejected`() {
        val unknownAction = engine.propose(
            EvolutionPatch(
                "borderline",
                0,
                "Try an unregistered action",
                listOf(UiNode.Action("bad", "system.shell", "Run")),
            ),
        )
        val unknownModule = engine.propose(
            EvolutionPatch("missing", 0, "Unknown target", listOf(UiNode.Heading("h", "Missing"))),
        )

        assertTrue(unknownAction is EvolutionResult.Rejected)
        assertTrue(unknownModule is EvolutionResult.Rejected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate module ids fail registration`() {
        ModuleRegistry(listOf(registry.find("borderline")!!, registry.find("borderline")!!))
    }

    @Test
    fun `dispatcher executes declared actions only`() {
        var calls = 0
        val dispatcher = ModuleActionDispatcher(registry).apply {
            bind("borderline.open") { calls++ }
        }

        assertEquals(ActionDispatchResult.Executed, dispatcher.dispatch("borderline", "borderline.open"))
        assertTrue(dispatcher.dispatch("borderline", "system.shell") is ActionDispatchResult.Rejected)
        assertTrue(dispatcher.dispatch("missing", "borderline.open") is ActionDispatchResult.Rejected)
        assertEquals(1, calls)
    }
}

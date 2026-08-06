package app.axolotl

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import app.axolotl.evolver.EvolutionPatch
import app.axolotl.evolver.EvolutionResult
import app.axolotl.evolver.UiNode
import app.axolotl.modules.AxolotlRuntime

/** Review screen for deterministic, declarative Evolver patches. */
class AutomateActivity : ComponentActivity() {
    private lateinit var modules: Spinner
    private lateinit var heading: EditText
    private lateinit var paragraph: EditText
    private lateinit var status: TextView
    private val availableModules by lazy {
        AxolotlRuntime.initialize(applicationContext)
        AxolotlRuntime.registry.available()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modules = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AutomateActivity,
                android.R.layout.simple_spinner_dropdown_item,
                availableModules.map { it.manifest.title },
            )
        }
        heading = EditText(this).apply { hint = "New heading" }
        paragraph = EditText(this).apply { hint = "New paragraph"; minLines = 3 }
        status = TextView(this).apply { text = "Changes are validated locally and never execute generated code." }
        val apply = Button(this).apply {
            text = "Validate & apply patch"
            setOnClickListener { applyPatch() }
        }
        val rollback = Button(this).apply {
            text = "Rollback latest patch"
            setOnClickListener { rollback() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            addView(TextView(this@AutomateActivity).apply { text = "EVOLVER · Declarative module editor" })
            addView(modules)
            addView(heading)
            addView(paragraph)
            addView(apply)
            addView(rollback)
            addView(status)
        })
    }

    private fun selectedModule() = availableModules[modules.selectedItemPosition]

    private fun applyPatch() {
        val module = selectedModule()
        val current = AxolotlRuntime.evolver.currentSurface(module.manifest.id) ?: return
        val title = heading.text.toString().trim()
        val body = paragraph.text.toString().trim()
        if (title.isEmpty() || body.isEmpty()) {
            status.text = "Heading and paragraph are required"
            return
        }
        val result = AxolotlRuntime.evolver.propose(
            EvolutionPatch(
                moduleId = module.manifest.id,
                baseRevision = current.revision,
                description = "Manual reviewed patch",
                nodes = listOf(
                    UiNode.Heading("heading-${current.revision + 1}", title),
                    UiNode.Paragraph("paragraph-${current.revision + 1}", body),
                ),
            ),
        )
        status.text = when (result) {
            is EvolutionResult.Applied -> "Applied revision ${result.snapshot.revision} to ${module.manifest.title}"
            is EvolutionResult.Rejected -> "Rejected: ${result.reasons.joinToString()}"
        }
    }

    private fun rollback() {
        val module = selectedModule()
        val surface = AxolotlRuntime.evolver.rollback(module.manifest.id)
        status.text = if (surface == null) "Nothing to roll back" else
            "${module.manifest.title} is now at revision ${surface.revision}"
    }
}

package app.axolotl

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import app.axolotl.ai.AiCompletionRequest
import app.axolotl.ai.AiCompletionResult
import app.axolotl.ai.OpenAiCompatibleGateway
import java.net.URI

/** Real OpenAI-compatible client. Credentials stay in memory and are never persisted. */
class AiModelsActivity : ComponentActivity() {
    private val gateway = OpenAiCompatibleGateway()
    private lateinit var endpoint: EditText
    private lateinit var model: EditText
    private lateinit var token: EditText
    private lateinit var prompt: EditText
    private lateinit var output: TextView
    private lateinit var progress: ProgressBar
    private lateinit var send: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endpoint = field("HTTPS chat-completions endpoint", "https://api.openai.com/v1/chat/completions")
        model = field("Model", "gpt-4o-mini")
        token = field("API token (kept in memory)", "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        prompt = field("Prompt", "Reply with a short hello.").apply { minLines = 3 }
        output = TextView(this).apply { text = "No request sent"; setTextIsSelectable(true) }
        progress = ProgressBar(this).apply { visibility = View.GONE }
        send = Button(this).apply {
            text = "Send request"
            setOnClickListener { submit() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 48, 24, 24)
            addView(endpoint)
            addView(model)
            addView(token)
            addView(prompt)
            addView(send)
            addView(progress)
            addView(output)
        })
    }

    private fun field(hint: String, value: String) = EditText(this).apply {
        this.hint = hint
        setText(value)
    }

    private fun submit() {
        val endpointValue = endpoint.text.toString().trim()
        val error = AiEndpointPolicy.validate(endpointValue)
        if (error != null) {
            output.text = error
            return
        }
        val modelValue = model.text.toString().trim()
        val promptValue = prompt.text.toString().trim()
        if (modelValue.isEmpty() || promptValue.isEmpty()) {
            output.text = "Model and prompt are required"
            return
        }
        setBusy(true)
        Thread {
            val result = runCatching {
                when (val response = gateway.complete(AiCompletionRequest(endpointValue, modelValue, token.text.toString(), promptValue))) {
                    is AiCompletionResult.Success -> response.text
                    is AiCompletionResult.HttpError -> "HTTP ${response.status}\n${response.body}"
                    is AiCompletionResult.InvalidResponse -> "Provider returned no assistant message\n${response.body}"
                }
            }.fold(onSuccess = { it }, onFailure = { "Request failed: ${it.message}" })
            runOnUiThread {
                output.text = result
                setBusy(false)
            }
        }.start()
    }

    private fun setBusy(busy: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        send.isEnabled = !busy
    }
}

object AiEndpointPolicy {
    fun validate(value: String): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return "Endpoint is not a valid URI"
        if (uri.scheme != "https") return "Only HTTPS endpoints are allowed"
        if (uri.host.isNullOrBlank()) return "Endpoint must include a host"
        if (uri.userInfo != null) return "Credentials must not be embedded in the endpoint"
        if (!uri.path.trimEnd('/').endsWith("chat/completions")) {
            return "Endpoint must target chat/completions"
        }
        return null
    }
}

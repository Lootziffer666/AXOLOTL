package app.axolotl.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

fun interface AiGateway {
    fun complete(request: AiCompletionRequest): AiCompletionResult
}

data class AiCompletionRequest(
    val endpoint: String,
    val model: String,
    val token: String,
    val prompt: String,
)

sealed interface AiCompletionResult {
    data class Success(val text: String) : AiCompletionResult
    data class HttpError(val status: Int, val body: String) : AiCompletionResult
    data class InvalidResponse(val body: String) : AiCompletionResult
}

/** Synchronous transport intended to run on a worker thread. */
class OpenAiCompatibleGateway : AiGateway {
    override fun complete(request: AiCompletionRequest): AiCompletionResult {
        val body = JSONObject()
            .put("model", request.model)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", request.prompt)),
            )
        val connection = URL(request.endpoint).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            if (request.token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer ${request.token.trim()}")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) return AiCompletionResult.HttpError(status, response)
            val text = JSONObject(response)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
            if (text == null) AiCompletionResult.InvalidResponse(response) else AiCompletionResult.Success(text)
        } finally {
            connection.disconnect()
        }
    }
}

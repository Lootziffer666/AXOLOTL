package app.axolotl.data

internal object ClipboardFailureDiagnostics {
    fun describe(cause: Throwable, sdk: Int, manufacturer: String): String {
        val reason = when (cause) {
            is SecurityException -> "security policy"
            is IllegalStateException -> "background or lifecycle restriction"
            else -> cause.javaClass.simpleName.ifBlank { "platform restriction" }
        }
        val vendor = manufacturer.trim().ifBlank { "unknown vendor" }
        return "Android $sdk ($vendor) blocked clipboard access: $reason"
    }
}

package app.axolotl.utils

import java.io.BufferedReader
import java.io.InputStreamReader

object LogReaderHelper {

    fun readRecentLogs(filterTag: String = "", maxLines: Int = 100): String {
        return try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            val logLines = mutableListOf<String>()

            var line: String? = bufferedReader.readLine()
            while (line != null) {
                if (filterTag.isBlank() || line.contains(filterTag, ignoreCase = true)) {
                    logLines.add(line)
                }
                line = bufferedReader.readLine()
            }

            if (logLines.size > maxLines) {
                logLines.takeLast(maxLines).joinToString("\n")
            } else {
                logLines.joinToString("\n")
            }
        } catch (e: Exception) {
            "App diagnostics unavailable: ${e.javaClass.simpleName}"
        }
    }

    fun clearLogcat() {
        try {
            Runtime.getRuntime().exec("logcat -c")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

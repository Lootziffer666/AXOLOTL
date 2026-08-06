package app.axolotl

import app.axolotl.data.ClipboardFailureDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardFailureDiagnosticsTest {
    @Test
    fun securityFailureIncludesPlatformWithoutClipboardContent() {
        val message = ClipboardFailureDiagnostics.describe(
            SecurityException("secret clipboard payload"),
            sdk = 35,
            manufacturer = "ExampleOEM"
        )

        assertEquals(
            "Android 35 (ExampleOEM) blocked clipboard access: security policy",
            message
        )
    }

    @Test
    fun lifecycleFailureHasDistinctReason() {
        val message = ClipboardFailureDiagnostics.describe(
            IllegalStateException(),
            sdk = 33,
            manufacturer = ""
        )

        assertEquals(
            "Android 33 (unknown vendor) blocked clipboard access: background or lifecycle restriction",
            message
        )
    }
}

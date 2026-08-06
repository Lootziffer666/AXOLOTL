package app.axolotl

import app.axolotl.data.ClipboardCaptureState
import app.axolotl.data.ClipboardCaptureStatus
import app.axolotl.data.DockSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClipboardPrivacyDefaultsTest {
    @Test
    fun clipboardHistoryRequiresExplicitOptIn() {
        assertFalse(DockSettings().clipboardHistoryEnabled)
    }

    @Test
    fun captureStatusStartsIdleWithoutClaimingCapture() {
        val status = ClipboardCaptureStatus()

        assertEquals(ClipboardCaptureState.IDLE, status.state)
        assertEquals("Clipboard history is off", status.message)
    }
}

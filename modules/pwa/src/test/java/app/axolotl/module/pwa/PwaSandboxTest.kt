package app.axolotl.module.pwa

import org.junit.Assert.assertTrue
import org.junit.Test

class PwaSandboxTest {
    @Test
    fun `sandbox adds restrictive CSP and keeps local app code`() {
        val document = PwaStudioActivity.sandboxDocument("<script>document.body.textContent='ok'</script>")

        assertTrue(document.contains("default-src 'none'"))
        assertTrue(document.contains("script-src 'unsafe-inline'"))
        assertTrue(document.contains("document.body.textContent='ok'"))
    }
}

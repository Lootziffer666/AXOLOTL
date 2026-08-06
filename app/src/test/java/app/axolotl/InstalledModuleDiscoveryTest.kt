package app.axolotl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.axolotl.modules.InstalledModuleDiscovery
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstalledModuleDiscoveryTest {
    @Test
    fun `bundled optional activities are discovered only through manifest metadata`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(
            listOf("ai-models", "automate", "browser", "files"),
            InstalledModuleDiscovery(context).discover().map { it.module.manifest.id },
        )
    }
}

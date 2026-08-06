package app.axolotl

import org.junit.Assert.*
import org.junit.Test
import app.axolotl.modules.createCoreModuleRegistry

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class WorkspaceRegistryTest {
  @Test
  fun `base app requires only the Borderline frame`() {
    assertEquals(listOf("borderline"), createCoreModuleRegistry().all().map { it.manifest.id })
  }
}

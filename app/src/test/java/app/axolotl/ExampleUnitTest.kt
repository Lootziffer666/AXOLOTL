package app.axolotl

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class WorkspaceRegistryTest {
  @Test
  fun `all migration targets are represented once`() {
    assertEquals(
      listOf("Apps", "Files", "Browser", "Automate", "AI & Models"),
      coreModuleRegistry.all().filterNot { it.manifest.id == "borderline" }.map { it.manifest.title },
    )
    assertEquals(6, coreModuleRegistry.all().distinctBy { it.manifest.id }.size)
  }
}

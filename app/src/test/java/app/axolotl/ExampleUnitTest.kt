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
      workspaceFeatures.map { it.title },
    )
    assertEquals(workspaceFeatures.size, workspaceFeatures.distinctBy { it.title }.size)
  }
}

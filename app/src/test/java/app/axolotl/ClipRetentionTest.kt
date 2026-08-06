package app.axolotl

import app.axolotl.data.ClipEntity
import app.axolotl.data.DockRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipRetentionTest {
    @Test
    fun oversizedClipIsCappedWithConsistentCharacterCount() {
        val content = "x".repeat(DockRepository.MAX_CLIP_CHARACTERS + 1)

        val retained = DockRepository.enforceClipSizeLimit(
            ClipEntity(content = content, charCount = content.length)
        )

        assertEquals(DockRepository.MAX_CLIP_CHARACTERS, retained.content.length)
        assertEquals(DockRepository.MAX_CLIP_CHARACTERS, retained.charCount)
    }

    @Test
    fun shortClipGetsItsActualCharacterCount() {
        val retained = DockRepository.enforceClipSizeLimit(
            ClipEntity(content = "axolotl", charCount = 0)
        )

        assertEquals("axolotl", retained.content)
        assertEquals(7, retained.charCount)
    }
}

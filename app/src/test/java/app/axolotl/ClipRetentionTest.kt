package app.axolotl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.axolotl.data.ClipEntity
import app.axolotl.data.DockDatabase
import app.axolotl.data.DockRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClipRetentionTest {
    private lateinit var database: DockDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

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

    @Test
    fun insertionRetainsOnlyNewestUnpinnedClips() = runTest {
        val dao = database.clipDao()

        repeat(DockRepository.MAX_UNPINNED_CLIPS + 2) { index ->
            dao.insertDeduplicatedAndTrim(
                ClipEntity(content = "clip-$index", timestamp = index.toLong()),
                DockRepository.MAX_UNPINNED_CLIPS
            )
        }

        val retained = dao.getAllClips().first()
        assertEquals(DockRepository.MAX_UNPINNED_CLIPS, retained.size)
        assertEquals("clip-51", retained.first().content)
        assertEquals("clip-2", retained.last().content)
    }

    @Test
    fun duplicateRefreshesUnpinnedClipWithoutGrowingHistory() = runTest {
        val dao = database.clipDao()
        dao.insertDeduplicatedAndTrim(ClipEntity(content = "same", timestamp = 1), 50)
        dao.insertDeduplicatedAndTrim(ClipEntity(content = "same", timestamp = 2), 50)

        val retained = dao.getAllClips().first()
        assertEquals(1, retained.size)
        assertEquals(2, retained.single().timestamp)
    }

    @Test
    fun pinnedDuplicateIsNotShadowedByNewUnpinnedClip() = runTest {
        val dao = database.clipDao()
        dao.insertClip(ClipEntity(content = "keep", timestamp = 1, isPinned = true))

        dao.insertDeduplicatedAndTrim(ClipEntity(content = "keep", timestamp = 2), 50)

        val retained = dao.getAllClips().first()
        assertEquals(1, retained.size)
        assertEquals(true, retained.single().isPinned)
    }
}

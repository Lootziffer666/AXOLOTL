package app.axolotl.data

import kotlinx.coroutines.flow.Flow

class DockRepository(
    private val dockAppDao: DockAppDao,
    private val dockItemDao: DockItemDao,
    private val snippetDao: SnippetDao,
    private val clipDao: ClipDao
) {
    val allDockApps: Flow<List<DockAppEntity>> = dockAppDao.getAllDockApps()
    val allDockItems: Flow<List<DockItemEntity>> = dockItemDao.getAllDockItems()
    val allSnippets: Flow<List<SnippetEntity>> = snippetDao.getAllSnippets()
    val allClips: Flow<List<ClipEntity>> = clipDao.getAllClips()

    suspend fun insertDockApp(packageName: String, orderIndex: Int) {
        dockAppDao.insertDockApp(DockAppEntity(packageName, orderIndex))
    }

    suspend fun deleteDockApp(packageName: String) {
        dockAppDao.deleteDockApp(DockAppEntity(packageName))
    }

    suspend fun insertDockItem(item: DockItemEntity) {
        dockItemDao.insertDockItem(item)
    }

    suspend fun deleteDockItem(key: String) {
        dockItemDao.deleteByKey(key)
    }

    suspend fun insertSnippet(snippet: SnippetEntity) {
        snippetDao.insertSnippet(snippet)
    }

    suspend fun updateSnippet(snippet: SnippetEntity) {
        snippetDao.updateSnippet(snippet)
    }

    suspend fun deleteSnippet(snippet: SnippetEntity) {
        snippetDao.deleteSnippet(snippet)
    }

    suspend fun insertClip(clip: ClipEntity) {
        val retainedClip = enforceClipSizeLimit(clip)
        clipDao.insertDeduplicatedAndTrim(
            retainedClip,
            MAX_UNPINNED_CLIPS,
            System.currentTimeMillis() - CLIP_RETENTION_MILLIS
        )
    }

    suspend fun pruneExpiredClips(now: Long = System.currentTimeMillis()) {
        clipDao.deleteExpiredUnpinnedClips(now - CLIP_RETENTION_MILLIS)
    }

    suspend fun deleteClip(clip: ClipEntity) {
        clipDao.deleteClip(clip)
    }

    suspend fun clearUnpinnedClips() {
        clipDao.clearUnpinnedClips()
    }

    companion object {
        const val MAX_UNPINNED_CLIPS = 50
        const val MAX_CLIP_CHARACTERS = 100_000
        const val CLIP_RETENTION_DAYS = 30L
        const val CLIP_RETENTION_MILLIS = CLIP_RETENTION_DAYS * 24 * 60 * 60 * 1000

        internal fun enforceClipSizeLimit(clip: ClipEntity): ClipEntity = clip.copy(
            content = clip.content.take(MAX_CLIP_CHARACTERS),
            charCount = clip.content.length.coerceAtMost(MAX_CLIP_CHARACTERS)
        )
    }
}

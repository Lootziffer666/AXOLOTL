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
        clipDao.insertClip(clip)
    }

    suspend fun deleteClip(clip: ClipEntity) {
        clipDao.deleteClip(clip)
    }

    suspend fun clearUnpinnedClips() {
        clipDao.clearUnpinnedClips()
    }
}

package app.axolotl.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.axolotl.data.ClipEntity
import app.axolotl.data.DockDatabase
import app.axolotl.data.DockItemEntity
import app.axolotl.data.DockRepository
import app.axolotl.data.SnippetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppInfo(
    val packageName: String,
    val name: String,
    val isSelected: Boolean
)

data class FeatureInfo(
    val key: String,
    val title: String,
    val description: String,
    val isSelected: Boolean
)

class DockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DockRepository
    private val packageManager: PackageManager = application.packageManager

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    val installedAppsState: StateFlow<List<AppInfo>>
    val dockItemsState: StateFlow<List<DockItemEntity>>
    val snippetsState: StateFlow<List<SnippetEntity>>
    val clipsState: StateFlow<List<ClipEntity>>

    val systemFeatures = listOf(
        FeatureInfo("feature:snippets", "Snippet Capsule", "Hands-free Prompt & Markdown Templates", false),
        FeatureInfo("feature:clipboard", "Clipboard+", "Clipboard History & Artifact Viewer", false),
        FeatureInfo("feature:appendix", "Appendix Mode", "Auto-append copied items as bullet points", false),
        FeatureInfo("feature:handoff", "Quick Handoff", "Pass clipboard content directly to AI / Search / Maps", false)
    )

    private val _features = MutableStateFlow(systemFeatures)
    val featuresState: StateFlow<List<FeatureInfo>>

    init {
        val database = DockDatabase.getDatabase(application)
        repository = DockRepository(
            database.dockAppDao(),
            database.dockItemDao(),
            database.snippetDao(),
            database.clipDao()
        )

        dockItemsState = repository.allDockItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        snippetsState = repository.allSnippets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        clipsState = repository.allClips.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        installedAppsState = _installedApps.combine(repository.allDockApps) { installed, dockApps ->
            val dockAppPackages = dockApps.map { it.packageName }.toSet()
            installed.map { app ->
                app.copy(isSelected = dockAppPackages.contains(app.packageName))
            }.sortedByDescending { it.isSelected }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        featuresState = _features.combine(repository.allDockItems) { featuresList, dockItems ->
            val activeKeys = dockItems.map { it.itemKey }.toSet()
            featuresList.map { feat ->
                feat.copy(isSelected = activeKeys.contains(feat.key))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = systemFeatures
        )

        loadInstalledApps()
        seedDefaultSnippetsIfEmpty()
    }

    private fun seedDefaultSnippetsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentSnippets = repository.allSnippets.first()
            if (currentSnippets.isEmpty()) {
                repository.insertSnippet(
                    SnippetEntity(
                        title = "PRD Gate Block",
                        content = "### PRD Gate Requirements\n- Target OS: Android 14+\n- Architecture: MVVM + Compose\n- Offline First: Local Room DB\n- Zero Unsolicited Dependencies",
                        category = "Rule",
                        isFavorite = true
                    )
                )
                repository.insertSnippet(
                    SnippetEntity(
                        title = "ChatGPT / Claude Prompt",
                        content = "Review the following code for memory leaks, missing state preservation, and unhandled coroutine cancellations. Provide actionable fixes:",
                        category = "Prompt",
                        isFavorite = true
                    )
                )
                repository.insertSnippet(
                    SnippetEntity(
                        title = "Bug Recovery Instruction",
                        content = "1. Clean build cache\n2. Verify AndroidManifest permissions\n3. Restart Foreground Overlay Service\n4. Inspect logcat for Room/SQLite exceptions",
                        category = "Recovery",
                        isFavorite = false
                    )
                )
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)

            val apps = resolveInfos.mapNotNull { resolveInfo ->
                if (resolveInfo.activityInfo.packageName == getApplication<Application>().packageName) return@mapNotNull null
                val name = resolveInfo.loadLabel(packageManager).toString()
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    name = name,
                    isSelected = false
                )
            }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }

            _installedApps.value = apps
        }
    }

    fun toggleApp(app: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val itemKey = "app:${app.packageName}"
            if (app.isSelected) {
                repository.deleteDockApp(app.packageName)
                repository.deleteDockItem(itemKey)
            } else {
                repository.insertDockApp(app.packageName, 0)
                repository.insertDockItem(
                    DockItemEntity(
                        itemKey = itemKey,
                        itemType = "APP",
                        title = app.name,
                        packageNameOrAction = app.packageName
                    )
                )
            }
        }
    }

    fun toggleFeature(feature: FeatureInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            if (feature.isSelected) {
                repository.deleteDockItem(feature.key)
            } else {
                repository.insertDockItem(
                    DockItemEntity(
                        itemKey = feature.key,
                        itemType = "FEATURE",
                        title = feature.title,
                        packageNameOrAction = feature.key
                    )
                )
            }
        }
    }

    fun addSnippet(title: String, content: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSnippet(
                SnippetEntity(
                    title = title.ifBlank { "Untitled Snippet" },
                    content = content,
                    category = category
                )
            )
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSnippet(snippet)
        }
    }

    fun addClip(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentType = if (text.startsWith("http://") || text.startsWith("https://")) "LINK"
            else if (text.contains("#") || text.contains("```") || text.contains("- ")) "MARKDOWN"
            else "TEXT"

            repository.insertClip(
                ClipEntity(
                    content = text,
                    contentType = contentType,
                    charCount = text.length
                )
            )
        }
    }

    fun deleteClip(clip: ClipEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteClip(clip)
        }
    }

    fun clearUnpinnedClips() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearUnpinnedClips()
        }
    }
}

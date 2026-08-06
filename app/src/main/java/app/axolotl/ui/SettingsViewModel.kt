package app.axolotl.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.axolotl.data.DockSettings
import app.axolotl.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager.getInstance(application)

    val settingsState: StateFlow<DockSettings> = settingsManager.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DockSettings()
    )

    fun updateSettings(newSettings: DockSettings) {
        viewModelScope.launch {
            settingsManager.updateSettings(newSettings)
        }
    }
}

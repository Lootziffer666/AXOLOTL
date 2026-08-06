package app.axolotl.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ControlMode {
    GESTURE, ACTION_BUTTON, MULTI_HANDLE
}

enum class DockEdge {
    LEFT, RIGHT
}

data class DockSettings(
    val edge: DockEdge = DockEdge.LEFT,
    val positionY: Float = 0.5f,
    val dockSize: Float = 1.0f,
    val barOpacity: Float = 0.2f,
    val dockOpacity: Float = 0.9f,
    val iconOpacity: Float = 1.0f,
    val controlMode: ControlMode = ControlMode.GESTURE,
    val buttonX: Int = 0,
    val buttonY: Int = 200,
    val appendixMode: Boolean = false,
    val appendixDraft: String = "",
    val clipboardHistoryEnabled: Boolean = false,
    val privateMode: Boolean = false,
    val emergencyOff: Boolean = false,
    val sensitiveAppMode: Boolean = true,
    val captureDraft: String = ""
)

enum class ClipboardCaptureState {
    IDLE, CAPTURED, BLOCKED, FAILED
}

data class ClipboardCaptureStatus(
    val state: ClipboardCaptureState = ClipboardCaptureState.IDLE,
    val message: String = "Clipboard history is off"
)

class SettingsManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dock_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<DockSettings> = _settings.asStateFlow()
    private val _clipboardCaptureStatus = MutableStateFlow(ClipboardCaptureStatus())
    val clipboardCaptureStatus: StateFlow<ClipboardCaptureStatus> =
        _clipboardCaptureStatus.asStateFlow()

    private fun loadSettings(): DockSettings {
        return DockSettings(
            edge = DockEdge.valueOf(prefs.getString("edge", DockEdge.LEFT.name) ?: DockEdge.LEFT.name),
            positionY = prefs.getFloat("positionY", 0.5f),
            dockSize = prefs.getFloat("dockSize", 1.0f),
            barOpacity = prefs.getFloat("barOpacity", 0.2f),
            dockOpacity = prefs.getFloat("dockOpacity", 0.9f),
            iconOpacity = prefs.getFloat("iconOpacity", 1.0f),
            controlMode = ControlMode.valueOf(prefs.getString("controlMode", ControlMode.GESTURE.name) ?: ControlMode.GESTURE.name),
            buttonX = prefs.getInt("buttonX", 0),
            buttonY = prefs.getInt("buttonY", 200),
            appendixMode = prefs.getBoolean("appendixMode", false),
            appendixDraft = prefs.getString("appendixDraft", "") ?: "",
            clipboardHistoryEnabled = prefs.getBoolean("clipboardHistoryEnabled", false),
            privateMode = prefs.getBoolean("privateMode", false),
            emergencyOff = prefs.getBoolean("emergencyOff", false),
            sensitiveAppMode = prefs.getBoolean("sensitiveAppMode", true),
            captureDraft = prefs.getString("captureDraft", "") ?: ""
        )
    }

    fun updateSettings(newSettings: DockSettings) {
        prefs.edit().apply {
            putString("edge", newSettings.edge.name)
            putFloat("positionY", newSettings.positionY)
            putFloat("dockSize", newSettings.dockSize)
            putFloat("barOpacity", newSettings.barOpacity)
            putFloat("dockOpacity", newSettings.dockOpacity)
            putFloat("iconOpacity", newSettings.iconOpacity)
            putString("controlMode", newSettings.controlMode.name)
            putInt("buttonX", newSettings.buttonX)
            putInt("buttonY", newSettings.buttonY)
            putBoolean("appendixMode", newSettings.appendixMode)
            putString("appendixDraft", newSettings.appendixDraft)
            putBoolean("clipboardHistoryEnabled", newSettings.clipboardHistoryEnabled)
            putBoolean("privateMode", newSettings.privateMode)
            putBoolean("emergencyOff", newSettings.emergencyOff)
            putBoolean("sensitiveAppMode", newSettings.sensitiveAppMode)
            putString("captureDraft", newSettings.captureDraft)
        }.apply()
        _settings.value = newSettings
        if (!newSettings.clipboardHistoryEnabled && !newSettings.appendixMode) {
            _clipboardCaptureStatus.value = ClipboardCaptureStatus()
        } else if (_clipboardCaptureStatus.value.state == ClipboardCaptureState.IDLE) {
            _clipboardCaptureStatus.value = ClipboardCaptureStatus(
                message = "Waiting for clipboard content"
            )
        }
    }

    fun reportClipboardCaptured() {
        _clipboardCaptureStatus.value = ClipboardCaptureStatus(
            ClipboardCaptureState.CAPTURED,
            "Clipboard captured"
        )
    }

    fun reportClipboardBlocked() {
        _clipboardCaptureStatus.value = ClipboardCaptureStatus(
            ClipboardCaptureState.BLOCKED,
            "Clipboard access was blocked by Android"
        )
    }

    fun reportClipboardCaptureFailed() {
        _clipboardCaptureStatus.value = ClipboardCaptureStatus(
            ClipboardCaptureState.FAILED,
            "Clipboard content could not be saved"
        )
    }

    fun appendToAppendix(text: String) {
        val current = _settings.value
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val newDraft = if (current.appendixDraft.isEmpty()) {
            "- $cleanText"
        } else {
            "${current.appendixDraft}\n- $cleanText"
        }
        updateSettings(current.copy(appendixDraft = newDraft))
    }

    fun clearAppendix() {
        val current = _settings.value
        updateSettings(current.copy(appendixDraft = ""))
    }

    fun saveCaptureDraft(draft: String) {
        val current = _settings.value
        updateSettings(current.copy(captureDraft = draft))
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.BuiltInStylePreset
import com.example.model.ButtonEventRecord
import com.example.model.CustomPreset
import com.example.model.DeviceHardwareLayout
import com.example.model.HardwareButtonType
import com.example.service.BezelAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class StudioTab(val label: String) {
    STUDIO("Studio"),
    BUTTONS("Buttons"),
    STYLE("Style"),
    PHYSICS("Physics")
}

data class BezelUiState(
    val config: BezelConfig = BezelConfig(),
    val selectedTab: StudioTab = StudioTab.STUDIO,
    val selectedButton: HardwareButtonType = HardwareButtonType.VOLUME_UP,
    val isServiceEnabled: Boolean = false,
    val isCalibrationMode: Boolean = false,
    val customPresets: List<CustomPreset> = emptyList(),
    val lastEvent: ButtonEventRecord? = null,
    val showSavePresetDialog: Boolean = false,
    val activePresetName: String? = null
)

class BezelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BezelPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(BezelUiState())
    val uiState: StateFlow<BezelUiState> = _uiState.asStateFlow()

    private var saveDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            repository.configFlow.collect { saved ->
                BezelRuntimeBus.updateConfig(saved)
                _uiState.update { it.copy(config = saved) }
            }
        }
        viewModelScope.launch {
            repository.customPresetsFlow.collect { presets ->
                _uiState.update { it.copy(customPresets = presets) }
            }
        }
        viewModelScope.launch {
            BezelRuntimeBus.serviceConnected.collect { connected ->
                refreshServiceStatus(connectedOverride = connected)
            }
        }
        viewModelScope.launch {
            BezelRuntimeBus.lastEvent.collect { event ->
                _uiState.update { it.copy(lastEvent = event) }
            }
        }
        refreshServiceStatus()
    }

    fun refreshServiceStatus(connectedOverride: Boolean? = null) {
        val enabledInSettings = isAccessibilityServiceEnabledInSettings()
        val isActive = (connectedOverride ?: BezelRuntimeBus.serviceConnected.value) || enabledInSettings
        _uiState.update { it.copy(isServiceEnabled = isActive) }
    }

    private fun isAccessibilityServiceEnabledInSettings(): Boolean {
        return runCatching {
            val context = getApplication<Application>()
            val expectedComponent = ComponentName(context, BezelAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.split(':').any { entry ->
                ComponentName.unflattenFromString(entry) == expectedComponent
            }
        }.getOrDefault(false)
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectButtonForEdit(type: HardwareButtonType) {
        _uiState.update { it.copy(selectedButton = type) }
    }

    fun setCalibrationMode(enabled: Boolean) {
        _uiState.update { it.copy(isCalibrationMode = enabled) }
    }

    fun setShowSavePresetDialog(show: Boolean) {
        _uiState.update { it.copy(showSavePresetDialog = show) }
    }

    fun updateConfig(clearActivePreset: Boolean = true, transform: (BezelConfig) -> BezelConfig) {
        val updated = transform(_uiState.value.config)
        BezelRuntimeBus.updateConfig(updated)
        _uiState.update {
            it.copy(
                config = updated,
                activePresetName = if (clearActivePreset) null else it.activePresetName
            )
        }
        schedulePersist(updated)
    }

    fun updateButtonPosition(type: HardwareButtonType, fraction: Float) {
        val clamped = fraction.coerceIn(0.06f, 0.94f)
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(type) { it.copy(verticalPositionFraction = clamped) }
        }
    }

    fun updateButtonSide(type: HardwareButtonType, side: BezelSide) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(type) { it.copy(side = side) }
        }
        triggerButtonPulse(type, "Preview")
    }

    fun updateButtonLength(type: HardwareButtonType, lengthDp: Float) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(type) { it.copy(lengthDp = lengthDp.coerceIn(36f, 140f)) }
        }
    }

    fun updateButtonEnabled(type: HardwareButtonType, enabled: Boolean) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(type) { it.copy(enabled = enabled) }
        }
    }

    fun applyDeviceLayout(layout: DeviceHardwareLayout) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.copy(
                volumeUp = layout.volumeUp,
                volumeDown = layout.volumeDown,
                power = layout.power
            )
        }
        triggerCascadeDemo()
    }

    fun applyBuiltInPreset(preset: BuiltInStylePreset) {
        val updated = preset.apply(_uiState.value.config)
        BezelRuntimeBus.updateConfig(updated)
        _uiState.update { it.copy(config = updated, activePresetName = preset.name) }
        schedulePersist(updated)
        triggerCascadeDemo()
    }

    fun applyCustomPreset(preset: CustomPreset) {
        val updated = preset.applyTo(_uiState.value.config)
        BezelRuntimeBus.updateConfig(updated)
        _uiState.update { it.copy(config = updated, activePresetName = preset.name) }
        schedulePersist(updated)
        triggerCascadeDemo()
    }

    fun saveCurrentAsCustomPreset(name: String) {
        val trimmed = name.trim().ifEmpty { "Custom Style" }
        val cfg = _uiState.value.config
        val newPreset = CustomPreset(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            style = cfg.style,
            maxDepthDp = cfg.maxDepthDp,
            shoulderSpread = cfg.shoulderSpread,
            peakFlatness = cfg.peakFlatness,
            fillColorArgb = cfg.fillColorArgb,
            fillOpacity = cfg.fillOpacity,
            rimHighlightEnabled = cfg.rimHighlightEnabled,
            rimColorArgb = cfg.rimColorArgb,
            rimWidthDp = cfg.rimWidthDp,
            rimGlowRadiusDp = cfg.rimGlowRadiusDp,
            rimOpacity = cfg.rimOpacity,
            showMicroGlyph = cfg.showMicroGlyph,
            stiffness = cfg.stiffness,
            dampingRatio = cfg.dampingRatio,
            holdBoostMultiplier = cfg.holdBoostMultiplier,
            minPressHoldMs = cfg.minPressHoldMs
        )
        val updatedList = listOf(newPreset) + _uiState.value.customPresets
        _uiState.update {
            it.copy(
                customPresets = updatedList,
                showSavePresetDialog = false,
                activePresetName = trimmed
            )
        }
        viewModelScope.launch {
            repository.saveCustomPresets(updatedList)
        }
    }

    fun deleteCustomPreset(id: String) {
        val updatedList = _uiState.value.customPresets.filterNot { it.id == id }
        _uiState.update { it.copy(customPresets = updatedList) }
        viewModelScope.launch {
            repository.saveCustomPresets(updatedList)
        }
    }

    fun resetToDefaults() {
        val defaults = BezelConfig()
        BezelRuntimeBus.updateConfig(defaults)
        _uiState.update { it.copy(config = defaults, activePresetName = "Default") }
        schedulePersist(defaults)
        triggerCascadeDemo()
    }

    fun setStyle(style: BezelStyle) {
        updateConfig { it.copy(style = style) }
        triggerButtonPulse(_uiState.value.selectedButton, "Style Preview")
    }

    fun onSimulatedPress(type: HardwareButtonType, isDown: Boolean) {
        BezelRuntimeBus.dispatchButtonEvent(
            type = type,
            isDown = isDown,
            sourceLabel = "Studio Pad"
        )
        if (isDown) {
            BezelRuntimeBus.triggerHaptic(getApplication(), isPress = true)
        }
    }

    fun triggerButtonPulse(type: HardwareButtonType, source: String = "Preview") {
        BezelRuntimeBus.dispatchButtonEvent(
            type = type,
            isDown = true,
            sourceLabel = source,
            isPulseOnly = true
        )
        BezelRuntimeBus.triggerHaptic(getApplication(), isPress = true)
    }

    fun triggerCascadeDemo() {
        viewModelScope.launch {
            val enabledButtons = _uiState.value.config.allButtons().filter { it.enabled }
            for (btn in enabledButtons) {
                BezelRuntimeBus.dispatchButtonEvent(
                    type = btn.type,
                    isDown = true,
                    sourceLabel = "Preset Demo",
                    isPulseOnly = true
                )
                delay(110L)
            }
        }
    }

    private fun schedulePersist(config: BezelConfig) {
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            delay(180L)
            repository.saveConfig(config)
        }
    }
}

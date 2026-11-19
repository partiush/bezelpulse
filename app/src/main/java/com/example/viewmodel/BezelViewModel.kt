package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.BuiltInStylePreset
import com.example.model.ButtonConfig
import com.example.model.ButtonEventRecord
import com.example.model.ButtonIds
import com.example.model.CustomPreset
import com.example.model.DeviceHardwareLayout
import com.example.service.BezelAccessibilityService
import com.example.service.BezelKeepAliveService
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
    val selectedButtonId: String = ButtonIds.VOLUME_UP,
    val isServiceEnabled: Boolean = false,
    val isKeepAliveRunning: Boolean = false,
    val isBatteryUnrestricted: Boolean = false,
    val isCalibrationMode: Boolean = false,
    val customPresets: List<CustomPreset> = emptyList(),
    val lastEvent: ButtonEventRecord? = null,
    val showSavePresetDialog: Boolean = false,
    val showAddButtonDialog: Boolean = false,
    val activePresetName: String? = null
) {
    val selectedButton: ButtonConfig
        get() = config.buttonById(selectedButtonId) ?: config.buttons.first()
}

class BezelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BezelPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(BezelUiState())
    val uiState: StateFlow<BezelUiState> = _uiState.asStateFlow()

    private var saveDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            repository.configFlow.collect { saved ->
                BezelRuntimeBus.updateConfig(saved)
                _uiState.update { state ->
                    val validSelectedId = if (saved.buttonById(state.selectedButtonId) != null) {
                        state.selectedButtonId
                    } else {
                        saved.buttons.firstOrNull()?.id ?: ButtonIds.VOLUME_UP
                    }
                    state.copy(config = saved, selectedButtonId = validSelectedId)
                }
                syncKeepAliveService(saved)
            }
        }
        viewModelScope.launch {
            repository.customPresetsFlow.collect { presets ->
                _uiState.update { it.copy(customPresets = presets) }
            }
        }
        viewModelScope.launch {
            BezelRuntimeBus.serviceConnected.collect { connected ->
                refreshSystemStatus(connectedOverride = connected)
            }
        }
        viewModelScope.launch {
            BezelRuntimeBus.keepAliveRunning.collect { running ->
                _uiState.update { it.copy(isKeepAliveRunning = running) }
            }
        }
        viewModelScope.launch {
            BezelRuntimeBus.lastEvent.collect { event ->
                _uiState.update { it.copy(lastEvent = event) }
            }
        }
        refreshSystemStatus()
    }

    fun refreshSystemStatus(connectedOverride: Boolean? = null) {
        val enabledInSettings = isAccessibilityServiceEnabledInSettings()
        val isActive = (connectedOverride ?: BezelRuntimeBus.serviceConnected.value) || enabledInSettings
        val batteryUnrestricted = checkBatteryUnrestricted()
        _uiState.update {
            it.copy(
                isServiceEnabled = isActive,
                isKeepAliveRunning = BezelRuntimeBus.keepAliveRunning.value,
                isBatteryUnrestricted = batteryUnrestricted
            )
        }
        syncKeepAliveService(_uiState.value.config)
    }

    private fun syncKeepAliveService(config: BezelConfig) {
        val app = getApplication<Application>()
        if (config.masterEnabled && config.keepAliveForeground) {
            BezelKeepAliveService.start(app)
        } else {
            BezelKeepAliveService.stop(app)
        }
    }

    private fun checkBatteryUnrestricted(): Boolean {
        return com.example.util.OemSupport.isIgnoringBatteryOptimizations(getApplication())
    }

    fun exportConfigJson(): String {
        return com.example.data.BezelConfigCodec.exportConfigJson(_uiState.value.config)
    }

    fun importConfigJson(rawJson: String): Result<Unit> {
        val result = com.example.data.BezelConfigCodec.importConfigJson(rawJson, _uiState.value.config)
        return result.map { imported ->
            BezelRuntimeBus.updateConfig(imported)
            _uiState.update { state ->
                val validSelected = if (imported.buttonById(state.selectedButtonId) != null) {
                    state.selectedButtonId
                } else {
                    imported.buttons.firstOrNull()?.id ?: ButtonIds.VOLUME_UP
                }
                state.copy(
                    config = imported,
                    selectedButtonId = validSelected,
                    activePresetName = "Imported"
                )
            }
            syncKeepAliveService(imported)
            schedulePersist(imported)
            triggerCascadeDemo()
        }
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

    fun selectButtonForEdit(buttonId: String) {
        _uiState.update { it.copy(selectedButtonId = buttonId) }
    }

    fun setCalibrationMode(enabled: Boolean) {
        _uiState.update { it.copy(isCalibrationMode = enabled) }
    }

    fun setShowSavePresetDialog(show: Boolean) {
        _uiState.update { it.copy(showSavePresetDialog = show) }
    }

    fun setShowAddButtonDialog(show: Boolean) {
        _uiState.update { it.copy(showAddButtonDialog = show) }
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
        syncKeepAliveService(updated)
        schedulePersist(updated)
    }

    fun addCustomButton(
        name: String,
        keyCode: Int,
        glyph: String,
        side: BezelSide,
        accentColorArgb: Int
    ) {
        val cleanName = name.trim().ifEmpty { "Custom Key" }
        val shortLabel = cleanName.take(7)
        val newId = "custom_${UUID.randomUUID().toString().take(8)}"

        // Find a clean non-overlapping initial vertical position on the chosen side
        val existingOnSide = _uiState.value.config.buttons.filter { it.side == side }
        val defaultPos = when {
            existingOnSide.isEmpty() -> 0.32f
            else -> ((existingOnSide.maxOf { it.verticalPositionFraction } + 0.12f)
                .takeIf { it <= 0.84f } ?: 0.22f)
        }

        val newButton = ButtonConfig(
            id = newId,
            name = cleanName,
            shortLabel = shortLabel,
            glyph = glyph,
            keyCode = keyCode,
            isBuiltIn = false,
            accentColorArgb = accentColorArgb,
            enabled = true,
            side = side,
            verticalPositionFraction = defaultPos,
            lengthDp = 58f
        )

        updateConfig(clearActivePreset = false) { it.addButton(newButton) }
        _uiState.update {
            it.copy(
                selectedButtonId = newId,
                showAddButtonDialog = false
            )
        }
        triggerButtonPulse(newId, "New Button")
    }

    fun deleteCustomButton(buttonId: String) {
        val updated = _uiState.value.config.removeButton(buttonId)
        BezelRuntimeBus.updateConfig(updated)
        _uiState.update { state ->
            val nextSelected = if (state.selectedButtonId == buttonId) {
                updated.buttons.firstOrNull()?.id ?: ButtonIds.VOLUME_UP
            } else {
                state.selectedButtonId
            }
            state.copy(config = updated, selectedButtonId = nextSelected)
        }
        schedulePersist(updated)
    }

    fun updateButtonPosition(buttonId: String, fraction: Float) {
        val clamped = fraction.coerceIn(0.05f, 0.95f)
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(buttonId) { it.copy(verticalPositionFraction = clamped) }
        }
    }

    fun updateButtonSide(buttonId: String, side: BezelSide) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(buttonId) { it.copy(side = side) }
        }
        triggerButtonPulse(buttonId, "Preview")
    }

    fun updateButtonLength(buttonId: String, lengthDp: Float) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(buttonId) { it.copy(lengthDp = lengthDp.coerceIn(36f, 140f)) }
        }
    }

    fun updateButtonEnabled(buttonId: String, enabled: Boolean) {
        updateConfig(clearActivePreset = false) { cfg ->
            cfg.updateButton(buttonId) { it.copy(enabled = enabled) }
        }
    }

    fun applyDeviceLayout(layout: DeviceHardwareLayout) {
        updateConfig(clearActivePreset = false) { cfg -> layout.applyTo(cfg) }
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
        _uiState.update {
            it.copy(
                config = defaults,
                selectedButtonId = ButtonIds.VOLUME_UP,
                activePresetName = "Default"
            )
        }
        syncKeepAliveService(defaults)
        schedulePersist(defaults)
        triggerCascadeDemo()
    }

    fun setStyle(style: BezelStyle) {
        updateConfig { it.copy(style = style) }
        triggerButtonPulse(_uiState.value.selectedButtonId, "Style Preview")
    }

    fun onSimulatedPress(buttonId: String, isDown: Boolean) {
        BezelRuntimeBus.dispatchButtonEvent(
            buttonId = buttonId,
            isDown = isDown,
            sourceLabel = "Studio Pad"
        )
        if (isDown) {
            BezelRuntimeBus.triggerHaptic(getApplication(), isPress = true)
        }
    }

    fun triggerButtonPulse(buttonId: String, source: String = "Preview") {
        BezelRuntimeBus.dispatchButtonEvent(
            buttonId = buttonId,
            isDown = true,
            sourceLabel = source,
            isPulseOnly = true
        )
        BezelRuntimeBus.triggerHaptic(getApplication(), isPress = true)
    }

    fun triggerCascadeDemo() {
        viewModelScope.launch {
            val enabledButtons = _uiState.value.config.buttons.filter { it.enabled }
            for (btn in enabledButtons) {
                BezelRuntimeBus.dispatchButtonEvent(
                    buttonId = btn.id,
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

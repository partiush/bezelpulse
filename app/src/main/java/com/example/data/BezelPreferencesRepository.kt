package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.BezelConfig
import com.example.model.BezelStyle
import com.example.model.CustomPreset
import com.example.model.defaultBuiltInButtons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bezelDataStore: DataStore<Preferences> by preferencesDataStore(name = "bezel_pulse_prefs")

class BezelPreferencesRepository(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val KEEP_ALIVE_FGS = booleanPreferencesKey("keep_alive_fgs")
        val AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        val POWER_WAKE_PULSE = booleanPreferencesKey("power_wake_pulse")
        val PAUSE_IN_LANDSCAPE = booleanPreferencesKey("pause_in_landscape")
        val STYLE = stringPreferencesKey("style")
        val MAX_DEPTH_DP = floatPreferencesKey("max_depth_dp")
        val SHOULDER_SPREAD = floatPreferencesKey("shoulder_spread")
        val PEAK_FLATNESS = floatPreferencesKey("peak_flatness")
        val FILL_COLOR = intPreferencesKey("fill_color")
        val FILL_OPACITY = floatPreferencesKey("fill_opacity")
        val RIM_ENABLED = booleanPreferencesKey("rim_enabled")
        val RIM_COLOR = intPreferencesKey("rim_color")
        val RIM_WIDTH_DP = floatPreferencesKey("rim_width_dp")
        val RIM_GLOW_DP = floatPreferencesKey("rim_glow_dp")
        val RIM_OPACITY = floatPreferencesKey("rim_opacity")
        val SHOW_GLYPH = booleanPreferencesKey("show_glyph")
        val STIFFNESS = floatPreferencesKey("stiffness")
        val DAMPING_RATIO = floatPreferencesKey("damping_ratio")
        val HOLD_BOOST = floatPreferencesKey("hold_boost")
        val MIN_HOLD_MS = longPreferencesKey("min_hold_ms")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAPTIC_STRENGTH = floatPreferencesKey("haptic_strength")
        val BUTTONS_JSON = stringPreferencesKey("buttons_json")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
    }

    val configFlow: Flow<BezelConfig> = context.bezelDataStore.data.map { prefs ->
        val defaults = BezelConfig()
        val parsedButtons = prefs[Keys.BUTTONS_JSON]?.let { BezelConfigCodec.decodeButtons(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultBuiltInButtons()

        BezelConfig(
            masterEnabled = prefs[Keys.MASTER_ENABLED] ?: defaults.masterEnabled,
            keepAliveForeground = prefs[Keys.KEEP_ALIVE_FGS] ?: defaults.keepAliveForeground,
            autoStartOnBoot = prefs[Keys.AUTO_START_BOOT] ?: defaults.autoStartOnBoot,
            powerWakePulseEnabled = prefs[Keys.POWER_WAKE_PULSE] ?: defaults.powerWakePulseEnabled,
            pauseInLandscape = prefs[Keys.PAUSE_IN_LANDSCAPE] ?: defaults.pauseInLandscape,
            style = prefs[Keys.STYLE]?.let { runCatching { BezelStyle.valueOf(it) }.getOrNull() } ?: defaults.style,
            maxDepthDp = prefs[Keys.MAX_DEPTH_DP] ?: defaults.maxDepthDp,
            shoulderSpread = prefs[Keys.SHOULDER_SPREAD] ?: defaults.shoulderSpread,
            peakFlatness = prefs[Keys.PEAK_FLATNESS] ?: defaults.peakFlatness,
            fillColorArgb = prefs[Keys.FILL_COLOR] ?: defaults.fillColorArgb,
            fillOpacity = prefs[Keys.FILL_OPACITY] ?: defaults.fillOpacity,
            rimHighlightEnabled = prefs[Keys.RIM_ENABLED] ?: defaults.rimHighlightEnabled,
            rimColorArgb = prefs[Keys.RIM_COLOR] ?: defaults.rimColorArgb,
            rimWidthDp = prefs[Keys.RIM_WIDTH_DP] ?: defaults.rimWidthDp,
            rimGlowRadiusDp = prefs[Keys.RIM_GLOW_DP] ?: defaults.rimGlowRadiusDp,
            rimOpacity = prefs[Keys.RIM_OPACITY] ?: defaults.rimOpacity,
            showMicroGlyph = prefs[Keys.SHOW_GLYPH] ?: defaults.showMicroGlyph,
            stiffness = prefs[Keys.STIFFNESS] ?: defaults.stiffness,
            dampingRatio = prefs[Keys.DAMPING_RATIO] ?: defaults.dampingRatio,
            holdBoostMultiplier = prefs[Keys.HOLD_BOOST] ?: defaults.holdBoostMultiplier,
            minPressHoldMs = prefs[Keys.MIN_HOLD_MS] ?: defaults.minPressHoldMs,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: defaults.hapticFeedbackEnabled,
            hapticStrength = prefs[Keys.HAPTIC_STRENGTH] ?: defaults.hapticStrength,
            buttons = parsedButtons
        )
    }

    val customPresetsFlow: Flow<List<CustomPreset>> = context.bezelDataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_PRESETS_JSON] ?: return@map emptyList()
        BezelConfigCodec.decodePresets(raw)
    }

    suspend fun saveConfig(config: BezelConfig) {
        context.bezelDataStore.edit { prefs ->
            prefs[Keys.MASTER_ENABLED] = config.masterEnabled
            prefs[Keys.KEEP_ALIVE_FGS] = config.keepAliveForeground
            prefs[Keys.AUTO_START_BOOT] = config.autoStartOnBoot
            prefs[Keys.POWER_WAKE_PULSE] = config.powerWakePulseEnabled
            prefs[Keys.PAUSE_IN_LANDSCAPE] = config.pauseInLandscape
            prefs[Keys.STYLE] = config.style.name
            prefs[Keys.MAX_DEPTH_DP] = config.maxDepthDp
            prefs[Keys.SHOULDER_SPREAD] = config.shoulderSpread
            prefs[Keys.PEAK_FLATNESS] = config.peakFlatness
            prefs[Keys.FILL_COLOR] = config.fillColorArgb
            prefs[Keys.FILL_OPACITY] = config.fillOpacity
            prefs[Keys.RIM_ENABLED] = config.rimHighlightEnabled
            prefs[Keys.RIM_COLOR] = config.rimColorArgb
            prefs[Keys.RIM_WIDTH_DP] = config.rimWidthDp
            prefs[Keys.RIM_GLOW_DP] = config.rimGlowRadiusDp
            prefs[Keys.RIM_OPACITY] = config.rimOpacity
            prefs[Keys.SHOW_GLYPH] = config.showMicroGlyph
            prefs[Keys.STIFFNESS] = config.stiffness
            prefs[Keys.DAMPING_RATIO] = config.dampingRatio
            prefs[Keys.HOLD_BOOST] = config.holdBoostMultiplier
            prefs[Keys.MIN_HOLD_MS] = config.minPressHoldMs
            prefs[Keys.HAPTIC_ENABLED] = config.hapticFeedbackEnabled
            prefs[Keys.HAPTIC_STRENGTH] = config.hapticStrength
            prefs[Keys.BUTTONS_JSON] = BezelConfigCodec.encodeButtons(config.buttons)
        }
    }

    suspend fun saveCustomPresets(presets: List<CustomPreset>) {
        context.bezelDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PRESETS_JSON] = BezelConfigCodec.encodePresets(presets)
        }
    }
}

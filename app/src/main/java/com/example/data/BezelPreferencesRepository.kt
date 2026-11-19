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
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.ButtonConfig
import com.example.model.CustomPreset
import com.example.model.HardwareButtonType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.bezelDataStore: DataStore<Preferences> by preferencesDataStore(name = "bezel_pulse_prefs")

class BezelPreferencesRepository(private val context: Context) {

    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
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

        val VOL_UP_ENABLED = booleanPreferencesKey("vol_up_enabled")
        val VOL_UP_SIDE = stringPreferencesKey("vol_up_side")
        val VOL_UP_POS = floatPreferencesKey("vol_up_pos")
        val VOL_UP_LEN = floatPreferencesKey("vol_up_len")

        val VOL_DOWN_ENABLED = booleanPreferencesKey("vol_down_enabled")
        val VOL_DOWN_SIDE = stringPreferencesKey("vol_down_side")
        val VOL_DOWN_POS = floatPreferencesKey("vol_down_pos")
        val VOL_DOWN_LEN = floatPreferencesKey("vol_down_len")

        val POWER_ENABLED = booleanPreferencesKey("power_enabled")
        val POWER_SIDE = stringPreferencesKey("power_side")
        val POWER_POS = floatPreferencesKey("power_pos")
        val POWER_LEN = floatPreferencesKey("power_len")

        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
    }

    val configFlow: Flow<BezelConfig> = context.bezelDataStore.data.map { prefs ->
        val defaults = BezelConfig()
        BezelConfig(
            masterEnabled = prefs[Keys.MASTER_ENABLED] ?: defaults.masterEnabled,
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
            volumeUp = ButtonConfig(
                type = HardwareButtonType.VOLUME_UP,
                enabled = prefs[Keys.VOL_UP_ENABLED] ?: defaults.volumeUp.enabled,
                side = prefs[Keys.VOL_UP_SIDE]?.let { runCatching { BezelSide.valueOf(it) }.getOrNull() } ?: defaults.volumeUp.side,
                verticalPositionFraction = prefs[Keys.VOL_UP_POS] ?: defaults.volumeUp.verticalPositionFraction,
                lengthDp = prefs[Keys.VOL_UP_LEN] ?: defaults.volumeUp.lengthDp
            ),
            volumeDown = ButtonConfig(
                type = HardwareButtonType.VOLUME_DOWN,
                enabled = prefs[Keys.VOL_DOWN_ENABLED] ?: defaults.volumeDown.enabled,
                side = prefs[Keys.VOL_DOWN_SIDE]?.let { runCatching { BezelSide.valueOf(it) }.getOrNull() } ?: defaults.volumeDown.side,
                verticalPositionFraction = prefs[Keys.VOL_DOWN_POS] ?: defaults.volumeDown.verticalPositionFraction,
                lengthDp = prefs[Keys.VOL_DOWN_LEN] ?: defaults.volumeDown.lengthDp
            ),
            power = ButtonConfig(
                type = HardwareButtonType.POWER,
                enabled = prefs[Keys.POWER_ENABLED] ?: defaults.power.enabled,
                side = prefs[Keys.POWER_SIDE]?.let { runCatching { BezelSide.valueOf(it) }.getOrNull() } ?: defaults.power.side,
                verticalPositionFraction = prefs[Keys.POWER_POS] ?: defaults.power.verticalPositionFraction,
                lengthDp = prefs[Keys.POWER_LEN] ?: defaults.power.lengthDp
            )
        )
    }

    val customPresetsFlow: Flow<List<CustomPreset>> = context.bezelDataStore.data.map { prefs ->
        val raw = prefs[Keys.CUSTOM_PRESETS_JSON] ?: return@map emptyList()
        decodePresets(raw)
    }

    suspend fun saveConfig(config: BezelConfig) {
        context.bezelDataStore.edit { prefs ->
            prefs[Keys.MASTER_ENABLED] = config.masterEnabled
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

            prefs[Keys.VOL_UP_ENABLED] = config.volumeUp.enabled
            prefs[Keys.VOL_UP_SIDE] = config.volumeUp.side.name
            prefs[Keys.VOL_UP_POS] = config.volumeUp.verticalPositionFraction
            prefs[Keys.VOL_UP_LEN] = config.volumeUp.lengthDp

            prefs[Keys.VOL_DOWN_ENABLED] = config.volumeDown.enabled
            prefs[Keys.VOL_DOWN_SIDE] = config.volumeDown.side.name
            prefs[Keys.VOL_DOWN_POS] = config.volumeDown.verticalPositionFraction
            prefs[Keys.VOL_DOWN_LEN] = config.volumeDown.lengthDp

            prefs[Keys.POWER_ENABLED] = config.power.enabled
            prefs[Keys.POWER_SIDE] = config.power.side.name
            prefs[Keys.POWER_POS] = config.power.verticalPositionFraction
            prefs[Keys.POWER_LEN] = config.power.lengthDp
        }
    }

    suspend fun saveCustomPresets(presets: List<CustomPreset>) {
        context.bezelDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PRESETS_JSON] = encodePresets(presets)
        }
    }

    private fun encodePresets(presets: List<CustomPreset>): String {
        val arr = JSONArray()
        for (p in presets) {
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("style", p.style.name)
                    put("maxDepthDp", p.maxDepthDp.toDouble())
                    put("shoulderSpread", p.shoulderSpread.toDouble())
                    put("peakFlatness", p.peakFlatness.toDouble())
                    put("fillColorArgb", p.fillColorArgb)
                    put("fillOpacity", p.fillOpacity.toDouble())
                    put("rimHighlightEnabled", p.rimHighlightEnabled)
                    put("rimColorArgb", p.rimColorArgb)
                    put("rimWidthDp", p.rimWidthDp.toDouble())
                    put("rimGlowRadiusDp", p.rimGlowRadiusDp.toDouble())
                    put("rimOpacity", p.rimOpacity.toDouble())
                    put("showMicroGlyph", p.showMicroGlyph)
                    put("stiffness", p.stiffness.toDouble())
                    put("dampingRatio", p.dampingRatio.toDouble())
                    put("holdBoostMultiplier", p.holdBoostMultiplier.toDouble())
                    put("minPressHoldMs", p.minPressHoldMs)
                }
            )
        }
        return arr.toString()
    }

    private fun decodePresets(json: String): List<CustomPreset> = runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val style = runCatching { BezelStyle.valueOf(obj.getString("style")) }
                    .getOrDefault(BezelStyle.LIQUID_BULGE)
                add(
                    CustomPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        style = style,
                        maxDepthDp = obj.getDouble("maxDepthDp").toFloat(),
                        shoulderSpread = obj.getDouble("shoulderSpread").toFloat(),
                        peakFlatness = obj.getDouble("peakFlatness").toFloat(),
                        fillColorArgb = obj.getInt("fillColorArgb"),
                        fillOpacity = obj.getDouble("fillOpacity").toFloat(),
                        rimHighlightEnabled = obj.getBoolean("rimHighlightEnabled"),
                        rimColorArgb = obj.getInt("rimColorArgb"),
                        rimWidthDp = obj.getDouble("rimWidthDp").toFloat(),
                        rimGlowRadiusDp = obj.getDouble("rimGlowRadiusDp").toFloat(),
                        rimOpacity = obj.getDouble("rimOpacity").toFloat(),
                        showMicroGlyph = obj.getBoolean("showMicroGlyph"),
                        stiffness = obj.getDouble("stiffness").toFloat(),
                        dampingRatio = obj.getDouble("dampingRatio").toFloat(),
                        holdBoostMultiplier = obj.getDouble("holdBoostMultiplier").toFloat(),
                        minPressHoldMs = obj.getLong("minPressHoldMs")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

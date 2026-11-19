package com.example.data

import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.ButtonConfig
import com.example.model.CustomPreset
import com.example.model.defaultBuiltInButtons
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON serializer for buttons, custom presets, and shareable BezelPulse config profiles.
 */
object BezelConfigCodec {

    fun encodeButtons(buttons: List<ButtonConfig>): String {
        val arr = JSONArray()
        for (b in buttons) {
            arr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("shortLabel", b.shortLabel)
                    put("glyph", b.glyph)
                    put("keyCode", b.keyCode)
                    put("isBuiltIn", b.isBuiltIn)
                    put("accentColorArgb", b.accentColorArgb)
                    put("enabled", b.enabled)
                    put("side", b.side.name)
                    put("verticalPositionFraction", b.verticalPositionFraction.toDouble())
                    put("lengthDp", b.lengthDp.toDouble())
                }
            )
        }
        return arr.toString()
    }

    fun decodeButtons(json: String): List<ButtonConfig> = runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val side = runCatching { BezelSide.valueOf(obj.getString("side")) }
                    .getOrDefault(BezelSide.RIGHT)
                add(
                    ButtonConfig(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        shortLabel = obj.getString("shortLabel"),
                        glyph = obj.getString("glyph"),
                        keyCode = obj.getInt("keyCode"),
                        isBuiltIn = obj.optBoolean("isBuiltIn", false),
                        accentColorArgb = obj.optInt("accentColorArgb", 0xFF38BDF8.toInt()),
                        enabled = obj.optBoolean("enabled", true),
                        side = side,
                        verticalPositionFraction = obj.optDouble("verticalPositionFraction", 0.35).toFloat()
                            .coerceIn(0.05f, 0.95f),
                        lengthDp = obj.optDouble("lengthDp", 62.0).toFloat().coerceIn(36f, 140f)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun encodePresets(presets: List<CustomPreset>): String {
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

    fun decodePresets(json: String): List<CustomPreset> = runCatching {
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

    fun exportConfigJson(config: BezelConfig): String {
        val root = JSONObject().apply {
            put("schemaVersion", 1)
            put("style", config.style.name)
            put("maxDepthDp", config.maxDepthDp.toDouble())
            put("shoulderSpread", config.shoulderSpread.toDouble())
            put("peakFlatness", config.peakFlatness.toDouble())
            put("fillColorArgb", config.fillColorArgb)
            put("fillOpacity", config.fillOpacity.toDouble())
            put("rimHighlightEnabled", config.rimHighlightEnabled)
            put("rimColorArgb", config.rimColorArgb)
            put("rimWidthDp", config.rimWidthDp.toDouble())
            put("rimGlowRadiusDp", config.rimGlowRadiusDp.toDouble())
            put("rimOpacity", config.rimOpacity.toDouble())
            put("showMicroGlyph", config.showMicroGlyph)
            put("stiffness", config.stiffness.toDouble())
            put("dampingRatio", config.dampingRatio.toDouble())
            put("holdBoostMultiplier", config.holdBoostMultiplier.toDouble())
            put("minPressHoldMs", config.minPressHoldMs)
            put("hapticFeedbackEnabled", config.hapticFeedbackEnabled)
            put("hapticStrength", config.hapticStrength.toDouble())
            put("pauseInLandscape", config.pauseInLandscape)
            put("buttons", JSONArray(encodeButtons(config.buttons)))
        }
        return root.toString(2)
    }

    fun importConfigJson(rawJson: String, base: BezelConfig): Result<BezelConfig> = runCatching {
        val obj = JSONObject(rawJson.trim())
        val style = obj.optString("style", base.style.name).let {
            runCatching { BezelStyle.valueOf(it) }.getOrDefault(base.style)
        }
        val importedButtons = obj.optJSONArray("buttons")?.let {
            decodeButtons(it.toString()).takeIf { list -> list.isNotEmpty() }
        } ?: base.buttons.ifEmpty { defaultBuiltInButtons() }

        base.copy(
            style = style,
            maxDepthDp = obj.optDouble("maxDepthDp", base.maxDepthDp.toDouble()).toFloat().coerceIn(6f, 36f),
            shoulderSpread = obj.optDouble("shoulderSpread", base.shoulderSpread.toDouble()).toFloat().coerceIn(0.2f, 1.0f),
            peakFlatness = obj.optDouble("peakFlatness", base.peakFlatness.toDouble()).toFloat().coerceIn(0f, 0.85f),
            fillColorArgb = obj.optInt("fillColorArgb", base.fillColorArgb),
            fillOpacity = obj.optDouble("fillOpacity", base.fillOpacity.toDouble()).toFloat().coerceIn(0.15f, 1.0f),
            rimHighlightEnabled = obj.optBoolean("rimHighlightEnabled", base.rimHighlightEnabled),
            rimColorArgb = obj.optInt("rimColorArgb", base.rimColorArgb),
            rimWidthDp = obj.optDouble("rimWidthDp", base.rimWidthDp.toDouble()).toFloat().coerceIn(0.5f, 4.5f),
            rimGlowRadiusDp = obj.optDouble("rimGlowRadiusDp", base.rimGlowRadiusDp.toDouble()).toFloat().coerceIn(0f, 24f),
            rimOpacity = obj.optDouble("rimOpacity", base.rimOpacity.toDouble()).toFloat().coerceIn(0.05f, 1.0f),
            showMicroGlyph = obj.optBoolean("showMicroGlyph", base.showMicroGlyph),
            stiffness = obj.optDouble("stiffness", base.stiffness.toDouble()).toFloat().coerceIn(180f, 1800f),
            dampingRatio = obj.optDouble("dampingRatio", base.dampingRatio.toDouble()).toFloat().coerceIn(0.35f, 1.1f),
            holdBoostMultiplier = obj.optDouble("holdBoostMultiplier", base.holdBoostMultiplier.toDouble()).toFloat().coerceIn(1.0f, 1.6f),
            minPressHoldMs = obj.optLong("minPressHoldMs", base.minPressHoldMs).coerceIn(50L, 250L),
            hapticFeedbackEnabled = obj.optBoolean("hapticFeedbackEnabled", base.hapticFeedbackEnabled),
            hapticStrength = obj.optDouble("hapticStrength", base.hapticStrength.toDouble()).toFloat().coerceIn(0.2f, 1.0f),
            pauseInLandscape = obj.optBoolean("pauseInLandscape", base.pauseInLandscape),
            buttons = importedButtons
        )
    }
}

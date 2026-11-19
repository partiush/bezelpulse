package com.example.model

import android.view.KeyEvent

object ButtonIds {
    const val VOLUME_UP = "volume_up"
    const val VOLUME_DOWN = "volume_down"
    const val POWER = "power"
}

enum class BezelSide(val label: String) {
    LEFT("Left"),
    RIGHT("Right")
}

enum class BezelStyle(val label: String, val subtitle: String) {
    LIQUID_BULGE("iOS Liquid", "Seamless organic bezel flex"),
    TACTILE_PILL("Tactile Pill", "Floating edge capsule"),
    EDGE_GLOW("Laser Arc", "Ultra-thin luminous rim"),
    MAGNETIC_WAVE("Ripple Wave", "Broad fluid bezel wave")
}

data class HardwareKeyPreset(
    val label: String,
    val shortLabel: String,
    val keyCode: Int,
    val defaultGlyph: String
)

object HardwareKeyCatalog {
    val presets = listOf(
        HardwareKeyPreset("Camera Shutter", "Cam", KeyEvent.KEYCODE_CAMERA, "◎"),
        HardwareKeyPreset("AI / Assist Key", "AI", KeyEvent.KEYCODE_ASSIST, "✦"),
        HardwareKeyPreset("Voice Assist", "Voice", KeyEvent.KEYCODE_VOICE_ASSIST, "◉"),
        HardwareKeyPreset("Camera Focus", "Focus", KeyEvent.KEYCODE_FOCUS, "○"),
        HardwareKeyPreset("Action / Mute", "Mute", KeyEvent.KEYCODE_VOLUME_MUTE, "⚡"),
        HardwareKeyPreset("Headset / Media", "Media", KeyEvent.KEYCODE_HEADSETHOOK, "♪"),
        HardwareKeyPreset("Trigger L1", "L1", KeyEvent.KEYCODE_BUTTON_L1, "L"),
        HardwareKeyPreset("Trigger R1", "R1", KeyEvent.KEYCODE_BUTTON_R1, "R"),
        HardwareKeyPreset("Power / Side", "Power", KeyEvent.KEYCODE_POWER, "⏻"),
        HardwareKeyPreset("Volume Up", "Vol +", KeyEvent.KEYCODE_VOLUME_UP, "+"),
        HardwareKeyPreset("Volume Down", "Vol −", KeyEvent.KEYCODE_VOLUME_DOWN, "−")
    )

    val customGlyphs = listOf("⚡", "◎", "✦", "★", "♪", "●", "▲", "▼", "L", "R", "+", "−", "⏻")

    fun keyCodeDisplayName(keyCode: Int): String {
        val raw = KeyEvent.keyCodeToString(keyCode)
        return raw.removePrefix("KEYCODE_").replace('_', ' ')
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}

data class ButtonConfig(
    val id: String,
    val name: String,
    val shortLabel: String,
    val glyph: String,
    val keyCode: Int,
    val isBuiltIn: Boolean = false,
    val accentColorArgb: Int = 0xFF38BDF8.toInt(),
    val enabled: Boolean = true,
    val side: BezelSide = BezelSide.RIGHT,
    val verticalPositionFraction: Float = 0.35f,
    val lengthDp: Float = 62f
)

fun defaultBuiltInButtons(): List<ButtonConfig> = listOf(
    ButtonConfig(
        id = ButtonIds.VOLUME_UP,
        name = "Volume Up",
        shortLabel = "Vol +",
        glyph = "+",
        keyCode = KeyEvent.KEYCODE_VOLUME_UP,
        isBuiltIn = true,
        accentColorArgb = 0xFF38BDF8.toInt(),
        enabled = true,
        side = BezelSide.RIGHT,
        verticalPositionFraction = 0.34f,
        lengthDp = 62f
    ),
    ButtonConfig(
        id = ButtonIds.VOLUME_DOWN,
        name = "Volume Down",
        shortLabel = "Vol −",
        glyph = "−",
        keyCode = KeyEvent.KEYCODE_VOLUME_DOWN,
        isBuiltIn = true,
        accentColorArgb = 0xFF10B981.toInt(),
        enabled = true,
        side = BezelSide.RIGHT,
        verticalPositionFraction = 0.44f,
        lengthDp = 62f
    ),
    ButtonConfig(
        id = ButtonIds.POWER,
        name = "Power / Side",
        shortLabel = "Power",
        glyph = "⏻",
        keyCode = KeyEvent.KEYCODE_POWER,
        isBuiltIn = true,
        accentColorArgb = 0xFFF59E0B.toInt(),
        enabled = true,
        side = BezelSide.RIGHT,
        verticalPositionFraction = 0.56f,
        lengthDp = 58f
    )
)

data class BezelConfig(
    val masterEnabled: Boolean = true,
    val keepAliveForeground: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val powerWakePulseEnabled: Boolean = false,
    val pauseInLandscape: Boolean = true,
    val style: BezelStyle = BezelStyle.LIQUID_BULGE,
    val maxDepthDp: Float = 16f,
    val shoulderSpread: Float = 0.58f,
    val peakFlatness: Float = 0.32f,
    val fillColorArgb: Int = 0xFF000000.toInt(),
    val fillOpacity: Float = 1.0f,
    val rimHighlightEnabled: Boolean = true,
    val rimColorArgb: Int = 0xFF38BDF8.toInt(),
    val rimWidthDp: Float = 1.6f,
    val rimGlowRadiusDp: Float = 10f,
    val rimOpacity: Float = 0.55f,
    val showMicroGlyph: Boolean = false,
    val stiffness: Float = 760f,
    val dampingRatio: Float = 0.90f,
    val holdBoostMultiplier: Float = 1.24f,
    val minPressHoldMs: Long = 115L,
    val hapticFeedbackEnabled: Boolean = true,
    val hapticStrength: Float = 0.72f,
    val buttons: List<ButtonConfig> = defaultBuiltInButtons()
) {
    fun buttonById(id: String): ButtonConfig? = buttons.firstOrNull { it.id == id }

    fun buttonsForKeyCode(keyCode: Int): List<ButtonConfig> =
        buttons.filter { it.enabled && it.keyCode == keyCode }

    fun updateButton(id: String, transform: (ButtonConfig) -> ButtonConfig): BezelConfig =
        copy(buttons = buttons.map { if (it.id == id) transform(it) else it })

    fun addButton(button: ButtonConfig): BezelConfig =
        copy(buttons = buttons + button)

    fun removeButton(id: String): BezelConfig =
        copy(buttons = buttons.filterNot { it.id == id && !it.isBuiltIn })
}

data class DeviceHardwareLayout(
    val id: String,
    val name: String,
    val subtitle: String,
    val volUpSide: BezelSide,
    val volUpPos: Float,
    val volUpLen: Float,
    val volDownSide: BezelSide,
    val volDownPos: Float,
    val volDownLen: Float,
    val powerSide: BezelSide,
    val powerPos: Float,
    val powerLen: Float
) {
    fun applyTo(config: BezelConfig): BezelConfig {
        return config.copy(
            buttons = config.buttons.map { btn ->
                when (btn.id) {
                    ButtonIds.VOLUME_UP -> btn.copy(
                        side = volUpSide,
                        verticalPositionFraction = volUpPos,
                        lengthDp = volUpLen
                    )
                    ButtonIds.VOLUME_DOWN -> btn.copy(
                        side = volDownSide,
                        verticalPositionFraction = volDownPos,
                        lengthDp = volDownLen
                    )
                    ButtonIds.POWER -> btn.copy(
                        side = powerSide,
                        verticalPositionFraction = powerPos,
                        lengthDp = powerLen
                    )
                    else -> btn
                }
            }
        )
    }
}

data class BuiltInStylePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val accentArgb: Int,
    val fillArgb: Int,
    val apply: (BezelConfig) -> BezelConfig
)

data class CustomPreset(
    val id: String,
    val name: String,
    val style: BezelStyle,
    val maxDepthDp: Float,
    val shoulderSpread: Float,
    val peakFlatness: Float,
    val fillColorArgb: Int,
    val fillOpacity: Float,
    val rimHighlightEnabled: Boolean,
    val rimColorArgb: Int,
    val rimWidthDp: Float,
    val rimGlowRadiusDp: Float,
    val rimOpacity: Float,
    val showMicroGlyph: Boolean,
    val stiffness: Float,
    val dampingRatio: Float,
    val holdBoostMultiplier: Float,
    val minPressHoldMs: Long
) {
    fun applyTo(base: BezelConfig): BezelConfig = base.copy(
        style = style,
        maxDepthDp = maxDepthDp,
        shoulderSpread = shoulderSpread,
        peakFlatness = peakFlatness,
        fillColorArgb = fillColorArgb,
        fillOpacity = fillOpacity,
        rimHighlightEnabled = rimHighlightEnabled,
        rimColorArgb = rimColorArgb,
        rimWidthDp = rimWidthDp,
        rimGlowRadiusDp = rimGlowRadiusDp,
        rimOpacity = rimOpacity,
        showMicroGlyph = showMicroGlyph,
        stiffness = stiffness,
        dampingRatio = dampingRatio,
        holdBoostMultiplier = holdBoostMultiplier,
        minPressHoldMs = minPressHoldMs
    )
}

data class ButtonEventRecord(
    val buttonId: String,
    val buttonLabel: String,
    val isDown: Boolean,
    val timestampMs: Long,
    val sourceLabel: String
)

object BezelPresetsCatalog {
    val deviceLayouts = listOf(
        DeviceHardwareLayout(
            id = "pixel",
            name = "Google Pixel",
            subtitle = "Power top-right, Volume below",
            powerSide = BezelSide.RIGHT, powerPos = 0.25f, powerLen = 56f,
            volUpSide = BezelSide.RIGHT, volUpPos = 0.36f, volUpLen = 60f,
            volDownSide = BezelSide.RIGHT, volDownPos = 0.46f, volDownLen = 60f
        ),
        DeviceHardwareLayout(
            id = "samsung",
            name = "Samsung Galaxy",
            subtitle = "Volume top-right, Power below",
            volUpSide = BezelSide.RIGHT, volUpPos = 0.23f, volUpLen = 64f,
            volDownSide = BezelSide.RIGHT, volDownPos = 0.33f, volDownLen = 64f,
            powerSide = BezelSide.RIGHT, powerPos = 0.45f, powerLen = 56f
        ),
        DeviceHardwareLayout(
            id = "xiaomi",
            name = "Xiaomi / Redmi",
            subtitle = "Volume top-right, Power mid-right",
            volUpSide = BezelSide.RIGHT, volUpPos = 0.28f, volUpLen = 62f,
            volDownSide = BezelSide.RIGHT, volDownPos = 0.38f, volDownLen = 62f,
            powerSide = BezelSide.RIGHT, powerPos = 0.50f, powerLen = 58f
        ),
        DeviceHardwareLayout(
            id = "oneplus",
            name = "OnePlus / Nothing",
            subtitle = "Volume left, Power right mid",
            volUpSide = BezelSide.LEFT, volUpPos = 0.29f, volUpLen = 62f,
            volDownSide = BezelSide.LEFT, volDownPos = 0.39f, volDownLen = 62f,
            powerSide = BezelSide.RIGHT, powerPos = 0.36f, powerLen = 60f
        ),
        DeviceHardwareLayout(
            id = "iphone",
            name = "iPhone Layout",
            subtitle = "Volume on left, Side on right",
            volUpSide = BezelSide.LEFT, volUpPos = 0.27f, volUpLen = 62f,
            volDownSide = BezelSide.LEFT, volDownPos = 0.37f, volDownLen = 62f,
            powerSide = BezelSide.RIGHT, powerPos = 0.31f, powerLen = 76f
        )
    )

    val stylePresets = listOf(
        BuiltInStylePreset(
            id = "ios_native",
            name = "iOS 18 Native",
            subtitle = "Pure pitch-black liquid flex",
            accentArgb = 0xFFE2E8F0.toInt(),
            fillArgb = 0xFF000000.toInt(),
            apply = { cfg ->
                cfg.copy(
                    style = BezelStyle.LIQUID_BULGE,
                    maxDepthDp = 15f,
                    shoulderSpread = 0.62f,
                    peakFlatness = 0.36f,
                    fillColorArgb = 0xFF000000.toInt(),
                    fillOpacity = 1.0f,
                    rimHighlightEnabled = true,
                    rimColorArgb = 0xFFFFFFFF.toInt(),
                    rimWidthDp = 1.0f,
                    rimGlowRadiusDp = 4f,
                    rimOpacity = 0.22f,
                    showMicroGlyph = false,
                    stiffness = 860f,
                    dampingRatio = 0.96f,
                    holdBoostMultiplier = 1.22f,
                    minPressHoldMs = 110L
                )
            }
        ),
        BuiltInStylePreset(
            id = "titanium_glow",
            name = "Titanium Cyan",
            subtitle = "Obsidian core + electric rim",
            accentArgb = 0xFF38BDF8.toInt(),
            fillArgb = 0xFF05070C.toInt(),
            apply = { cfg ->
                cfg.copy(
                    style = BezelStyle.LIQUID_BULGE,
                    maxDepthDp = 19f,
                    shoulderSpread = 0.58f,
                    peakFlatness = 0.30f,
                    fillColorArgb = 0xFF05070C.toInt(),
                    fillOpacity = 0.98f,
                    rimHighlightEnabled = true,
                    rimColorArgb = 0xFF38BDF8.toInt(),
                    rimWidthDp = 1.8f,
                    rimGlowRadiusDp = 14f,
                    rimOpacity = 0.80f,
                    showMicroGlyph = false,
                    stiffness = 700f,
                    dampingRatio = 0.82f,
                    holdBoostMultiplier = 1.28f,
                    minPressHoldMs = 125L
                )
            }
        ),
        BuiltInStylePreset(
            id = "bouncy_plasma",
            name = "Bouncy Plasma",
            subtitle = "Playful underdamped coral wave",
            accentArgb = 0xFFFB7185.toInt(),
            fillArgb = 0xFF1A0912.toInt(),
            apply = { cfg ->
                cfg.copy(
                    style = BezelStyle.MAGNETIC_WAVE,
                    maxDepthDp = 24f,
                    shoulderSpread = 0.78f,
                    peakFlatness = 0.18f,
                    fillColorArgb = 0xFF1A0912.toInt(),
                    fillOpacity = 0.94f,
                    rimHighlightEnabled = true,
                    rimColorArgb = 0xFFFB7185.toInt(),
                    rimWidthDp = 2.2f,
                    rimGlowRadiusDp = 18f,
                    rimOpacity = 0.92f,
                    showMicroGlyph = true,
                    stiffness = 460f,
                    dampingRatio = 0.50f,
                    holdBoostMultiplier = 1.38f,
                    minPressHoldMs = 145L
                )
            }
        ),
        BuiltInStylePreset(
            id = "stealth_laser",
            name = "Laser Edge",
            subtitle = "Razor-thin glowing rim arc",
            accentArgb = 0xFFF59E0B.toInt(),
            fillArgb = 0xFF000000.toInt(),
            apply = { cfg ->
                cfg.copy(
                    style = BezelStyle.EDGE_GLOW,
                    maxDepthDp = 12f,
                    shoulderSpread = 0.45f,
                    peakFlatness = 0.42f,
                    fillColorArgb = 0xFF000000.toInt(),
                    fillOpacity = 0.50f,
                    rimHighlightEnabled = true,
                    rimColorArgb = 0xFFF59E0B.toInt(),
                    rimWidthDp = 2.6f,
                    rimGlowRadiusDp = 16f,
                    rimOpacity = 0.95f,
                    showMicroGlyph = false,
                    stiffness = 1050f,
                    dampingRatio = 0.94f,
                    holdBoostMultiplier = 1.18f,
                    minPressHoldMs = 100L
                )
            }
        ),
        BuiltInStylePreset(
            id = "tactile_pill",
            name = "Emerald Pill",
            subtitle = "Floating tactile capsule + icon",
            accentArgb = 0xFF10B981.toInt(),
            fillArgb = 0xFF091A15.toInt(),
            apply = { cfg ->
                cfg.copy(
                    style = BezelStyle.TACTILE_PILL,
                    maxDepthDp = 22f,
                    shoulderSpread = 0.35f,
                    peakFlatness = 0.55f,
                    fillColorArgb = 0xFF091A15.toInt(),
                    fillOpacity = 0.96f,
                    rimHighlightEnabled = true,
                    rimColorArgb = 0xFF10B981.toInt(),
                    rimWidthDp = 1.8f,
                    rimGlowRadiusDp = 12f,
                    rimOpacity = 0.85f,
                    showMicroGlyph = true,
                    stiffness = 620f,
                    dampingRatio = 0.72f,
                    holdBoostMultiplier = 1.25f,
                    minPressHoldMs = 130L
                )
            }
        )
    )
}

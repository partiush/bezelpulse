package com.example.model

enum class HardwareButtonType(val label: String, val shortLabel: String, val glyph: String) {
    VOLUME_UP("Volume Up", "Vol +", "+"),
    VOLUME_DOWN("Volume Down", "Vol −", "−"),
    POWER("Power / Side", "Power", "⏻")
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

data class ButtonConfig(
    val type: HardwareButtonType,
    val enabled: Boolean = true,
    val side: BezelSide = BezelSide.RIGHT,
    val verticalPositionFraction: Float = 0.30f,
    val lengthDp: Float = 66f
)

data class BezelConfig(
    val masterEnabled: Boolean = true,
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
    val volumeUp: ButtonConfig = ButtonConfig(
        type = HardwareButtonType.VOLUME_UP,
        enabled = true,
        side = BezelSide.RIGHT,
        verticalPositionFraction = 0.36f,
        lengthDp = 62f
    ),
    val volumeDown: ButtonConfig = ButtonConfig(
        type = HardwareButtonType.VOLUME_DOWN,
        enabled = true,
        side = BezelSide.RIGHT,
        verticalPositionFraction = 0.46f,
        lengthDp = 62f
    ),
    val power: ButtonConfig = ButtonConfig(
        type = HardwareButtonType.POWER,
        enabled = true,
        side = BezelSide.RIGHT,
        verticalPositionFraction = 0.25f,
        lengthDp = 58f
    )
) {
    fun buttonConfig(type: HardwareButtonType): ButtonConfig = when (type) {
        HardwareButtonType.VOLUME_UP -> volumeUp
        HardwareButtonType.VOLUME_DOWN -> volumeDown
        HardwareButtonType.POWER -> power
    }

    fun updateButton(type: HardwareButtonType, transform: (ButtonConfig) -> ButtonConfig): BezelConfig =
        when (type) {
            HardwareButtonType.VOLUME_UP -> copy(volumeUp = transform(volumeUp))
            HardwareButtonType.VOLUME_DOWN -> copy(volumeDown = transform(volumeDown))
            HardwareButtonType.POWER -> copy(power = transform(power))
        }

    fun allButtons(): List<ButtonConfig> = listOf(volumeUp, volumeDown, power)
}

data class DeviceHardwareLayout(
    val id: String,
    val name: String,
    val subtitle: String,
    val volumeUp: ButtonConfig,
    val volumeDown: ButtonConfig,
    val power: ButtonConfig
)

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
    val button: HardwareButtonType,
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
            power = ButtonConfig(HardwareButtonType.POWER, true, BezelSide.RIGHT, 0.25f, 56f),
            volumeUp = ButtonConfig(HardwareButtonType.VOLUME_UP, true, BezelSide.RIGHT, 0.36f, 60f),
            volumeDown = ButtonConfig(HardwareButtonType.VOLUME_DOWN, true, BezelSide.RIGHT, 0.46f, 60f)
        ),
        DeviceHardwareLayout(
            id = "samsung",
            name = "Samsung Galaxy",
            subtitle = "Volume top-right, Power below",
            volumeUp = ButtonConfig(HardwareButtonType.VOLUME_UP, true, BezelSide.RIGHT, 0.23f, 64f),
            volumeDown = ButtonConfig(HardwareButtonType.VOLUME_DOWN, true, BezelSide.RIGHT, 0.33f, 64f),
            power = ButtonConfig(HardwareButtonType.POWER, true, BezelSide.RIGHT, 0.45f, 56f)
        ),
        DeviceHardwareLayout(
            id = "iphone",
            name = "iPhone Layout",
            subtitle = "Volume on left, Side on right",
            volumeUp = ButtonConfig(HardwareButtonType.VOLUME_UP, true, BezelSide.LEFT, 0.27f, 62f),
            volumeDown = ButtonConfig(HardwareButtonType.VOLUME_DOWN, true, BezelSide.LEFT, 0.37f, 62f),
            power = ButtonConfig(HardwareButtonType.POWER, true, BezelSide.RIGHT, 0.31f, 76f)
        ),
        DeviceHardwareLayout(
            id = "oneplus",
            name = "OnePlus / Nothing",
            subtitle = "Volume left, Power right mid",
            volumeUp = ButtonConfig(HardwareButtonType.VOLUME_UP, true, BezelSide.LEFT, 0.29f, 62f),
            volumeDown = ButtonConfig(HardwareButtonType.VOLUME_DOWN, true, BezelSide.LEFT, 0.39f, 62f),
            power = ButtonConfig(HardwareButtonType.POWER, true, BezelSide.RIGHT, 0.36f, 60f)
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

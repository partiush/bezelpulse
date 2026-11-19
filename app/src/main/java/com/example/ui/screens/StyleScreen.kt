package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BezelConfig
import com.example.model.BezelStyle
import com.example.ui.components.BezelStyleMiniPreview
import com.example.ui.components.ColorPalettePicker
import com.example.ui.components.PrecisionSliderRow
import com.example.ui.components.PrecisionToggleRow
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCardHighlight
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import com.example.viewmodel.BezelUiState
import kotlin.math.roundToInt

@Composable
fun StyleScreen(
    uiState: BezelUiState,
    onSelectStyle: (BezelStyle) -> Unit,
    onUpdateConfig: ((BezelConfig) -> BezelConfig) -> Unit,
    onPreviewPulse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.config

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Curve Shape Architecture (2x2 Visual Cards)
        item {
            StudioSectionCard(
                title = "Bezel Curve Architecture",
                subtitle = "Choose how the screen edge deforms when a button is pressed"
            ) {
                val styles = BezelStyle.entries
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (rowIdx in 0 until styles.size step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (colOffset in 0..1) {
                                val style = styles.getOrNull(rowIdx + colOffset)
                                if (style != null) {
                                    val isSelected = cfg.style == style
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onSelectStyle(style) }
                                            .testTag("style_${style.name.lowercase()}"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) ObsidianCardHighlight else ObsidianElevated,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) TitaniumCyan else HairlineBorder
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            BezelStyleMiniPreview(
                                                style = style,
                                                fillArgb = cfg.fillColorArgb,
                                                rimArgb = cfg.rimColorArgb,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(62.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            Text(
                                                text = style.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) TitaniumCyan else TextPrimary
                                            )
                                            Text(
                                                text = style.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Curve Geometry & Proportions
        item {
            StudioSectionCard(
                title = "Geometry & Sculpting",
                subtitle = "Release any slider to preview live on your screen edge"
            ) {
                PrecisionSliderRow(
                    label = "Protrusion Depth",
                    valueText = "${cfg.maxDepthDp.roundToInt()} dp",
                    value = cfg.maxDepthDp,
                    valueRange = 6f..36f,
                    onValueChange = { v -> onUpdateConfig { it.copy(maxDepthDp = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "How deep the bezel bulges onto the display",
                    testTagName = "depth_slider"
                )
                Spacer(Modifier.height(6.dp))
                PrecisionSliderRow(
                    label = "Shoulder Liquid Spread",
                    valueText = "${(cfg.shoulderSpread * 100).roundToInt()}%",
                    value = cfg.shoulderSpread,
                    valueRange = 0.2f..1.0f,
                    onValueChange = { v -> onUpdateConfig { it.copy(shoulderSpread = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "Controls how smoothly the curve melts into the bezel",
                    testTagName = "spread_slider"
                )
                Spacer(Modifier.height(6.dp))
                PrecisionSliderRow(
                    label = "Crown Plateau",
                    valueText = "${(cfg.peakFlatness * 100).roundToInt()}%",
                    value = cfg.peakFlatness,
                    valueRange = 0.0f..0.85f,
                    onValueChange = { v -> onUpdateConfig { it.copy(peakFlatness = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "Rounded droplet (0%) vs flat iOS rocker peak (85%)",
                    testTagName = "flatness_slider"
                )
            }
        }

        // 3. Fill Color & Opacity
        item {
            StudioSectionCard(
                title = "Body Material & Color",
                subtitle = "Use Pitch Obsidian (#000000) for authentic iOS hardware bezel fusion"
            ) {
                ColorPalettePicker(
                    title = "Body Fill Color",
                    selectedArgb = cfg.fillColorArgb,
                    onColorSelected = { argb -> onUpdateConfig { it.copy(fillColorArgb = argb) } },
                    onColorFinished = onPreviewPulse
                )
                Spacer(Modifier.height(14.dp))
                PrecisionSliderRow(
                    label = "Body Opacity",
                    valueText = "${(cfg.fillOpacity * 100).roundToInt()}%",
                    value = cfg.fillOpacity,
                    valueRange = 0.15f..1.0f,
                    onValueChange = { v -> onUpdateConfig { it.copy(fillOpacity = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    testTagName = "fill_opacity_slider"
                )
            }
        }

        // 4. Luminous Edge Rim & Micro-Glyph
        item {
            StudioSectionCard(
                title = "Edge Rim, Halo & Micro-Glyph",
                subtitle = "Add a crisp glass specular highlight or ambient neon aura"
            ) {
                PrecisionToggleRow(
                    title = "Edge Rim Highlight",
                    subtitle = "Draws a glowing contour along the flexing bezel curve",
                    checked = cfg.rimHighlightEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateConfig { it.copy(rimHighlightEnabled = enabled) }
                        onPreviewPulse()
                    },
                    testTagName = "rim_highlight_switch"
                )

                AnimatedVisibility(visible = cfg.rimHighlightEnabled || cfg.style == BezelStyle.EDGE_GLOW) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        ColorPalettePicker(
                            title = "Rim & Halo Color",
                            selectedArgb = cfg.rimColorArgb,
                            onColorSelected = { argb -> onUpdateConfig { it.copy(rimColorArgb = argb) } },
                            onColorFinished = onPreviewPulse
                        )
                        Spacer(Modifier.height(14.dp))
                        PrecisionSliderRow(
                            label = "Rim Stroke Thickness",
                            valueText = String.format("%.1f dp", cfg.rimWidthDp),
                            value = cfg.rimWidthDp,
                            valueRange = 0.5f..4.5f,
                            onValueChange = { v -> onUpdateConfig { it.copy(rimWidthDp = v) } },
                            onValueChangeFinished = onPreviewPulse,
                            testTagName = "rim_width_slider"
                        )
                        PrecisionSliderRow(
                            label = "Luminous Glow Radius",
                            valueText = "${cfg.rimGlowRadiusDp.roundToInt()} dp",
                            value = cfg.rimGlowRadiusDp,
                            valueRange = 0f..24f,
                            onValueChange = { v -> onUpdateConfig { it.copy(rimGlowRadiusDp = v) } },
                            onValueChangeFinished = onPreviewPulse,
                            testTagName = "rim_glow_slider"
                        )
                        PrecisionSliderRow(
                            label = "Rim Intensity",
                            valueText = "${(cfg.rimOpacity * 100).roundToInt()}%",
                            value = cfg.rimOpacity,
                            valueRange = 0.05f..1.0f,
                            onValueChange = { v -> onUpdateConfig { it.copy(rimOpacity = v) } },
                            onValueChangeFinished = onPreviewPulse,
                            testTagName = "rim_opacity_slider"
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                PrecisionToggleRow(
                    title = "Show Micro-Glyph Inside Bulge",
                    subtitle = "Reveals subtle +, −, or power symbol on deep presses",
                    checked = cfg.showMicroGlyph,
                    onCheckedChange = { enabled ->
                        onUpdateConfig { it.copy(showMicroGlyph = enabled) }
                        onPreviewPulse()
                    },
                    testTagName = "micro_glyph_switch"
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

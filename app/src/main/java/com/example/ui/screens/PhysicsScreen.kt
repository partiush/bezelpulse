package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BezelConfig
import com.example.ui.components.PrecisionSliderRow
import com.example.ui.components.PrecisionToggleRow
import com.example.ui.components.SpringCurveGraph
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlasmaCoral
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import com.example.viewmodel.BezelUiState
import kotlin.math.roundToInt

@Composable
fun PhysicsScreen(
    uiState: BezelUiState,
    onUpdateConfig: ((BezelConfig) -> BezelConfig) -> Unit,
    onPreviewPulse: () -> Unit,
    onTriggerCascade: () -> Unit,
    onOpenSavePresetDialog: () -> Unit,
    onDismissSavePresetDialog: () -> Unit,
    onConfirmSavePreset: (String) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.config

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Spring Oscilloscope Graph
        item {
            SpringCurveGraph(
                stiffness = cfg.stiffness,
                dampingRatio = cfg.dampingRatio
            )
        }

        // 2. Spring Dynamics Parameters
        item {
            StudioSectionCard(
                title = "Spring Physics Engine",
                subtitle = "Sub-stepped harmonic oscillator running directly on display VSYNC"
            ) {
                PrecisionSliderRow(
                    label = "Spring Stiffness (k)",
                    valueText = "${cfg.stiffness.roundToInt()} N/m",
                    value = cfg.stiffness,
                    valueRange = 180f..1800f,
                    onValueChange = { v -> onUpdateConfig { it.copy(stiffness = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "Higher values snap faster into the bezel with crisp urgency",
                    testTagName = "stiffness_slider"
                )
                Spacer(Modifier.height(6.dp))
                PrecisionSliderRow(
                    label = "Damping Ratio (ζ)",
                    valueText = String.format("%.2f ζ", cfg.dampingRatio),
                    value = cfg.dampingRatio,
                    valueRange = 0.35f..1.10f,
                    onValueChange = { v -> onUpdateConfig { it.copy(dampingRatio = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "0.95 = zero-bounce iOS native · 0.50 = elastic bouncy spring",
                    testTagName = "damping_slider"
                )
                Spacer(Modifier.height(6.dp))
                PrecisionSliderRow(
                    label = "Hold Sustain Depth Boost",
                    valueText = String.format("%.2fx", cfg.holdBoostMultiplier),
                    value = cfg.holdBoostMultiplier,
                    valueRange = 1.0f..1.60f,
                    onValueChange = { v -> onUpdateConfig { it.copy(holdBoostMultiplier = v) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "Extra bulge swell when holding a volume key continuously",
                    testTagName = "hold_boost_slider"
                )
                Spacer(Modifier.height(6.dp))
                PrecisionSliderRow(
                    label = "Quick-Tap Minimum Hold",
                    valueText = "${cfg.minPressHoldMs} ms",
                    value = cfg.minPressHoldMs.toFloat(),
                    valueRange = 50f..250f,
                    onValueChange = { v -> onUpdateConfig { it.copy(minPressHoldMs = v.roundToInt().toLong()) } },
                    onValueChangeFinished = onPreviewPulse,
                    hint = "Ensures ultra-rapid clicks still render a full fluid pulse",
                    testTagName = "min_hold_slider"
                )

                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onTriggerCascade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("physics_cascade_preview_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TitaniumCyan.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = TitaniumCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Feel Spring Cascade on Bezel",
                        style = MaterialTheme.typography.labelLarge,
                        color = TitaniumCyan
                    )
                }
            }
        }

        // 3. Synchronized Tactile Haptics
        item {
            StudioSectionCard(
                title = "Synchronized Haptics",
                subtitle = "Pairs physical button press with a crisp micro-tick impulse"
            ) {
                PrecisionToggleRow(
                    title = "Enable Tactile Tick",
                    subtitle = "Fires a sharp sub-12ms haptic pulse synced with the spring launch",
                    checked = cfg.hapticFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateConfig { it.copy(hapticFeedbackEnabled = enabled) }
                        if (enabled) onPreviewPulse()
                    },
                    testTagName = "haptics_toggle_switch"
                )

                AnimatedVisibility(visible = cfg.hapticFeedbackEnabled) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        PrecisionSliderRow(
                            label = "Haptic Impulse Strength",
                            valueText = "${(cfg.hapticStrength * 100).roundToInt()}%",
                            value = cfg.hapticStrength,
                            valueRange = 0.2f..1.0f,
                            onValueChange = { v -> onUpdateConfig { it.copy(hapticStrength = v) } },
                            onValueChangeFinished = onPreviewPulse,
                            testTagName = "haptic_strength_slider"
                        )
                    }
                }
            }
        }

        // 4. Save Custom Preset / Reset Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenSavePresetDialog,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_preset_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TitaniumCyan,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save_preset),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("reset_defaults_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PlasmaCoral.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = PlasmaCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.reset_defaults),
                        style = MaterialTheme.typography.labelLarge,
                        color = PlasmaCoral
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (uiState.showSavePresetDialog) {
        SavePresetDialog(
            onDismiss = onDismissSavePresetDialog,
            onSave = onConfirmSavePreset
        )
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Save Custom Preset",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "Store your current curve geometry, colors, and spring physics for one-tap switching.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it.take(28) },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. Midnight Liquid") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TitaniumCyan,
                        unfocusedBorderColor = HairlineBorder,
                        focusedLabelColor = TitaniumCyan,
                        cursorColor = TitaniumCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(presetName) },
                modifier = Modifier.testTag("confirm_save_preset_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TitaniumCyan,
                    contentColor = ObsidianVoid
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

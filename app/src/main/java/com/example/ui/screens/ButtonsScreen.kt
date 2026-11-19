package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BezelPresetsCatalog
import com.example.model.BezelSide
import com.example.model.DeviceHardwareLayout
import com.example.model.HardwareButtonType
import com.example.ui.components.PrecisionSliderRow
import com.example.ui.components.PrecisionToggleRow
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.TactileHoldButton
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCardHighlight
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import com.example.ui.theme.TitaniumCyanContainer
import com.example.viewmodel.BezelUiState
import kotlin.math.roundToInt

@Composable
fun ButtonsScreen(
    uiState: BezelUiState,
    onSelectButton: (HardwareButtonType) -> Unit,
    onApplyDeviceLayout: (DeviceHardwareLayout) -> Unit,
    onOpenCalibration: () -> Unit,
    onToggleEnabled: (HardwareButtonType, Boolean) -> Unit,
    onSelectSide: (HardwareButtonType, BezelSide) -> Unit,
    onPositionChange: (HardwareButtonType, Float) -> Unit,
    onLengthChange: (HardwareButtonType, Float) -> Unit,
    onTriggerPulse: (HardwareButtonType) -> Unit,
    onSimulatedPress: (HardwareButtonType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBtnConfig = uiState.config.buttonConfig(uiState.selectedButton)
    val accent = when (uiState.selectedButton) {
        HardwareButtonType.VOLUME_UP -> TitaniumCyan
        HardwareButtonType.VOLUME_DOWN -> EmeraldActive
        HardwareButtonType.POWER -> SignalAmber
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Interactive Screen-Edge Calibration CTA
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = TitaniumCyanContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, TitaniumCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Straighten,
                            contentDescription = null,
                            tint = TitaniumCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Millimeter Edge Alignment",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Drag handles directly along your phone bezel to match your exact physical hardware keys.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onOpenCalibration,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_calibration_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TitaniumCyan,
                            contentColor = ObsidianVoid
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.calibrate_edges),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // 2. Quick Device Chassis Presets
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Phone Hardware Layouts",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(BezelPresetsCatalog.deviceLayouts, key = { it.id }) { layout ->
                        Surface(
                            modifier = Modifier
                                .width(164.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onApplyDeviceLayout(layout) }
                                .testTag("device_layout_${layout.id}"),
                            shape = RoundedCornerShape(18.dp),
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, HairlineBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneAndroid,
                                    contentDescription = null,
                                    tint = TitaniumCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = layout.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = layout.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Per-Button Selector & Fine Tuning
        item {
            StudioSectionCard(
                title = "Per-Button Calibration",
                subtitle = "Configure side, vertical placement, and rocker length independently"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HardwareButtonType.entries.forEach { type ->
                        val isSelected = uiState.selectedButton == type
                        val chipColor = when (type) {
                            HardwareButtonType.VOLUME_UP -> TitaniumCyan
                            HardwareButtonType.VOLUME_DOWN -> EmeraldActive
                            HardwareButtonType.POWER -> SignalAmber
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectButton(type)
                                    onTriggerPulse(type)
                                }
                                .testTag("select_button_${type.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) chipColor.copy(alpha = 0.2f) else ObsidianElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipColor else HairlineBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = type.shortLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) chipColor else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                PrecisionToggleRow(
                    title = "Animate ${selectedBtnConfig.type.label}",
                    subtitle = "Show bezel flex when this hardware button is clicked",
                    checked = selectedBtnConfig.enabled,
                    onCheckedChange = { onToggleEnabled(selectedBtnConfig.type, it) },
                    testTagName = "button_enabled_switch"
                )

                Spacer(Modifier.height(12.dp))

                // Bezel Edge Side Selector
                Text(
                    text = "Physical Bezel Edge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BezelSide.entries.forEach { side ->
                        val isSelected = selectedBtnConfig.side == side
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectSide(selectedBtnConfig.type, side) }
                                .testTag("side_${side.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ObsidianCardHighlight else ObsidianElevated,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) accent else HairlineBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${side.label} Edge",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) accent else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                PrecisionSliderRow(
                    label = "Vertical Position",
                    valueText = "${(selectedBtnConfig.verticalPositionFraction * 100).roundToInt()}%",
                    value = selectedBtnConfig.verticalPositionFraction,
                    valueRange = 0.06f..0.94f,
                    onValueChange = { onPositionChange(selectedBtnConfig.type, it) },
                    onValueChangeFinished = { onTriggerPulse(selectedBtnConfig.type) },
                    accentColor = accent,
                    testTagName = "button_position_slider"
                )

                Spacer(Modifier.height(8.dp))

                PrecisionSliderRow(
                    label = "Button Rocker Span",
                    valueText = "${selectedBtnConfig.lengthDp.roundToInt()} dp",
                    value = selectedBtnConfig.lengthDp,
                    valueRange = 36f..140f,
                    onValueChange = { onLengthChange(selectedBtnConfig.type, it) },
                    onValueChangeFinished = { onTriggerPulse(selectedBtnConfig.type) },
                    accentColor = accent,
                    testTagName = "button_length_slider"
                )

                Spacer(Modifier.height(12.dp))

                val icon = when (selectedBtnConfig.type) {
                    HardwareButtonType.VOLUME_UP -> Icons.AutoMirrored.Rounded.VolumeUp
                    HardwareButtonType.VOLUME_DOWN -> Icons.AutoMirrored.Rounded.VolumeDown
                    HardwareButtonType.POWER -> Icons.Rounded.PowerSettingsNew
                }

                TactileHoldButton(
                    label = "Press & Hold to Test ${selectedBtnConfig.type.label}",
                    icon = icon,
                    accentColor = accent,
                    onPressChange = { down -> onSimulatedPress(selectedBtnConfig.type, down) },
                    testTagName = "test_selected_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

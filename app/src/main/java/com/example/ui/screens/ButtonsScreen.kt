package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BezelPresetsCatalog
import com.example.model.BezelSide
import com.example.model.ButtonIds
import com.example.model.DeviceHardwareLayout
import com.example.model.HardwareKeyCatalog
import com.example.ui.components.PrecisionSliderRow
import com.example.ui.components.PrecisionToggleRow
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.TactileHoldButton
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCardHighlight
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlasmaCoral
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
    onSelectButton: (String) -> Unit,
    onOpenAddButton: () -> Unit,
    onDeleteCustomButton: (String) -> Unit,
    onApplyDeviceLayout: (DeviceHardwareLayout) -> Unit,
    onOpenCalibration: () -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onSelectSide: (String, BezelSide) -> Unit,
    onPositionChange: (String, Float) -> Unit,
    onLengthChange: (String, Float) -> Unit,
    onTriggerPulse: (String) -> Unit,
    onSimulatedPress: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBtnConfig = uiState.selectedButton
    val accent = Color(selectedBtnConfig.accentColorArgb)

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
                                text = "Drag handles smoothly along your screen bezel with live curve preview and 1% fine-tune controls.",
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

        // Device chassis presets
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
                                .width(168.dp)
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

        // 3. Per-Button Selector & Fine Tuning + Add New Button
        item {
            StudioSectionCard(
                title = "Configured Hardware Buttons",
                subtitle = "Select any button to customize or add extra hardware keys",
                trailing = {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenAddButton)
                            .testTag("add_new_button_cta"),
                        shape = RoundedCornerShape(12.dp),
                        color = TitaniumCyanContainer,
                        border = BorderStroke(1.dp, TitaniumCyan.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.add_new_button),
                                tint = TitaniumCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelMedium,
                                color = TitaniumCyan
                            )
                        }
                    }
                }
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.config.buttons, key = { it.id }) { btn ->
                        val isSelected = selectedBtnConfig.id == btn.id
                        val chipColor = Color(btn.accentColorArgb)
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectButton(btn.id)
                                    onTriggerPulse(btn.id)
                                }
                                .testTag("select_button_${btn.id}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) chipColor.copy(alpha = 0.2f) else ObsidianElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipColor else HairlineBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = btn.shortLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) chipColor else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Mapped Android KeyCode telemetry pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mapped Hardware Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${HardwareKeyCatalog.keyCodeDisplayName(selectedBtnConfig.keyCode)} (#${selectedBtnConfig.keyCode})",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }

                if (selectedBtnConfig.id == ButtonIds.POWER || selectedBtnConfig.keyCode == KeyEvent.KEYCODE_POWER) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SignalAmber.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = SignalAmber,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Direct Power presses and long-press Power menus trigger press and sustained hold animations. You can also enable Screen Wake & Lock Animation in the Studio tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                PrecisionToggleRow(
                    title = "Animate ${selectedBtnConfig.name}",
                    subtitle = "Show bezel flex when this hardware button is clicked",
                    checked = selectedBtnConfig.enabled,
                    onCheckedChange = { onToggleEnabled(selectedBtnConfig.id, it) },
                    testTagName = "button_enabled_switch"
                )

                Spacer(Modifier.height(12.dp))

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
                                .clickable { onSelectSide(selectedBtnConfig.id, side) }
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
                    valueRange = 0.05f..0.95f,
                    onValueChange = { onPositionChange(selectedBtnConfig.id, it) },
                    onValueChangeFinished = { onTriggerPulse(selectedBtnConfig.id) },
                    accentColor = accent,
                    testTagName = "button_position_slider"
                )

                Spacer(Modifier.height(8.dp))

                PrecisionSliderRow(
                    label = "Button Rocker Span",
                    valueText = "${selectedBtnConfig.lengthDp.roundToInt()} dp",
                    value = selectedBtnConfig.lengthDp,
                    valueRange = 36f..140f,
                    onValueChange = { onLengthChange(selectedBtnConfig.id, it) },
                    onValueChangeFinished = { onTriggerPulse(selectedBtnConfig.id) },
                    accentColor = accent,
                    testTagName = "button_length_slider"
                )

                Spacer(Modifier.height(12.dp))

                val icon = when (selectedBtnConfig.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> Icons.AutoMirrored.Rounded.VolumeUp
                    KeyEvent.KEYCODE_VOLUME_DOWN -> Icons.AutoMirrored.Rounded.VolumeDown
                    KeyEvent.KEYCODE_POWER -> Icons.Rounded.PowerSettingsNew
                    else -> Icons.Rounded.TouchApp
                }

                TactileHoldButton(
                    label = "Press & Hold to Test ${selectedBtnConfig.name}",
                    icon = icon,
                    accentColor = accent,
                    onPressChange = { down -> onSimulatedPress(selectedBtnConfig.id, down) },
                    testTagName = "test_selected_button",
                    modifier = Modifier.fillMaxWidth()
                )

                // Delete action for user-added custom buttons
                if (!selectedBtnConfig.isBuiltIn) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onDeleteCustomButton(selectedBtnConfig.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_custom_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PlasmaCoral.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = PlasmaCoral,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Remove ${selectedBtnConfig.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PlasmaCoral
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

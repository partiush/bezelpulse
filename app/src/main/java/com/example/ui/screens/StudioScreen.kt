package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsAccessibility
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.model.BezelConfig
import com.example.model.BezelPresetsCatalog
import com.example.model.BuiltInStylePreset
import com.example.model.ButtonConfig
import com.example.model.CustomPreset
import com.example.ui.components.PrecisionToggleRow
import com.example.ui.components.StudioSectionCard
import com.example.ui.components.TactileHoldButton
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCardHighlight
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalAmberContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import com.example.ui.theme.TitaniumCyanContainer
import com.example.viewmodel.BezelUiState

@Composable
fun StudioScreen(
    uiState: BezelUiState,
    onMasterToggle: (Boolean) -> Unit,
    onUpdateConfig: ((BezelConfig) -> BezelConfig) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenAddButton: () -> Unit,
    onSimulatedPress: (String, Boolean) -> Unit,
    onApplyBuiltInPreset: (BuiltInStylePreset) -> Unit,
    onApplyCustomPreset: (CustomPreset) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner Card with Master Toggle & Real-Time Status
        item {
            HeroBezelCard(
                masterEnabled = uiState.config.masterEnabled,
                isServiceEnabled = uiState.isServiceEnabled,
                isKeepAliveRunning = uiState.isKeepAliveRunning,
                styleLabel = uiState.config.style.label,
                lastEventText = uiState.lastEvent?.let {
                    "${it.buttonLabel} · ${it.sourceLabel}"
                },
                onMasterToggle = onMasterToggle
            )
        }

        // 2. System-Wide Zero-Latency Service Activation Card
        item {
            SystemOverlayStatusCard(
                isServiceEnabled = uiState.isServiceEnabled,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onOpenCalibration = onOpenCalibration
            )
        }

        // 3. Background Reliability & System Behavior
        item {
            AlwaysOnBackgroundCard(
                config = uiState.config,
                isKeepAliveRunning = uiState.isKeepAliveRunning,
                isBatteryUnrestricted = uiState.isBatteryUnrestricted,
                onToggleKeepAlive = { enabled ->
                    if (enabled) onRequestNotificationPermission()
                    onUpdateConfig { it.copy(keepAliveForeground = enabled) }
                },
                onToggleAutoBoot = { enabled ->
                    onUpdateConfig { it.copy(autoStartOnBoot = enabled) }
                },
                onTogglePowerWakePulse = { enabled ->
                    onUpdateConfig { it.copy(powerWakePulseEnabled = enabled) }
                },
                onTogglePauseLandscape = { enabled ->
                    onUpdateConfig { it.copy(pauseInLandscape = enabled) }
                },
                onOpenBatterySettings = onOpenBatterySettings,
                onOpenAutoStartSettings = onOpenAutoStartSettings
            )
        }

        // 4. Interactive Hardware Test Pad (supports all built-in + custom added buttons!)
        item {
            StudioSectionCard(
                title = "Live Edge Test Pad",
                subtitle = "Tap or press-and-hold any button, or click your physical keys right now",
                trailing = {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenAddButton)
                            .testTag("studio_add_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = TitaniumCyanContainer,
                        border = BorderStroke(1.dp, TitaniumCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add new button",
                                tint = TitaniumCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelMedium,
                                color = TitaniumCyan
                            )
                        }
                    }
                }
            ) {
                if (uiState.config.buttons.size <= 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.config.buttons.forEach { btn ->
                            TactileHoldButton(
                                label = btn.shortLabel,
                                icon = iconForButton(btn),
                                accentColor = Color(btn.accentColorArgb),
                                onPressChange = { down -> onSimulatedPress(btn.id, down) },
                                testTagName = "test_pad_${btn.id}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.config.buttons, key = { it.id }) { btn ->
                            TactileHoldButton(
                                label = btn.shortLabel,
                                icon = iconForButton(btn),
                                accentColor = Color(btn.accentColorArgb),
                                onPressChange = { down -> onSimulatedPress(btn.id, down) },
                                testTagName = "test_pad_${btn.id}",
                                modifier = Modifier.width(104.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Signature Style Presets Carousel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Signature Presets",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "TAP TO APPLY & PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(BezelPresetsCatalog.stylePresets, key = { it.id }) { preset ->
                        val isSelected = uiState.activePresetName == preset.name
                        PresetCard(
                            name = preset.name,
                            subtitle = preset.subtitle,
                            accentArgb = preset.accentArgb,
                            fillArgb = preset.fillArgb,
                            isSelected = isSelected,
                            onClick = { onApplyBuiltInPreset(preset) }
                        )
                    }
                }
            }
        }

        // 6. User Custom Presets
        if (uiState.customPresets.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Your Saved Presets",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.customPresets, key = { it.id }) { custom ->
                            CustomPresetChipCard(
                                preset = custom,
                                isSelected = uiState.activePresetName == custom.name,
                                onApply = { onApplyCustomPreset(custom) },
                                onDelete = { onDeleteCustomPreset(custom.id) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BezelPulse v${BuildConfig.VERSION_NAME} · Offline & Open Source",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun AlwaysOnBackgroundCard(
    config: BezelConfig,
    isKeepAliveRunning: Boolean,
    isBatteryUnrestricted: Boolean,
    onToggleKeepAlive: (Boolean) -> Unit,
    onToggleAutoBoot: (Boolean) -> Unit,
    onTogglePowerWakePulse: (Boolean) -> Unit,
    onTogglePauseLandscape: (Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit
) {
    val deviceHint = androidx.compose.runtime.remember {
        com.example.util.OemSupport.universalBackgroundHint()
    }

    StudioSectionCard(
        title = "Background Reliability",
        subtitle = "Keep animations active after clearing recents and across reboots",
        trailing = {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isKeepAliveRunning) EmeraldContainer else SignalAmberContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (isKeepAliveRunning) EmeraldActive else SignalAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isKeepAliveRunning) "PINNED" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isKeepAliveRunning) EmeraldActive else SignalAmber
                    )
                }
            }
        }
    ) {
        PrecisionToggleRow(
            title = "Stay Active After Clearing Recents",
            subtitle = "Runs a silent foreground service so Android does not unload the overlay",
            checked = config.keepAliveForeground,
            onCheckedChange = onToggleKeepAlive,
            testTagName = "keep_alive_switch"
        )
        PrecisionToggleRow(
            title = "Auto-Start After Reboot",
            subtitle = "Restores the background engine automatically when the device boots",
            checked = config.autoStartOnBoot,
            onCheckedChange = onToggleAutoBoot,
            testTagName = "auto_boot_switch"
        )
        PrecisionToggleRow(
            title = "Power Wake / Lock Fallback",
            subtitle = "Optional: also animate when the screen wakes or sleeps (may trigger on tap-to-wake)",
            checked = config.powerWakePulseEnabled,
            onCheckedChange = onTogglePowerWakePulse,
            testTagName = "power_wake_switch"
        )
        PrecisionToggleRow(
            title = "Pause in Landscape",
            subtitle = "Automatically pauses side-bezel animations during fullscreen videos and games",
            checked = config.pauseInLandscape,
            onCheckedChange = onTogglePauseLandscape,
            testTagName = "pause_landscape_switch"
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("battery_unrestrict_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    if (isBatteryUnrestricted) EmeraldActive.copy(alpha = 0.6f) else SignalAmber.copy(alpha = 0.65f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (isBatteryUnrestricted) EmeraldActive else SignalAmber,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (isBatteryUnrestricted) "Battery OK" else "Battery Settings",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isBatteryUnrestricted) EmeraldActive else SignalAmber,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = onOpenAutoStartSettings,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("oem_autostart_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TitaniumCyan.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = TitaniumCyan,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Auto-Start",
                    style = MaterialTheme.typography.labelMedium,
                    color = TitaniumCyan,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = deviceHint,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

private fun iconForButton(btn: ButtonConfig): ImageVector = when (btn.keyCode) {
    KeyEvent.KEYCODE_VOLUME_UP -> Icons.AutoMirrored.Rounded.VolumeUp
    KeyEvent.KEYCODE_VOLUME_DOWN -> Icons.AutoMirrored.Rounded.VolumeDown
    KeyEvent.KEYCODE_POWER -> Icons.Rounded.PowerSettingsNew
    else -> Icons.Rounded.TouchApp
}

@Composable
private fun HeroBezelCard(
    masterEnabled: Boolean,
    isServiceEnabled: Boolean,
    isKeepAliveRunning: Boolean,
    styleLabel: String,
    lastEventText: String?,
    onMasterToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(26.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_bezel_hero_banner_1795062643490),
                contentDescription = "BezelPulse hardware edge illumination artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianVoid.copy(alpha = 0.45f),
                                ObsidianVoid.copy(alpha = 0.72f),
                                ObsidianVoid.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isServiceEnabled && masterEnabled) EmeraldContainer.copy(alpha = 0.9f)
                        else TitaniumCyanContainer.copy(alpha = 0.9f),
                        border = BorderStroke(
                            1.dp,
                            if (isServiceEnabled && masterEnabled) EmeraldActive.copy(alpha = 0.6f)
                            else TitaniumCyan.copy(alpha = 0.55f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isServiceEnabled && masterEnabled) EmeraldActive else TitaniumCyan)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !masterEnabled -> "PAUSED"
                                    isServiceEnabled && isKeepAliveRunning -> "ALWAYS-ON · 120HZ"
                                    isServiceEnabled -> "SYSTEM-WIDE · 120HZ"
                                    else -> "IN-APP PREVIEW READY"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isServiceEnabled && masterEnabled) EmeraldActive else TitaniumCyan
                            )
                        }
                    }

                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = onMasterToggle,
                        modifier = Modifier.testTag("master_enable_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianVoid,
                            checkedTrackColor = TitaniumCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ObsidianCardHighlight
                        )
                    )
                }

                Column {
                    Text(
                        text = "BezelPulse",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = TitaniumCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = lastEventText ?: "Active curve: $styleLabel · Sub-frame VSYNC spring",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemOverlayStatusCard(
    isServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenCalibration: () -> Unit
) {
    val accent = if (isServiceEnabled) EmeraldActive else SignalAmber
    val badgeBg = if (isServiceEnabled) EmeraldContainer else SignalAmberContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = badgeBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isServiceEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.SettingsAccessibility,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isServiceEnabled) R.string.service_active else R.string.service_inactive
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            if (isServiceEnabled) R.string.service_active_sub else R.string.service_inactive_sub
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianElevated)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Privacy-first: filters hardware keys only. Never reads screen content or touches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isServiceEnabled) {
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("enable_service_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TitaniumCyan,
                            contentColor = ObsidianVoid
                        )
                    ) {
                        Text(
                            text = "Enable Service",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("manage_service_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenCalibration,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("studio_calibrate_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TitaniumCyan.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Straighten,
                        contentDescription = null,
                        tint = TitaniumCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Align Edges",
                        style = MaterialTheme.typography.labelLarge,
                        color = TitaniumCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    name: String,
    subtitle: String,
    accentArgb: Int,
    fillArgb: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(accentArgb)
    Surface(
        modifier = Modifier
            .width(158.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("preset_${name.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) ObsidianCardHighlight else ObsidianSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accent else HairlineBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(fillArgb))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(5.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent)
                    )
                }
                if (isSelected) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomPresetChipCard(
    preset: CustomPreset,
    isSelected: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color(preset.rimColorArgb)
    Surface(
        modifier = Modifier
            .width(172.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onApply),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) ObsidianCardHighlight else ObsidianSurface,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accent else HairlineBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${preset.style.label} · ${preset.maxDepthDp.toInt()}dp",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Delete preset ${preset.name}",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

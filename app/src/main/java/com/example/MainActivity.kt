package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.BezelRuntimeBus
import com.example.model.HardwareButtonType
import com.example.ui.components.EdgeCalibrationOverlay
import com.example.ui.components.InAppBezelOverlay
import com.example.ui.screens.ButtonsScreen
import com.example.ui.screens.PhysicsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.StyleScreen
import com.example.ui.theme.BezelPulseTheme
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TitaniumCyan
import com.example.ui.theme.TitaniumCyanContainer
import com.example.viewmodel.BezelViewModel
import com.example.viewmodel.StudioTab

class MainActivity : ComponentActivity() {

    private val viewModel: BezelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            BezelPulseTheme {
                BezelPulseApp(
                    viewModel = viewModel,
                    onOpenAccessibilitySettings = { openAccessibilitySettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
    }

    /**
     * Zero-latency in-app hardware button listener:
     * Even before the user enables the system-wide AccessibilityService, pressing physical
     * Volume Up / Volume Down / Power buttons inside BezelPulse immediately triggers the
     * live spring animation along the real screen edge without blocking normal OS behavior.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!BezelRuntimeBus.serviceConnected.value) {
            val buttonType = when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> HardwareButtonType.VOLUME_UP
                KeyEvent.KEYCODE_VOLUME_DOWN -> HardwareButtonType.VOLUME_DOWN
                KeyEvent.KEYCODE_POWER -> HardwareButtonType.POWER
                else -> null
            }

            if (buttonType != null) {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (event.repeatCount == 0) {
                            BezelRuntimeBus.dispatchButtonEvent(
                                type = buttonType,
                                isDown = true,
                                sourceLabel = "Physical Key"
                            )
                            BezelRuntimeBus.triggerHaptic(applicationContext, isPress = true)
                        }
                    }
                    KeyEvent.ACTION_UP -> {
                        BezelRuntimeBus.dispatchButtonEvent(
                            type = buttonType,
                            isDown = false,
                            sourceLabel = "Physical Key"
                        )
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }
}

@Composable
private fun BezelPulseApp(
    viewModel: BezelViewModel,
    onOpenAccessibilitySettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemServiceConnected by BezelRuntimeBus.serviceConnected.collectAsStateWithLifecycle()

    if (uiState.selectedTab != StudioTab.STUDIO && !uiState.isCalibrationMode) {
        BackHandler { viewModel.selectTab(StudioTab.STUDIO) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 640.dp

            if (useNavRail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TitaniumCyan,
                                    selectedTextColor = TitaniumCyan,
                                    indicatorColor = TitaniumCyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        StudioTabContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                            modifier = Modifier.widthIn(max = 680.dp)
                        )
                    }
                }
            } else {
                Scaffold(
                    containerColor = ObsidianVoid,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            windowInsets = WindowInsets.navigationBars,
                            tonalElevation = 0.dp
                        ) {
                            StudioTab.entries.forEach { tab ->
                                val selected = uiState.selectedTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TitaniumCyan,
                                        selectedTextColor = TitaniumCyan,
                                        indicatorColor = TitaniumCyanContainer,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    StudioTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Interactive Full-Screen Edge Calibration HUD
        AnimatedVisibility(
            visible = uiState.isCalibrationMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            EdgeCalibrationOverlay(
                config = uiState.config,
                onPositionChange = { type, pos -> viewModel.updateButtonPosition(type, pos) },
                onSideFlip = { type, side -> viewModel.updateButtonSide(type, side) },
                onTriggerPulse = { type -> viewModel.triggerButtonPulse(type, "Calibration") },
                onDone = { viewModel.setCalibrationMode(false) }
            )
        }

        // Full-bleed edge-to-edge live bezel animation overlay (auto-yields when system Accessibility overlay is active)
        InAppBezelOverlay(
            isSystemServiceRunning = systemServiceConnected,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StudioTabContent(
    uiState: com.example.viewmodel.BezelUiState,
    viewModel: BezelViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.selectedTab) {
        StudioTab.STUDIO -> StudioScreen(
            uiState = uiState,
            onMasterToggle = { enabled ->
                viewModel.updateConfig(clearActivePreset = false) { it.copy(masterEnabled = enabled) }
                if (enabled) viewModel.triggerCascadeDemo()
            },
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenCalibration = { viewModel.setCalibrationMode(true) },
            onSimulatedPress = { type, down -> viewModel.onSimulatedPress(type, down) },
            onApplyBuiltInPreset = { viewModel.applyBuiltInPreset(it) },
            onApplyCustomPreset = { viewModel.applyCustomPreset(it) },
            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
            modifier = modifier
        )

        StudioTab.BUTTONS -> ButtonsScreen(
            uiState = uiState,
            onSelectButton = { viewModel.selectButtonForEdit(it) },
            onApplyDeviceLayout = { viewModel.applyDeviceLayout(it) },
            onOpenCalibration = { viewModel.setCalibrationMode(true) },
            onToggleEnabled = { type, enabled -> viewModel.updateButtonEnabled(type, enabled) },
            onSelectSide = { type, side -> viewModel.updateButtonSide(type, side) },
            onPositionChange = { type, pos -> viewModel.updateButtonPosition(type, pos) },
            onLengthChange = { type, len -> viewModel.updateButtonLength(type, len) },
            onTriggerPulse = { viewModel.triggerButtonPulse(it) },
            onSimulatedPress = { type, down -> viewModel.onSimulatedPress(type, down) },
            modifier = modifier
        )

        StudioTab.STYLE -> StyleScreen(
            uiState = uiState,
            onSelectStyle = { viewModel.setStyle(it) },
            onUpdateConfig = { transform -> viewModel.updateConfig(transform = transform) },
            onPreviewPulse = { viewModel.triggerButtonPulse(uiState.selectedButton, "Style Preview") },
            modifier = modifier
        )

        StudioTab.PHYSICS -> PhysicsScreen(
            uiState = uiState,
            onUpdateConfig = { transform -> viewModel.updateConfig(transform = transform) },
            onPreviewPulse = { viewModel.triggerButtonPulse(uiState.selectedButton, "Physics Preview") },
            onTriggerCascade = { viewModel.triggerCascadeDemo() },
            onOpenSavePresetDialog = { viewModel.setShowSavePresetDialog(true) },
            onDismissSavePresetDialog = { viewModel.setShowSavePresetDialog(false) },
            onConfirmSavePreset = { name -> viewModel.saveCurrentAsCustomPreset(name) },
            onResetDefaults = { viewModel.resetToDefaults() },
            modifier = modifier
        )
    }
}

private fun tabIcon(tab: StudioTab, selected: Boolean): ImageVector = when (tab) {
    StudioTab.STUDIO -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
    StudioTab.BUTTONS -> if (selected) Icons.Rounded.Tune else Icons.Outlined.Tune
    StudioTab.STYLE -> if (selected) Icons.Rounded.Palette else Icons.Outlined.Palette
    StudioTab.PHYSICS -> if (selected) Icons.Rounded.Speed else Icons.Outlined.Speed
}

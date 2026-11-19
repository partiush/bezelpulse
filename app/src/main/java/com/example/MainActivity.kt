package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.BezelRuntimeBus
import com.example.ui.components.AddButtonDialog
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
import com.example.viewmodel.BezelUiState
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
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onOpenBatterySettings = { openBatteryOptimizationSettings() },
                    onOpenAutoStartSettings = { openAutoStartSettings() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatus()
    }

    /**
     * Zero-latency in-app hardware key listener + Live Physical Key Capture for AddButtonDialog.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val captureListener = BezelRuntimeBus.onKeyCaptureListener
        if (captureListener != null) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
                event.keyCode != KeyEvent.KEYCODE_BACK
            ) {
                captureListener.invoke(event.keyCode)
                return true
            }
            if (event.action == KeyEvent.ACTION_UP && event.keyCode != KeyEvent.KEYCODE_BACK) {
                return true
            }
        }

        if (!BezelRuntimeBus.serviceConnected.value) {
            val isDown = when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount > 0) return super.dispatchKeyEvent(event)
                    true
                }
                KeyEvent.ACTION_UP -> false
                else -> return super.dispatchKeyEvent(event)
            }

            val matched = BezelRuntimeBus.dispatchKeyCode(
                keyCode = event.keyCode,
                isDown = isDown,
                sourceLabel = "Physical Key"
            )
            if (matched && isDown) {
                BezelRuntimeBus.triggerHaptic(applicationContext, isPress = true)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun openAccessibilitySettings() {
        com.example.util.OemSupport.openAccessibilitySettings(this)
    }

    private fun openBatteryOptimizationSettings() {
        com.example.util.OemSupport.openBatterySettings(this)
    }

    private fun openAutoStartSettings() {
        com.example.util.OemSupport.openAutoStartSettings(this)
    }
}

@Composable
private fun BezelPulseApp(
    viewModel: BezelViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val systemServiceConnected by BezelRuntimeBus.serviceConnected.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.refreshSystemStatus() }
    )

    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Proactively request notification permission once on Android 13+ so the silent Keep-Alive service displays cleanly
    LaunchedEffect(Unit) {
        if (uiState.config.keepAliveForeground) {
            requestNotificationPermissionIfNeeded()
        }
    }

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
                            onOpenBatterySettings = onOpenBatterySettings,
                            onOpenAutoStartSettings = onOpenAutoStartSettings,
                            onRequestNotificationPermission = requestNotificationPermissionIfNeeded,
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
                        onOpenBatterySettings = onOpenBatterySettings,
                        onOpenAutoStartSettings = onOpenAutoStartSettings,
                        onRequestNotificationPermission = requestNotificationPermissionIfNeeded,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Interactive Full-Screen Edge Calibration HUD (zero-jitter fixed coordinate tracking)
        AnimatedVisibility(
            visible = uiState.isCalibrationMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            EdgeCalibrationOverlay(
                config = uiState.config,
                onPositionChange = { id, pos -> viewModel.updateButtonPosition(id, pos) },
                onSideFlip = { id, side -> viewModel.updateButtonSide(id, side) },
                onLengthChange = { id, len -> viewModel.updateButtonLength(id, len) },
                onTriggerPulse = { id -> viewModel.triggerButtonPulse(id, "Calibration") },
                onDone = { viewModel.setCalibrationMode(false) }
            )
        }

        // Add Custom Hardware Button Dialog
        if (uiState.showAddButtonDialog) {
            AddButtonDialog(
                onDismiss = { viewModel.setShowAddButtonDialog(false) },
                onConfirm = { name, keyCode, glyph, side, accent ->
                    viewModel.addCustomButton(name, keyCode, glyph, side, accent)
                }
            )
        }

        // Full-bleed edge-to-edge live bezel animation overlay
        InAppBezelOverlay(
            isSystemServiceRunning = systemServiceConnected,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun StudioTabContent(
    uiState: BezelUiState,
    viewModel: BezelViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.selectedTab) {
        StudioTab.STUDIO -> StudioScreen(
            uiState = uiState,
            onMasterToggle = { enabled ->
                viewModel.updateConfig(clearActivePreset = false) { it.copy(masterEnabled = enabled) }
                if (enabled) viewModel.triggerCascadeDemo()
            },
            onUpdateConfig = { transform -> viewModel.updateConfig(clearActivePreset = false, transform = transform) },
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenAutoStartSettings = onOpenAutoStartSettings,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenCalibration = { viewModel.setCalibrationMode(true) },
            onOpenAddButton = { viewModel.setShowAddButtonDialog(true) },
            onSimulatedPress = { id, down -> viewModel.onSimulatedPress(id, down) },
            onApplyBuiltInPreset = { viewModel.applyBuiltInPreset(it) },
            onApplyCustomPreset = { viewModel.applyCustomPreset(it) },
            onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
            modifier = modifier
        )

        StudioTab.BUTTONS -> ButtonsScreen(
            uiState = uiState,
            onSelectButton = { viewModel.selectButtonForEdit(it) },
            onOpenAddButton = { viewModel.setShowAddButtonDialog(true) },
            onDeleteCustomButton = { viewModel.deleteCustomButton(it) },
            onApplyDeviceLayout = { viewModel.applyDeviceLayout(it) },
            onOpenCalibration = { viewModel.setCalibrationMode(true) },
            onToggleEnabled = { id, enabled -> viewModel.updateButtonEnabled(id, enabled) },
            onSelectSide = { id, side -> viewModel.updateButtonSide(id, side) },
            onPositionChange = { id, pos -> viewModel.updateButtonPosition(id, pos) },
            onLengthChange = { id, len -> viewModel.updateButtonLength(id, len) },
            onTriggerPulse = { viewModel.triggerButtonPulse(it) },
            onSimulatedPress = { id, down -> viewModel.onSimulatedPress(id, down) },
            modifier = modifier
        )

        StudioTab.STYLE -> StyleScreen(
            uiState = uiState,
            onSelectStyle = { viewModel.setStyle(it) },
            onUpdateConfig = { transform -> viewModel.updateConfig(transform = transform) },
            onPreviewPulse = { viewModel.triggerButtonPulse(uiState.selectedButtonId, "Style Preview") },
            modifier = modifier
        )

        StudioTab.PHYSICS -> PhysicsScreen(
            uiState = uiState,
            onUpdateConfig = { transform -> viewModel.updateConfig(transform = transform) },
            onPreviewPulse = { viewModel.triggerButtonPulse(uiState.selectedButtonId, "Physics Preview") },
            onTriggerCascade = { viewModel.triggerCascadeDemo() },
            onOpenSavePresetDialog = { viewModel.setShowSavePresetDialog(true) },
            onDismissSavePresetDialog = { viewModel.setShowSavePresetDialog(false) },
            onConfirmSavePreset = { name -> viewModel.saveCurrentAsCustomPreset(name) },
            onExportConfigJson = { viewModel.exportConfigJson() },
            onImportConfigJson = { raw -> viewModel.importConfigJson(raw) },
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

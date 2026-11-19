package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import com.example.model.ButtonIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Listens for hardware key events system-wide and drives the pre-attached overlay view.
 * Always returns false from [onKeyEvent] so normal OS volume and power behavior is untouched.
 */
class BezelAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: BezelOverlayView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastPowerPulseMs: Long = 0L

    private val busListener = BezelRuntimeBus.ButtonListener { buttonId, isDown, isPulseOnly, holdOverrideMs ->
        mainHandler.post {
            overlayView?.onButtonEvent(buttonId, isDown, isPulseOnly, holdOverrideMs)
        }
    }

    private val systemPowerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val cfg = BezelRuntimeBus.config.value
            if (!cfg.masterEnabled || shouldSkipForOrientation(cfg)) return

            when (action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (cfg.powerWakePulseEnabled) {
                        // Brief delay lets the display backlight/OLED panel turn on before the curve peaks.
                        mainHandler.postDelayed({
                            triggerPowerPulse("Screen Wake", holdOverrideMs = 210L)
                        }, 110L)
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (cfg.powerWakePulseEnabled && SystemClock.uptimeMillis() - lastPowerPulseMs > 1800L) {
                        triggerPowerPulse("Device Unlock", holdOverrideMs = 150L)
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    triggerPowerPulse("Screen Lock", holdOverrideMs = 120L)
                }
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra("reason").orEmpty().lowercase()
                    if (reason == "globalactions" || reason == "lock") {
                        triggerPowerPulse("Power Key", holdOverrideMs = 190L)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        BezelRuntimeBus.setServiceConnected(true)

        val repo = BezelPreferencesRepository(applicationContext)
        serviceScope.launch {
            repo.configFlow.collect { savedConfig ->
                BezelRuntimeBus.updateConfig(savedConfig)
                if (savedConfig.masterEnabled && savedConfig.keepAliveForeground) {
                    BezelKeepAliveService.start(applicationContext)
                }
            }
        }

        attachOverlayWindow()
        BezelRuntimeBus.registerListener(busListener)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            @Suppress("DEPRECATION")
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemPowerReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(systemPowerReceiver, filter)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayView?.requestLayout()
    }

    private fun shouldSkipForOrientation(cfg: com.example.model.BezelConfig): Boolean {
        return cfg.pauseInLandscape &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun attachOverlayWindow() {
        if (overlayView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val view = BezelOverlayView(this)
        @Suppress("DEPRECATION")
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        runCatching {
            wm.addView(view, params)
            overlayView = view
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val cfg = BezelRuntimeBus.config.value
        if (shouldSkipForOrientation(cfg)) return false

        val isDown = when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount > 0) return false
                true
            }
            KeyEvent.ACTION_UP -> false
            else -> return false
        }

        val matched = BezelRuntimeBus.dispatchKeyCode(
            keyCode = event.keyCode,
            isDown = isDown,
            sourceLabel = "Hardware Key"
        )
        if (matched && isDown) {
            if (event.keyCode == KeyEvent.KEYCODE_POWER) {
                lastPowerPulseMs = SystemClock.uptimeMillis()
            }
            BezelRuntimeBus.triggerHaptic(applicationContext, isPress = true)
        }

        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val cfg = BezelRuntimeBus.config.value
        if (!cfg.masterEnabled || shouldSkipForOrientation(cfg)) return

        val className = event.className?.toString().orEmpty().lowercase()
        val pkgName = event.packageName?.toString().orEmpty().lowercase()

        // Many OEM frameworks swallow KEYCODE_POWER before AccessibilityService sees it;
        // detect the system power menu window as a clean fallback for long-press power.
        val isPowerDialog = className.contains("globalactions") ||
            className.contains("powermenu") ||
            className.contains("shutdown") ||
            className.contains("rebootdialog") ||
            (pkgName.contains("systemui") && className.contains("power")) ||
            (pkgName.contains("miui") && (className.contains("power") || className.contains("globalaction")))

        if (isPowerDialog) {
            triggerPowerPulse("Power Menu", holdOverrideMs = 220L)
        }
    }

    private fun triggerPowerPulse(source: String, holdOverrideMs: Long) {
        val now = SystemClock.uptimeMillis()
        if (now - lastPowerPulseMs < 240L) return
        lastPowerPulseMs = now

        val cfg = BezelRuntimeBus.config.value
        val powerTargets = cfg.buttons.filter {
            it.enabled && (it.id == ButtonIds.POWER || it.keyCode == KeyEvent.KEYCODE_POWER)
        }
        for (btn in powerTargets) {
            BezelRuntimeBus.dispatchButtonEvent(
                buttonId = btn.id,
                isDown = true,
                sourceLabel = source,
                isPulseOnly = true,
                holdOverrideMs = holdOverrideMs
            )
        }
        if (powerTargets.isNotEmpty()) {
            BezelRuntimeBus.triggerHaptic(applicationContext, isPress = true)
        }
    }

    override fun onInterrupt() {
        overlayView?.stopAnimationLoop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        BezelRuntimeBus.setServiceConnected(false)
        BezelRuntimeBus.unregisterListener(busListener)
        runCatching { unregisterReceiver(systemPowerReceiver) }
        overlayView?.let { view ->
            view.stopAnimationLoop()
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayView = null
        serviceScope.cancel()
    }
}

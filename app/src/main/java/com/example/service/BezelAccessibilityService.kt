package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import com.example.model.BezelConfig
import com.example.model.ButtonIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * System-wide hardware key listener and overlay host.
 *
 * Always returns false from [onKeyEvent] so normal OS volume and power handling is never delayed.
 *
 * Power button behavior:
 *  - Direct hardware key events (KEYCODE_POWER / SLEEP / WAKEUP / SOFT_SLEEP) drive true
 *    press, continuous hold swell, and release animations whenever forwarded by the OS.
 *  - Long-pressing the Power button on Android 12-16 opens the Power Menu (GlobalActions) or
 *    Assistant dialog, which triggers a sustained hold animation that stays expanded while the
 *    menu is active and releases smoothly when dismissed.
 *  - Screen wake/lock display transitions are strictly opt-in ([BezelConfig.powerWakePulseEnabled])
 *    so tap-to-wake, lift-to-wake, and lock-screen unlocks never cause false animations by default.
 */
class BezelAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var displayManager: DisplayManager? = null
    private var overlayView: BezelOverlayView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastDisplayState: Int = Display.STATE_UNKNOWN
    private var lastPowerWakePulseMs: Long = 0L
    private var lastPowerSleepPulseMs: Long = 0L
    private var lastPowerMenuHoldMs: Long = 0L

    private var isDirectPowerKeyHeld: Boolean = false
    private var isPowerMenuHoldActive: Boolean = false

    private val powerMenuAutoReleaseRunnable = Runnable {
        releasePowerMenuHold()
    }

    private val busListener = BezelRuntimeBus.ButtonListener { buttonId, isDown, isPulseOnly, holdOverrideMs ->
        mainHandler.post {
            ensureOverlayAttached()
            overlayView?.onButtonEvent(buttonId, isDown, isPulseOnly, holdOverrideMs)
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
            val newState = display.state
            if (newState == lastDisplayState) return

            val prevState = lastDisplayState
            lastDisplayState = newState

            val cfg = BezelRuntimeBus.config.value
            if (!cfg.masterEnabled || shouldSkipForOrientation(cfg)) return

            val wasAsleep = prevState == Display.STATE_OFF ||
                prevState == Display.STATE_DOZE ||
                prevState == Display.STATE_DOZE_SUSPEND
            val isNowAwake = newState == Display.STATE_ON
            val isGoingToSleep = prevState == Display.STATE_ON &&
                (newState == Display.STATE_OFF || newState == Display.STATE_DOZE || newState == Display.STATE_DOZE_SUSPEND)

            if (isGoingToSleep) {
                clearHeldStateOnScreenOff()
                if (cfg.powerWakePulseEnabled) {
                    triggerSleepPowerPulse("Display Sleep")
                }
            } else if (wasAsleep && isNowAwake && cfg.powerWakePulseEnabled) {
                scheduleWakePowerPulse("Display Wake")
            }
        }
    }

    private val systemPowerReceiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val cfg = BezelRuntimeBus.config.value
            if (!cfg.masterEnabled || shouldSkipForOrientation(cfg)) return

            when (action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (cfg.powerWakePulseEnabled) {
                        scheduleWakePowerPulse("Screen Wake")
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    clearHeldStateOnScreenOff()
                    if (cfg.powerWakePulseEnabled) {
                        triggerSleepPowerPulse("Screen Lock")
                    }
                }
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                    val reason = intent.getStringExtra("reason").orEmpty().lowercase()
                    if (reason == "globalactions") {
                        startPowerMenuHold("Power Hold")
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

        ensureOverlayAttached()
        BezelRuntimeBus.registerListener(busListener)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        lastDisplayState = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.state ?: Display.STATE_ON
        displayManager?.registerDisplayListener(displayListener, mainHandler)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
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

    private fun shouldSkipForOrientation(cfg: BezelConfig): Boolean {
        return cfg.pauseInLandscape &&
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun ensureOverlayAttached() {
        val existing = overlayView
        if (existing != null && existing.isAttachedToWindow) {
            if (existing.visibility != View.VISIBLE) {
                existing.visibility = View.VISIBLE
            }
            return
        }
        if (existing != null) {
            runCatching { windowManager?.removeViewImmediate(existing) }
            overlayView = null
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val view = BezelOverlayView(this)
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = 1f
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
            KeyEvent.ACTION_DOWN -> true
            KeyEvent.ACTION_UP -> false
            else -> return false
        }
        val isInitialPress = isDown && event.repeatCount == 0

        val normalizedCode = when (event.keyCode) {
            KeyEvent.KEYCODE_SLEEP,
            KeyEvent.KEYCODE_WAKEUP,
            KeyEvent.KEYCODE_SOFT_SLEEP -> KeyEvent.KEYCODE_POWER
            else -> event.keyCode
        }

        if (normalizedCode == KeyEvent.KEYCODE_POWER) {
            isDirectPowerKeyHeld = isDown
            if (isDown) {
                lastPowerWakePulseMs = SystemClock.uptimeMillis()
                lastPowerSleepPulseMs = lastPowerWakePulseMs
            } else if (isPowerMenuHoldActive) {
                isPowerMenuHoldActive = false
                mainHandler.removeCallbacks(powerMenuAutoReleaseRunnable)
            }
        }

        val matched = BezelRuntimeBus.dispatchKeyCode(
            keyCode = normalizedCode,
            isDown = isDown,
            sourceLabel = if (isDown && event.repeatCount > 0) "Hardware Hold" else "Hardware Key"
        )
        if (matched && isInitialPress) {
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

        val isPowerDialog = className.contains("globalactions") ||
            className.contains("powermenu") ||
            className.contains("powerdialog") ||
            className.contains("shutdown") ||
            className.contains("rebootdialog") ||
            (pkgName.contains("systemui") && (className.contains("power") || className.contains("globalaction"))) ||
            (pkgName.contains("miui") && (className.contains("power") || className.contains("globalaction")))

        if (isPowerDialog) {
            startPowerMenuHold("Power Hold")
        } else if (isPowerMenuHoldActive && className.isNotEmpty()) {
            // Power menu closed or user switched back to an app — release the sustained hold smoothly.
            releasePowerMenuHold()
        }
    }

    private fun startPowerMenuHold(source: String) {
        val now = SystemClock.uptimeMillis()
        if (isDirectPowerKeyHeld) return
        if (isPowerMenuHoldActive) {
            // Extend hold window while menu stays active
            mainHandler.removeCallbacks(powerMenuAutoReleaseRunnable)
            mainHandler.postDelayed(powerMenuAutoReleaseRunnable, POWER_MENU_HOLD_CAP_MS)
            return
        }
        if (now - lastPowerMenuHoldMs < 350L) return
        lastPowerMenuHoldMs = now
        isPowerMenuHoldActive = true

        val powerTargets = activePowerTargets()
        for (btn in powerTargets) {
            BezelRuntimeBus.dispatchButtonEvent(
                buttonId = btn.id,
                isDown = true,
                sourceLabel = source,
                isPulseOnly = false
            )
        }
        if (powerTargets.isNotEmpty()) {
            BezelRuntimeBus.triggerHaptic(applicationContext, isPress = true)
        }

        mainHandler.removeCallbacks(powerMenuAutoReleaseRunnable)
        mainHandler.postDelayed(powerMenuAutoReleaseRunnable, POWER_MENU_HOLD_CAP_MS)
    }

    private fun releasePowerMenuHold() {
        mainHandler.removeCallbacks(powerMenuAutoReleaseRunnable)
        if (!isPowerMenuHoldActive) return
        isPowerMenuHoldActive = false

        for (btn in activePowerTargets()) {
            BezelRuntimeBus.dispatchButtonEvent(
                buttonId = btn.id,
                isDown = false,
                sourceLabel = "Power Release",
                isPulseOnly = false
            )
        }
    }

    private fun clearHeldStateOnScreenOff() {
        isDirectPowerKeyHeld = false
        releasePowerMenuHold()
        mainHandler.post {
            overlayView?.releaseAllButtons()
        }
    }

    private fun scheduleWakePowerPulse(source: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastPowerWakePulseMs < 500L) return
        lastPowerWakePulseMs = now

        mainHandler.postDelayed({
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isInteractive) return@postDelayed
            ensureOverlayAttached()
            dispatchPowerPulse(source, holdOverrideMs = 260L)
        }, 160L)
    }

    private fun triggerSleepPowerPulse(source: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastPowerSleepPulseMs < 400L) return
        lastPowerSleepPulseMs = now
        dispatchPowerPulse(source, holdOverrideMs = 150L)
    }

    private fun activePowerTargets() = BezelRuntimeBus.config.value.buttons.filter {
        it.enabled && (it.id == ButtonIds.POWER || it.keyCode == KeyEvent.KEYCODE_POWER)
    }

    private fun dispatchPowerPulse(source: String, holdOverrideMs: Long) {
        val powerTargets = activePowerTargets()
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
        mainHandler.removeCallbacksAndMessages(null)
        isDirectPowerKeyHeld = false
        isPowerMenuHoldActive = false
        BezelRuntimeBus.setServiceConnected(false)
        BezelRuntimeBus.unregisterListener(busListener)
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
        runCatching { unregisterReceiver(systemPowerReceiver) }
        overlayView?.let { view ->
            view.stopAnimationLoop()
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayView = null
        serviceScope.cancel()
    }

    companion object {
        private const val POWER_MENU_HOLD_CAP_MS = 1500L
    }
}

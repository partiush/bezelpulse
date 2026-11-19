package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.data.BezelPreferencesRepository
import com.example.engine.BezelRuntimeBus
import com.example.model.HardwareButtonType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Privacy-first, zero-latency system-wide AccessibilityService.
 * - Reads NO window content (canRetrieveWindowContent=false).
 * - Filters hardware volume/power key events and ALWAYS returns false so native OS handling is never delayed.
 * - Pre-attaches a non-touchable hardware-accelerated TYPE_ACCESSIBILITY_OVERLAY view for sub-frame onset.
 */
class BezelAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: BezelOverlayView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val busListener = BezelRuntimeBus.ButtonListener { type, isDown, isPulseOnly ->
        mainHandler.post {
            overlayView?.onButtonEvent(type, isDown, isPulseOnly)
        }
    }

    private val screenEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_SCREEN_OFF) {
                // Fallback pulse for OEMs that intercept KEYCODE_POWER at kernel/PhoneWindowManager level
                BezelRuntimeBus.dispatchButtonEvent(
                    type = HardwareButtonType.POWER,
                    isDown = true,
                    sourceLabel = "System Power",
                    isPulseOnly = true
                )
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
            }
        }

        attachOverlayWindow()
        BezelRuntimeBus.registerListener(busListener)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenEventReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenEventReceiver, filter)
        }
    }

    private fun attachOverlayWindow() {
        if (overlayView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val view = BezelOverlayView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
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
        val buttonType = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> HardwareButtonType.VOLUME_UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> HardwareButtonType.VOLUME_DOWN
            KeyEvent.KEYCODE_POWER -> HardwareButtonType.POWER
            else -> return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    BezelRuntimeBus.dispatchButtonEvent(
                        type = buttonType,
                        isDown = true,
                        sourceLabel = "Hardware Key"
                    )
                    BezelRuntimeBus.triggerHaptic(applicationContext, isPress = true)
                }
            }
            KeyEvent.ACTION_UP -> {
                BezelRuntimeBus.dispatchButtonEvent(
                    type = buttonType,
                    isDown = false,
                    sourceLabel = "Hardware Key"
                )
            }
        }

        // CRITICAL: Always return false so Android processes the real volume/power action natively!
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally unused: this service only renders bezel feedback on hardware key presses.
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
        runCatching { unregisterReceiver(screenEventReceiver) }
        overlayView?.let { view ->
            view.stopAnimationLoop()
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayView = null
        serviceScope.cancel()
    }
}

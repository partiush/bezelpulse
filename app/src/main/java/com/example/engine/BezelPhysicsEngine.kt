package com.example.engine

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.BezelConfig
import com.example.model.ButtonEventRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Damped harmonic spring state for a single hardware button.
 *
 * Pulse hold durations are measured in accumulated rendered frame time rather than wall-clock
 * deltas so display wake-up VSYNC stalls cannot expire an animation before its first frame draws.
 */
class ButtonSpringState(val buttonId: String) {
    var displacement: Float = 0f
        private set
    var velocity: Float = 0f
        private set
    var isPhysicallyPressed: Boolean = false
        private set

    private var pressStartTimeMs: Long = 0L
    private var releasePending: Boolean = false
    private var pulseRenderedMs: Float = 0f
    private var pulseTargetHoldMs: Float = 0f

    fun onPress(nowMs: Long = SystemClock.uptimeMillis()) {
        if (!isPhysicallyPressed) {
            isPhysicallyPressed = true
            pressStartTimeMs = nowMs
            releasePending = false
            pulseRenderedMs = 0f
            pulseTargetHoldMs = 0f
            if (velocity < 2.2f) {
                velocity += 2.2f
            }
        }
    }

    fun onRelease(nowMs: Long = SystemClock.uptimeMillis(), minHoldMs: Long) {
        isPhysicallyPressed = false
        val heldMs = (nowMs - pressStartTimeMs).coerceAtLeast(0L)
        if (heldMs < minHoldMs) {
            releasePending = true
            pulseRenderedMs = heldMs.toFloat()
            pulseTargetHoldMs = minHoldMs.toFloat()
        } else {
            releasePending = false
        }
    }

    fun triggerPulse(nowMs: Long = SystemClock.uptimeMillis(), holdDurationOverrideMs: Long? = null) {
        isPhysicallyPressed = false
        pressStartTimeMs = nowMs
        releasePending = true
        pulseRenderedMs = 0f
        pulseTargetHoldMs = (holdDurationOverrideMs ?: 0L).toFloat()
        if (velocity < 3.4f) {
            velocity += 3.4f
        }
    }

    fun isActive(): Boolean {
        return isPhysicallyPressed || releasePending || abs(displacement) > 0.002f || abs(velocity) > 0.004f
    }

    fun step(dtSeconds: Float, config: BezelConfig, nowMs: Long = SystemClock.uptimeMillis()): Boolean {
        val clampedDt = dtSeconds.coerceIn(0.0005f, 0.033f)

        if (releasePending) {
            val requiredHoldMs = if (pulseTargetHoldMs > 0f) pulseTargetHoldMs else config.minPressHoldMs.toFloat()
            pulseRenderedMs += clampedDt * 1000f
            if (pulseRenderedMs >= requiredHoldMs) {
                releasePending = false
                pulseTargetHoldMs = 0f
            }
        }

        val target = when {
            isPhysicallyPressed -> {
                val heldMs = (nowMs - pressStartTimeMs).coerceAtLeast(0L)
                val sustainFraction = ((heldMs - 140L).coerceAtLeast(0L) / 240f).coerceIn(0f, 1f)
                1.0f + (config.holdBoostMultiplier - 1.0f) * sustainFraction
            }
            releasePending -> 1.0f
            else -> 0.0f
        }

        val subSteps = 4
        val subDt = clampedDt / subSteps
        val k = config.stiffness.coerceIn(120f, 2000f)
        val zeta = config.dampingRatio.coerceIn(0.25f, 1.2f)
        val c = 2f * zeta * sqrt(k)

        for (i in 0 until subSteps) {
            val force = -k * (displacement - target) - c * velocity
            velocity += force * subDt
            displacement += velocity * subDt
            if (displacement < 0f && target == 0f) {
                displacement = 0f
                if (velocity < 0f) velocity = -velocity * 0.12f
            }
        }

        if (!isPhysicallyPressed && !releasePending && abs(displacement) < 0.002f && abs(velocity) < 0.005f) {
            displacement = 0f
            velocity = 0f
            return false
        }
        return true
    }
}

class BezelSpringController {
    private val states = ConcurrentHashMap<String, ButtonSpringState>()

    fun stateFor(buttonId: String): ButtonSpringState =
        states.computeIfAbsent(buttonId) { ButtonSpringState(it) }

    fun onButtonEvent(buttonId: String, isDown: Boolean, config: BezelConfig) {
        val state = stateFor(buttonId)
        if (isDown) {
            state.onPress()
        } else {
            state.onRelease(minHoldMs = config.minPressHoldMs)
        }
    }

    fun triggerPulse(buttonId: String, holdOverrideMs: Long? = null) {
        stateFor(buttonId).triggerPulse(holdDurationOverrideMs = holdOverrideMs)
    }

    fun stepAll(dtSeconds: Float, config: BezelConfig): Boolean {
        val nowMs = SystemClock.uptimeMillis()
        var anyActive = false
        for (state in states.values) {
            if (state.isActive()) {
                if (state.step(dtSeconds, config, nowMs)) {
                    anyActive = true
                }
            }
        }
        return anyActive
    }
}

data class SpringTelemetry(
    val samples: List<Float>,
    val peakOvershootPercent: Int,
    val settlingTimeMs: Int,
    val regimeLabel: String
)

object SpringAnalyzer {
    fun analyze(stiffness: Float, dampingRatio: Float, durationMs: Int = 550): SpringTelemetry {
        val sampleCount = 110
        val dt = (durationMs / 1000f) / sampleCount
        var x = 0f
        var v = 2.4f
        val k = stiffness.coerceIn(120f, 2000f)
        val zeta = dampingRatio.coerceIn(0.25f, 1.2f)
        val c = 2f * zeta * sqrt(k)

        val samples = ArrayList<Float>(sampleCount + 1)
        samples.add(0f)

        var maxVal = 0f
        var lastUnsettledIdx = 0

        for (step in 1..sampleCount) {
            val subSteps = 4
            val subDt = dt / subSteps
            for (s in 0 until subSteps) {
                val force = -k * (x - 1f) - c * v
                v += force * subDt
                x += v * subDt
            }
            samples.add(x)
            if (x > maxVal) maxVal = x
            if (abs(x - 1f) > 0.025f || abs(v) > 0.08f) {
                lastUnsettledIdx = step
            }
        }

        val overshootPct = ((maxVal - 1f).coerceAtLeast(0f) * 100f).toInt()
        val settlingMs = ((lastUnsettledIdx.toFloat() / sampleCount) * durationMs).toInt().coerceIn(40, durationMs)
        val regime = when {
            dampingRatio < 0.65f -> "Playful Bounce"
            dampingRatio < 0.88f -> "Snappy Spring"
            dampingRatio <= 1.02f -> "iOS Critical"
            else -> "Viscous Smooth"
        }

        return SpringTelemetry(
            samples = samples,
            peakOvershootPercent = overshootPct,
            settlingTimeMs = settlingMs,
            regimeLabel = regime
        )
    }
}

object BezelRuntimeBus {
    fun interface ButtonListener {
        fun onHardwareButton(buttonId: String, isDown: Boolean, isPulseOnly: Boolean, holdOverrideMs: Long?)
    }

    private val _config = MutableStateFlow(BezelConfig())
    val config: StateFlow<BezelConfig> = _config.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _keepAliveRunning = MutableStateFlow(false)
    val keepAliveRunning: StateFlow<Boolean> = _keepAliveRunning.asStateFlow()

    private val _lastEvent = MutableStateFlow<ButtonEventRecord?>(null)
    val lastEvent: StateFlow<ButtonEventRecord?> = _lastEvent.asStateFlow()

    @Volatile
    var onKeyCaptureListener: ((Int) -> Unit)? = null

    private val listeners = CopyOnWriteArrayList<ButtonListener>()

    fun updateConfig(newConfig: BezelConfig) {
        _config.value = newConfig
    }

    fun setServiceConnected(connected: Boolean) {
        _serviceConnected.value = connected
    }

    fun setKeepAliveRunning(running: Boolean) {
        _keepAliveRunning.value = running
    }

    fun registerListener(listener: ButtonListener) {
        listeners.addIfAbsent(listener)
    }

    fun unregisterListener(listener: ButtonListener) {
        listeners.remove(listener)
    }

    fun dispatchKeyCode(keyCode: Int, isDown: Boolean, sourceLabel: String): Boolean {
        val capture = onKeyCaptureListener
        if (capture != null && isDown) {
            capture.invoke(keyCode)
            return true
        }

        val cfg = _config.value
        if (!cfg.masterEnabled) return false
        val matched = cfg.buttonsForKeyCode(keyCode)
        if (matched.isEmpty()) return false

        for (btn in matched) {
            dispatchButtonEvent(
                buttonId = btn.id,
                isDown = isDown,
                sourceLabel = sourceLabel
            )
        }
        return true
    }

    fun dispatchButtonEvent(
        buttonId: String,
        isDown: Boolean,
        sourceLabel: String,
        isPulseOnly: Boolean = false,
        holdOverrideMs: Long? = null
    ) {
        val cfg = _config.value
        if (!cfg.masterEnabled) return
        val button = cfg.buttonById(buttonId) ?: return
        if (!button.enabled) return

        _lastEvent.value = ButtonEventRecord(
            buttonId = button.id,
            buttonLabel = button.shortLabel,
            isDown = isDown,
            timestampMs = System.currentTimeMillis(),
            sourceLabel = sourceLabel
        )
        for (listener in listeners) {
            listener.onHardwareButton(button.id, isDown, isPulseOnly, holdOverrideMs)
        }
    }

    fun triggerHaptic(context: Context, isPress: Boolean) {
        val cfg = _config.value
        if (!cfg.hapticFeedbackEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = ((if (isPress) 160 else 85) * cfg.hapticStrength)
                    .toInt()
                    .coerceIn(1, 255)
                val durationMs = if (isPress) 12L else 7L
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isPress) 12L else 7L)
            }
        } catch (_: SecurityException) {
            // Device policy restricts vibration
        }
    }
}

package com.example.engine

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.BezelConfig
import com.example.model.ButtonEventRecord
import com.example.model.HardwareButtonType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * High-precision critically/under-damped spring physics state for a single hardware button.
 * Sub-stepped semi-implicit Euler integration guarantees 120Hz+ smoothness and numerical stability
 * across the entire stiffness (150..1800) and damping (0.35..1.1) range.
 */
class ButtonSpringState(val type: HardwareButtonType) {
    var displacement: Float = 0f
        private set
    var velocity: Float = 0f
        private set
    var isPhysicallyPressed: Boolean = false
        private set

    private var pressStartTimeMs: Long = 0L
    private var releasePending: Boolean = false

    fun onPress(nowMs: Long = SystemClock.uptimeMillis()) {
        if (!isPhysicallyPressed) {
            isPhysicallyPressed = true
            pressStartTimeMs = nowMs
            releasePending = false
            // Immediate tiny initial velocity impulse for perceptible zero-latency onset
            if (velocity < 2.2f) {
                velocity += 2.2f
            }
        }
    }

    fun onRelease(nowMs: Long = SystemClock.uptimeMillis(), minHoldMs: Long) {
        isPhysicallyPressed = false
        val heldMs = nowMs - pressStartTimeMs
        if (heldMs < minHoldMs) {
            releasePending = true
        } else {
            releasePending = false
        }
    }

    fun triggerPulse(nowMs: Long = SystemClock.uptimeMillis()) {
        isPhysicallyPressed = false
        pressStartTimeMs = nowMs
        releasePending = true
        if (velocity < 3.0f) {
            velocity += 3.0f
        }
    }

    fun isActive(): Boolean {
        return isPhysicallyPressed || releasePending || abs(displacement) > 0.002f || abs(velocity) > 0.004f
    }

    fun step(dtSeconds: Float, config: BezelConfig, nowMs: Long = SystemClock.uptimeMillis()): Boolean {
        val elapsedMs = nowMs - pressStartTimeMs
        if (releasePending && elapsedMs >= config.minPressHoldMs) {
            releasePending = false
        }

        val target = when {
            isPhysicallyPressed -> {
                // Smoothly ramp into hold boost after 150ms of sustained hold
                val sustainFraction = ((elapsedMs - 140L).coerceAtLeast(0L) / 240f).coerceIn(0f, 1f)
                1.0f + (config.holdBoostMultiplier - 1.0f) * sustainFraction
            }
            releasePending -> 1.0f
            else -> 0.0f
        }

        val clampedDt = dtSeconds.coerceIn(0.0005f, 0.033f)
        // Sub-step into 4 micro-steps for rock-solid high-stiffness accuracy
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
                // Prevent visual clipping behind the physical bezel edge
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

    fun reset() {
        displacement = 0f
        velocity = 0f
        isPhysicallyPressed = false
        releasePending = false
    }
}

class BezelSpringController {
    private val states = HardwareButtonType.entries.associateWith { ButtonSpringState(it) }

    fun stateFor(type: HardwareButtonType): ButtonSpringState = states.getValue(type)

    fun onButtonEvent(type: HardwareButtonType, isDown: Boolean, config: BezelConfig) {
        val state = states.getValue(type)
        if (isDown) {
            state.onPress()
        } else {
            state.onRelease(minHoldMs = config.minPressHoldMs)
        }
    }

    fun triggerPulse(type: HardwareButtonType) {
        states.getValue(type).triggerPulse()
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

    fun isAnyActive(): Boolean = states.values.any { it.isActive() }
}

/**
 * Computes a sampled spring curve over [durationMs] for live physics telemetry & visualization.
 */
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

/**
 * Zero-latency in-process event bus connecting BezelAccessibilityService, MainActivity,
 * and Compose live previews without coroutine dispatch overhead on the key-event hot path.
 */
object BezelRuntimeBus {
    fun interface ButtonListener {
        fun onHardwareButton(type: HardwareButtonType, isDown: Boolean, isPulseOnly: Boolean)
    }

    private val _config = MutableStateFlow(BezelConfig())
    val config: StateFlow<BezelConfig> = _config.asStateFlow()

    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()

    private val _lastEvent = MutableStateFlow<ButtonEventRecord?>(null)
    val lastEvent: StateFlow<ButtonEventRecord?> = _lastEvent.asStateFlow()

    private val listeners = CopyOnWriteArrayList<ButtonListener>()

    fun updateConfig(newConfig: BezelConfig) {
        _config.value = newConfig
    }

    fun setServiceConnected(connected: Boolean) {
        _serviceConnected.value = connected
    }

    fun registerListener(listener: ButtonListener) {
        listeners.addIfAbsent(listener)
    }

    fun unregisterListener(listener: ButtonListener) {
        listeners.remove(listener)
    }

    fun dispatchButtonEvent(
        type: HardwareButtonType,
        isDown: Boolean,
        sourceLabel: String,
        isPulseOnly: Boolean = false
    ) {
        val cfg = _config.value
        if (!cfg.masterEnabled) return
        if (!cfg.buttonConfig(type).enabled) return

        _lastEvent.value = ButtonEventRecord(
            button = type,
            isDown = isDown,
            timestampMs = System.currentTimeMillis(),
            sourceLabel = sourceLabel
        )
        for (listener in listeners) {
            listener.onHardwareButton(type, isDown, isPulseOnly)
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
            // Gracefully ignore if device restricts vibration
        }
    }
}

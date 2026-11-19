package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.Choreographer
import android.view.View
import com.example.engine.BezelGeometry
import com.example.engine.BezelRuntimeBus
import com.example.engine.BezelSpringController
import com.example.model.BezelSide
import com.example.model.BezelStyle

/**
 * Hardware-accelerated, zero-allocation overlay view driven directly by Display VSYNC
 * via Choreographer. Renders all configured built-in and custom buttons at 60/90/120Hz with 0% idle CPU.
 */
class BezelOverlayView(context: Context) : View(context), Choreographer.FrameCallback {

    private val springController = BezelSpringController()
    private val density = context.resources.displayMetrics.density

    private val fillPath = Path()
    private val rimPath = Path()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private var frameLoopRunning = false
    private var lastFrameNanos = 0L

    init {
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
    }

    fun onButtonEvent(
        buttonId: String,
        isDown: Boolean,
        isPulseOnly: Boolean = false,
        holdOverrideMs: Long? = null
    ) {
        val config = BezelRuntimeBus.config.value
        val button = config.buttonById(buttonId) ?: return
        if (!config.masterEnabled || !button.enabled) return

        if (isPulseOnly) {
            springController.triggerPulse(buttonId, holdOverrideMs)
        } else {
            springController.onButtonEvent(buttonId, isDown, config)
        }
        startFrameLoopIfNeeded()
    }

    private fun startFrameLoopIfNeeded() {
        if (!frameLoopRunning) {
            frameLoopRunning = true
            lastFrameNanos = System.nanoTime()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stopAnimationLoop() {
        frameLoopRunning = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!frameLoopRunning) return

        val dtSeconds = if (lastFrameNanos == 0L) {
            1f / 60f
        } else {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
        }
        lastFrameNanos = frameTimeNanos

        val config = BezelRuntimeBus.config.value
        val stillActive = springController.stepAll(dtSeconds, config)
        invalidate()

        if (stillActive) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            frameLoopRunning = false
            lastFrameNanos = 0L
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val config = BezelRuntimeBus.config.value
        if (!config.masterEnabled) return

        for (btn in config.buttons) {
            if (!btn.enabled) continue
            val state = springController.stateFor(btn.id)
            val displacement = state.displacement
            if (displacement <= 0.003f) continue

            val depthPx = BezelGeometry.computeDepthPx(displacement, config, density)
            val edgeX = if (btn.side == BezelSide.RIGHT) w else 0f
            val centerY = BezelGeometry.computeCenterY(btn, h)
            val lengthPx = btn.lengthDp * density

            BezelGeometry.buildAndroidPath(
                outPath = fillPath,
                rimPath = rimPath,
                style = config.style,
                side = btn.side,
                edgeX = edgeX,
                centerY = centerY,
                lengthPx = lengthPx,
                depthPx = depthPx,
                shoulderSpread = config.shoulderSpread,
                peakFlatness = config.peakFlatness
            )

            val visibilityAlpha = (displacement * 2.2f).coerceIn(0f, 1f)

            val fillAlpha = (config.fillOpacity * visibilityAlpha * 255f).toInt().coerceIn(0, 255)
            if (fillAlpha > 0) {
                fillPaint.color = withAlpha(config.fillColorArgb, fillAlpha)
                canvas.drawPath(fillPath, fillPaint)
            }

            if (config.rimHighlightEnabled || config.style == BezelStyle.EDGE_GLOW) {
                val baseRimAlpha = config.rimOpacity * visibilityAlpha
                if (config.rimGlowRadiusDp > 0.5f && baseRimAlpha > 0.02f) {
                    val glowPx = config.rimGlowRadiusDp * density
                    val passes = 3
                    for (p in passes downTo 1) {
                        val factor = p.toFloat() / passes
                        glowPaint.strokeWidth = config.rimWidthDp * density + glowPx * factor
                        val passAlpha = (baseRimAlpha * (0.20f / p) * 255f).toInt().coerceIn(0, 255)
                        glowPaint.color = withAlpha(config.rimColorArgb, passAlpha)
                        canvas.drawPath(rimPath, glowPaint)
                    }
                }

                rimPaint.strokeWidth = (config.rimWidthDp * density).coerceAtLeast(1f)
                rimPaint.color = withAlpha(
                    config.rimColorArgb,
                    (baseRimAlpha * 255f).toInt().coerceIn(0, 255)
                )
                canvas.drawPath(rimPath, rimPaint)
            }

            if (config.showMicroGlyph && displacement > 0.25f && depthPx >= 10f * density) {
                val glyphAlpha = (((displacement - 0.25f) / 0.65f).coerceIn(0f, 1f) * 235f).toInt()
                val gx = BezelGeometry.glyphCenterX(config.style, btn.side, edgeX, depthPx)
                glyphPaint.textSize = (depthPx * 0.52f).coerceIn(8f * density, 16f * density)
                glyphPaint.color = withAlpha(
                    if (config.rimHighlightEnabled) config.rimColorArgb else Color.WHITE,
                    glyphAlpha
                )
                val baseline = centerY - (glyphPaint.descent() + glyphPaint.ascent()) * 0.5f
                canvas.drawText(btn.glyph, gx, baseline, glyphPaint)
            }
        }
    }

    private fun withAlpha(argb: Int, alpha: Int): Int {
        return (argb and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
    }
}

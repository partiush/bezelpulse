package com.example.engine

import android.graphics.Path
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.ButtonConfig

/**
 * Shared Bezier geometry builder for both native Android Canvas (AccessibilityService overlay)
 * and Jetpack Compose Canvas (in-app live edge preview & style cards).
 *
 * The LIQUID_BULGE curve uses strict vertical tangents along the bezel edge (x = edgeX),
 * creating the authentic iOS 18 optical illusion that the physical black bezel itself is flexing.
 */
object BezelGeometry {

    fun buildAndroidPath(
        outPath: Path,
        rimPath: Path,
        style: BezelStyle,
        side: BezelSide,
        edgeX: Float,
        centerY: Float,
        lengthPx: Float,
        depthPx: Float,
        shoulderSpread: Float,
        peakFlatness: Float
    ) {
        outPath.reset()
        rimPath.reset()
        if (depthPx <= 0.35f) return

        val dir = if (side == BezelSide.RIGHT) -1f else 1f
        val peakX = edgeX + dir * depthPx

        when (style) {
            BezelStyle.LIQUID_BULGE -> {
                val halfCore = lengthPx * 0.5f
                val shoulderExt = lengthPx * (0.30f + shoulderSpread * 0.65f)
                val topY = centerY - halfCore - shoulderExt
                val bottomY = centerY + halfCore + shoulderExt
                val crownHalf = (halfCore * (0.12f + peakFlatness * 0.68f)).coerceAtMost(halfCore * 0.85f)
                val crownTopY = centerY - crownHalf
                val crownBottomY = centerY + crownHalf

                val topSpan = crownTopY - topY
                val bottomSpan = bottomY - crownBottomY

                // Rim open stroke path (from top bezel anchor -> crown -> bottom bezel anchor)
                rimPath.moveTo(edgeX, topY)
                rimPath.cubicTo(
                    edgeX, topY + topSpan * 0.52f,
                    peakX, crownTopY - topSpan * 0.42f,
                    peakX, crownTopY
                )
                if (crownBottomY > crownTopY + 0.5f) {
                    rimPath.lineTo(peakX, crownBottomY)
                }
                rimPath.cubicTo(
                    peakX, crownBottomY + bottomSpan * 0.42f,
                    edgeX, bottomY - bottomSpan * 0.52f,
                    edgeX, bottomY
                )

                // Closed fill path fused flush to bezel edge (slightly tucked 2px past edge to eliminate anti-alias seam)
                val tuckX = edgeX - dir * 2f
                outPath.moveTo(tuckX, topY)
                outPath.lineTo(edgeX, topY)
                outPath.cubicTo(
                    edgeX, topY + topSpan * 0.52f,
                    peakX, crownTopY - topSpan * 0.42f,
                    peakX, crownTopY
                )
                if (crownBottomY > crownTopY + 0.5f) {
                    outPath.lineTo(peakX, crownBottomY)
                }
                outPath.cubicTo(
                    peakX, crownBottomY + bottomSpan * 0.42f,
                    edgeX, bottomY - bottomSpan * 0.52f,
                    edgeX, bottomY
                )
                outPath.lineTo(tuckX, bottomY)
                outPath.close()
            }

            BezelStyle.TACTILE_PILL -> {
                val pillWidth = (depthPx * 0.62f).coerceAtLeast(0.5f)
                val gap = depthPx * 0.34f
                val halfH = lengthPx * (0.46f + shoulderSpread * 0.14f)
                val radius = (pillWidth * 0.5f).coerceAtMost(halfH * 0.5f)

                val left = if (side == BezelSide.RIGHT) edgeX - gap - pillWidth else edgeX + gap
                val right = left + pillWidth
                val top = centerY - halfH
                val bottom = centerY + halfH

                outPath.addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW)
                rimPath.addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW)
            }

            BezelStyle.EDGE_GLOW -> {
                val halfSpan = lengthPx * (0.58f + shoulderSpread * 0.42f)
                val topY = centerY - halfSpan
                val bottomY = centerY + halfSpan
                val arcDepth = depthPx * 0.68f
                val arcPeakX = edgeX + dir * arcDepth
                val crownHalf = (lengthPx * (0.14f + peakFlatness * 0.34f)).coerceAtMost(halfSpan * 0.72f)
                val crownTopY = centerY - crownHalf
                val crownBottomY = centerY + crownHalf
                val topSpan = crownTopY - topY
                val bottomSpan = bottomY - crownBottomY

                rimPath.moveTo(edgeX, topY)
                rimPath.cubicTo(
                    edgeX, topY + topSpan * 0.55f,
                    arcPeakX, crownTopY - topSpan * 0.35f,
                    arcPeakX, crownTopY
                )
                if (crownBottomY > crownTopY + 0.5f) {
                    rimPath.lineTo(arcPeakX, crownBottomY)
                }
                rimPath.cubicTo(
                    arcPeakX, crownBottomY + bottomSpan * 0.35f,
                    edgeX, bottomY - bottomSpan * 0.55f,
                    edgeX, bottomY
                )

                val tuckX = edgeX - dir * 2f
                outPath.moveTo(tuckX, topY)
                outPath.lineTo(edgeX, topY)
                outPath.cubicTo(
                    edgeX, topY + topSpan * 0.55f,
                    arcPeakX, crownTopY - topSpan * 0.35f,
                    arcPeakX, crownTopY
                )
                if (crownBottomY > crownTopY + 0.5f) {
                    outPath.lineTo(arcPeakX, crownBottomY)
                }
                outPath.cubicTo(
                    arcPeakX, crownBottomY + bottomSpan * 0.35f,
                    edgeX, bottomY - bottomSpan * 0.55f,
                    edgeX, bottomY
                )
                outPath.lineTo(tuckX, bottomY)
                outPath.close()
            }

            BezelStyle.MAGNETIC_WAVE -> {
                val halfSpan = lengthPx * (0.75f + shoulderSpread * 0.85f)
                val topY = centerY - halfSpan
                val bottomY = centerY + halfSpan
                val crownHalf = lengthPx * (0.06f + peakFlatness * 0.25f)
                val crownTopY = centerY - crownHalf
                val crownBottomY = centerY + crownHalf

                val topSpan = crownTopY - topY
                val bottomSpan = bottomY - crownBottomY

                rimPath.moveTo(edgeX, topY)
                rimPath.cubicTo(
                    edgeX, topY + topSpan * 0.64f,
                    peakX, crownTopY - topSpan * 0.36f,
                    peakX, crownTopY
                )
                if (crownBottomY > crownTopY + 0.5f) {
                    rimPath.lineTo(peakX, crownBottomY)
                }
                rimPath.cubicTo(
                    peakX, crownBottomY + bottomSpan * 0.36f,
                    edgeX, bottomY - bottomSpan * 0.64f,
                    edgeX, bottomY
                )

                val tuckX = edgeX - dir * 2f
                outPath.moveTo(tuckX, topY)
                outPath.lineTo(edgeX, topY)
                outPath.cubicTo(
                    edgeX, topY + topSpan * 0.64f,
                    peakX, crownTopY - topSpan * 0.36f,
                    peakX, crownTopY
                )
                if (crownBottomY > crownTopY + 0.5f) {
                    outPath.lineTo(peakX, crownBottomY)
                }
                outPath.cubicTo(
                    peakX, crownBottomY + bottomSpan * 0.36f,
                    edgeX, bottomY - bottomSpan * 0.64f,
                    edgeX, bottomY
                )
                outPath.lineTo(tuckX, bottomY)
                outPath.close()
            }
        }
    }

    fun glyphCenterX(
        style: BezelStyle,
        side: BezelSide,
        edgeX: Float,
        depthPx: Float
    ): Float {
        val dir = if (side == BezelSide.RIGHT) -1f else 1f
        return when (style) {
            BezelStyle.TACTILE_PILL -> edgeX + dir * (depthPx * 0.69f)
            else -> edgeX + dir * (depthPx * 0.48f)
        }
    }

    fun computeDepthPx(
        displacement: Float,
        config: BezelConfig,
        density: Float
    ): Float = (config.maxDepthDp * density * displacement).coerceAtLeast(0f)

    fun computeCenterY(button: ButtonConfig, viewHeightPx: Float): Float =
        viewHeightPx * button.verticalPositionFraction.coerceIn(0.05f, 0.95f)
}

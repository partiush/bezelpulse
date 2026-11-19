package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.engine.SpringAnalyzer
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PlasmaCoral
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan

/**
 * Live spring displacement response oscilloscope showing exact x(t) trajectory,
 * overshoot percentage, settling duration, and damping regime.
 */
@Composable
fun SpringCurveGraph(
    stiffness: Float,
    dampingRatio: Float,
    modifier: Modifier = Modifier
) {
    val telemetry = remember(stiffness, dampingRatio) {
        SpringAnalyzer.analyze(stiffness, dampingRatio)
    }

    val regimeColor = when {
        dampingRatio < 0.65f -> PlasmaCoral
        dampingRatio < 0.88f -> TitaniumCyan
        dampingRatio <= 1.02f -> EmeraldActive
        else -> SignalAmber
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spring Response Curve",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time 120Hz step response x(t)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = regimeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, regimeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = telemetry.regimeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = regimeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 12.dp.toPx()
                val bottomPad = 14.dp.toPx()
                val plotH = h - topPad - bottomPad

                // Vertical range maps [0.0 .. 1.45]
                val maxYVal = 1.45f
                fun mapY(v: Float): Float = (topPad + plotH * (1f - (v / maxYVal))).coerceIn(2f, h - 2f)

                // Subtle grid lines
                for (i in 1..3) {
                    val x = w * (i / 4f)
                    drawLine(
                        color = HairlineBorder.copy(alpha = 0.55f),
                        start = Offset(x, topPad),
                        end = Offset(x, h - bottomPad),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Target 1.0 dashed reference line
                val targetY = mapY(1.0f)
                drawLine(
                    color = TextMuted.copy(alpha = 0.65f),
                    start = Offset(0f, targetY),
                    end = Offset(w, targetY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )

                // Baseline 0.0
                val baseY = mapY(0f)
                drawLine(
                    color = HairlineBorder,
                    start = Offset(0f, baseY),
                    end = Offset(w, baseY),
                    strokeWidth = 1.dp.toPx()
                )

                val samples = telemetry.samples
                if (samples.size > 1) {
                    val linePath = Path()
                    val fillPath = Path()
                    val stepX = w / (samples.size - 1)

                    samples.forEachIndexed { idx, value ->
                        val px = idx * stepX
                        val py = mapY(value)
                        if (idx == 0) {
                            linePath.moveTo(px, py)
                            fillPath.moveTo(px, baseY)
                            fillPath.lineTo(px, py)
                        } else {
                            linePath.lineTo(px, py)
                            fillPath.lineTo(px, py)
                        }
                    }
                    fillPath.lineTo(w, baseY)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                regimeColor.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            startY = topPad,
                            endY = baseY
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = regimeColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryChip(
                    label = "SETTLE",
                    value = "${telemetry.settlingTimeMs} ms",
                    modifier = Modifier.weight(1f)
                )
                TelemetryChip(
                    label = "OVERSHOOT",
                    value = "${telemetry.peakOvershootPercent}%",
                    modifier = Modifier.weight(1f)
                )
                TelemetryChip(
                    label = "DAMPING",
                    value = String.format("%.2f ζ", dampingRatio),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianElevated
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

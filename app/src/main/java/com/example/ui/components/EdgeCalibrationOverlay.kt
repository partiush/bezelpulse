package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.ButtonConfig
import com.example.model.HardwareButtonType
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import kotlin.math.roundToInt

/**
 * Full-screen interactive bezel alignment overlay.
 * Lets users drag each button handle directly along the left/right screen edge until
 * it lines up millimeter-perfectly with their physical hardware buttons.
 */
@Composable
fun EdgeCalibrationOverlay(
    config: BezelConfig,
    onPositionChange: (HardwareButtonType, Float) -> Unit,
    onSideFlip: (HardwareButtonType, BezelSide) -> Unit,
    onTriggerPulse: (HardwareButtonType) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDone() }

    var activeDragButton by remember { mutableStateOf<HardwareButtonType?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.88f))
    ) {
        val density = LocalDensity.current
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)

        // Precision ruler tick marks along both left & right physical edges
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tickCount = 40
            val stepY = size.height / tickCount
            for (i in 1 until tickCount) {
                val y = i * stepY
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 14.dp.toPx() else 6.dp.toPx()
                val alpha = if (isMajor) 0.35f else 0.14f
                val color = TitaniumCyan.copy(alpha = alpha)

                // Left bezel ticks
                drawLine(color, Offset(0f, y), Offset(tickLen, y), strokeWidth = 1.dp.toPx())
                // Right bezel ticks
                drawLine(
                    color,
                    Offset(size.width - tickLen, y),
                    Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Horizontal alignment guide line for currently dragged button
            activeDragButton?.let { btnType ->
                val btnCfg = config.buttonConfig(btnType)
                val y = size.height * btnCfg.verticalPositionFraction
                drawLine(
                    color = TitaniumCyan.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Center HUD instruction card
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 44.dp)
                .widthIn(max = 320.dp),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianElevated.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, HairlineBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.TouchApp,
                    contentDescription = null,
                    tint = TitaniumCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Bezel Edge Calibration",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.calibration_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Tip: Press your real volume buttons to verify alignment live.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TitaniumCyan,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("calibration_done_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TitaniumCyan,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.calibration_done),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Draggable edge alignment handles for each enabled button
        config.allButtons().filter { it.enabled }.forEach { btn ->
            CalibrationEdgeHandle(
                button = btn,
                containerHeightPx = heightPx,
                containerWidthPx = widthPx,
                isDragging = activeDragButton == btn.type,
                onDragStart = {
                    activeDragButton = btn.type
                    onTriggerPulse(btn.type)
                },
                onDragEnd = {
                    activeDragButton = null
                    onTriggerPulse(btn.type)
                },
                onDeltaY = { dyPx ->
                    val current = config.buttonConfig(btn.type).verticalPositionFraction
                    val updated = (current + dyPx / heightPx).coerceIn(0.06f, 0.94f)
                    onPositionChange(btn.type, updated)
                },
                onFlipSide = {
                    val newSide = if (btn.side == BezelSide.RIGHT) BezelSide.LEFT else BezelSide.RIGHT
                    onSideFlip(btn.type, newSide)
                }
            )
        }
    }
}

@Composable
private fun CalibrationEdgeHandle(
    button: ButtonConfig,
    containerHeightPx: Float,
    containerWidthPx: Float,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDeltaY: (Float) -> Unit,
    onFlipSide: () -> Unit
) {
    val density = LocalDensity.current
    val accent = when (button.type) {
        HardwareButtonType.VOLUME_UP -> TitaniumCyan
        HardwareButtonType.VOLUME_DOWN -> EmeraldActive
        HardwareButtonType.POWER -> SignalAmber
    }

    val handleWidthDp = 114.dp
    val handleHeightDp = button.lengthDp.coerceIn(48f, 110f).dp
    val handleWidthPx = with(density) { handleWidthDp.toPx() }
    val handleHeightPx = with(density) { handleHeightDp.toPx() }

    val centerY = containerHeightPx * button.verticalPositionFraction
    val topY = (centerY - handleHeightPx / 2f).coerceIn(0f, (containerHeightPx - handleHeightPx).coerceAtLeast(0f))
    val leftX = if (button.side == BezelSide.LEFT) 0f else (containerWidthPx - handleWidthPx).coerceAtLeast(0f)

    val shape = if (button.side == BezelSide.LEFT) {
        RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
    }

    Surface(
        modifier = Modifier
            .offset { IntOffset(leftX.roundToInt(), topY.roundToInt()) }
            .size(width = handleWidthDp, height = handleHeightDp)
            .pointerInput(button.type) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDeltaY(dragAmount.y)
                    }
                )
            }
            .testTag("calibration_handle_${button.type.name.lowercase()}"),
        shape = shape,
        color = if (isDragging) accent.copy(alpha = 0.28f) else ObsidianElevated.copy(alpha = 0.92f),
        border = BorderStroke(if (isDragging) 2.dp else 1.dp, accent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (button.side == BezelSide.LEFT) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(accent, RoundedCornerShape(2.dp))
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = button.type.shortLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${(button.verticalPositionFraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
            }

            Surface(
                onClick = onFlipSide,
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Flip bezel side",
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (button.side == BezelSide.RIGHT) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(accent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

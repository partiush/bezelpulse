package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.engine.BezelGeometry
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.ButtonConfig
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Rock-solid, zero-jitter full-screen edge calibration overlay.
 *
 * All touch tracking happens on a single NON-MOVING full-screen coordinate layer so moving
 * visual handles never shift the gesture coordinate system or suffer from stale lambda closures.
 */
@Composable
fun EdgeCalibrationOverlay(
    config: BezelConfig,
    onPositionChange: (String, Float) -> Unit,
    onSideFlip: (String, BezelSide) -> Unit,
    onLengthChange: (String, Float) -> Unit,
    onTriggerPulse: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDone() }

    val enabledButtons = remember(config.buttons) { config.buttons.filter { it.enabled } }
    var selectedButtonId by remember(enabledButtons) {
        mutableStateOf(enabledButtons.firstOrNull()?.id ?: "")
    }
    var draggingButtonId by remember { mutableStateOf<String?>(null) }

    // Always keep fresh references for the fixed-coordinate pointerInput loop
    val latestButtons by rememberUpdatedState(enabledButtons)
    val latestOnPositionChange by rememberUpdatedState(onPositionChange)
    val latestOnTriggerPulse by rememberUpdatedState(onTriggerPulse)

    val fillPath = remember { android.graphics.Path() }
    val rimPath = remember { android.graphics.Path() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid.copy(alpha = 0.86f))
    ) {
        val density = LocalDensity.current
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val grabZoneWidthPx = with(density) { 128.dp.toPx() }
        val grabHalfHeightPaddingPx = with(density) { 26.dp.toPx() }

        // Fixed-coordinate full-screen gesture layer: ZERO jitter, exact 1:1 finger tracking!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(heightPx, widthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downX = down.position.x
                        val downY = down.position.y

                        // Pick the closest button handle within the left/right grab strip
                        val hit = latestButtons
                            .filter { btn ->
                                val inSideStrip = if (btn.side == BezelSide.LEFT) {
                                    downX <= grabZoneWidthPx
                                } else {
                                    downX >= widthPx - grabZoneWidthPx
                                }
                                if (!inSideStrip) return@filter false
                                val btnCenterY = heightPx * btn.verticalPositionFraction
                                val halfLen = (btn.lengthDp * density.density * 0.5f) + grabHalfHeightPaddingPx
                                abs(downY - btnCenterY) <= halfLen
                            }
                            .minByOrNull { abs(downY - heightPx * it.verticalPositionFraction) }

                        if (hit == null) return@awaitEachGesture

                        down.consume()
                        selectedButtonId = hit.id
                        draggingButtonId = hit.id
                        val grabOffsetY = downY - (heightPx * hit.verticalPositionFraction)
                        latestOnTriggerPulse(hit.id)

                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                                val targetCenterY = change.position.y - grabOffsetY
                                val newFraction = (targetCenterY / heightPx).coerceIn(0.05f, 0.95f)
                                latestOnPositionChange(hit.id, newFraction)
                            }
                        }

                        draggingButtonId = null
                        latestOnTriggerPulse(hit.id)
                    }
                }
        ) {
            // Precision bezel ruler + live static bezel curve preview for every enabled button
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tickCount = 40
                val stepY = size.height / tickCount
                for (i in 1 until tickCount) {
                    val y = i * stepY
                    val isMajor = i % 5 == 0
                    val tickLen = if (isMajor) 14.dp.toPx() else 6.dp.toPx()
                    val color = TitaniumCyan.copy(alpha = if (isMajor) 0.34f else 0.14f)
                    drawLine(color, Offset(0f, y), Offset(tickLen, y), strokeWidth = 1.dp.toPx())
                    drawLine(
                        color,
                        Offset(size.width - tickLen, y),
                        Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw live shape silhouette right on the screen edge for each button
                enabledButtons.forEach { btn ->
                    val isSelected = btn.id == selectedButtonId
                    val isDragging = btn.id == draggingButtonId
                    val accent = Color(btn.accentColorArgb)
                    val centerY = size.height * btn.verticalPositionFraction
                    val edgeX = if (btn.side == BezelSide.RIGHT) size.width else 0f
                    val depthPx = config.maxDepthDp.dp.toPx() * (if (isDragging) 1.05f else 0.72f)
                    val lengthPx = btn.lengthDp.dp.toPx()

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

                    val composeFill = fillPath.asComposePath()
                    val composeRim = rimPath.asComposePath()
                    drawPath(composeFill, color = accent.copy(alpha = if (isDragging) 0.34f else 0.18f))
                    drawPath(
                        composeRim,
                        color = accent.copy(alpha = if (isSelected || isDragging) 0.95f else 0.55f),
                        style = Stroke(width = if (isDragging) 2.5.dp.toPx() else 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Horizontal alignment crosshair line for selected/dragged button
                    if (isSelected || isDragging) {
                        drawLine(
                            color = accent.copy(alpha = if (isDragging) 0.72f else 0.32f),
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }
                }
            }

            // Visual edge handles (non-clickable so all drags go cleanly through the fixed parent layer)
            enabledButtons.forEach { btn ->
                CalibrationEdgeBadge(
                    button = btn,
                    containerHeightPx = heightPx,
                    containerWidthPx = widthPx,
                    isSelected = btn.id == selectedButtonId,
                    isDragging = btn.id == draggingButtonId
                )
            }
        }

        // Top compact instruction pill (safely below status bar, never blocks side edges)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 56.dp, end = 56.dp),
            shape = RoundedCornerShape(50),
            color = ObsidianSurface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, HairlineBorder)
        ) {
            Text(
                text = stringResource(R.string.calibration_hint),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }

        // Bottom precision fine-tune control dock (safely above nav bar)
        val activeBtn = enabledButtons.firstOrNull { it.id == selectedButtonId } ?: enabledButtons.firstOrNull()
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, HairlineBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeBtn != null) {
                    val accent = Color(activeBtn.accentColorArgb)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeBtn.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${activeBtn.side.label} edge · ${(activeBtn.verticalPositionFraction * 100).roundToInt()}% · ${activeBtn.lengthDp.roundToInt()}dp",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }

                        // Side flip button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val newSide = if (activeBtn.side == BezelSide.RIGHT) BezelSide.LEFT else BezelSide.RIGHT
                                    onSideFlip(activeBtn.id, newSide)
                                }
                                .testTag("calibration_flip_side"),
                            shape = RoundedCornerShape(12.dp),
                            color = accent.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = "Switch left or right edge",
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Flip Side",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                        }
                    }

                    // Micro-nudge position & span controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalibrationNudgeButton(
                            label = "Up",
                            icon = Icons.Rounded.KeyboardArrowUp,
                            onClick = {
                                onPositionChange(activeBtn.id, (activeBtn.verticalPositionFraction - 0.01f).coerceAtLeast(0.05f))
                                onTriggerPulse(activeBtn.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationNudgeButton(
                            label = "Down",
                            icon = Icons.Rounded.KeyboardArrowDown,
                            onClick = {
                                onPositionChange(activeBtn.id, (activeBtn.verticalPositionFraction + 0.01f).coerceAtMost(0.95f))
                                onTriggerPulse(activeBtn.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationNudgeButton(
                            label = "Shorter",
                            icon = Icons.Rounded.Remove,
                            onClick = {
                                onLengthChange(activeBtn.id, (activeBtn.lengthDp - 6f).coerceAtLeast(36f))
                                onTriggerPulse(activeBtn.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CalibrationNudgeButton(
                            label = "Longer",
                            icon = Icons.Rounded.Add,
                            onClick = {
                                onLengthChange(activeBtn.id, (activeBtn.lengthDp + 6f).coerceAtMost(140f))
                                onTriggerPulse(activeBtn.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

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
    }
}

@Composable
private fun CalibrationEdgeBadge(
    button: ButtonConfig,
    containerHeightPx: Float,
    containerWidthPx: Float,
    isSelected: Boolean,
    isDragging: Boolean
) {
    val density = LocalDensity.current
    val accent = Color(button.accentColorArgb)

    val handleWidthDp = 96.dp
    val handleHeightDp = button.lengthDp.coerceIn(48f, 108f).dp
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
            .testTag("calibration_handle_${button.id}"),
        shape = shape,
        color = when {
            isDragging -> accent.copy(alpha = 0.28f)
            isSelected -> ObsidianElevated.copy(alpha = 0.94f)
            else -> ObsidianSurface.copy(alpha = 0.85f)
        },
        border = BorderStroke(
            width = if (isDragging || isSelected) 2.dp else 1.dp,
            color = if (isDragging || isSelected) accent else accent.copy(alpha = 0.55f)
        )
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

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = button.shortLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${(button.verticalPositionFraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
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

@Composable
private fun CalibrationNudgeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianElevated,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

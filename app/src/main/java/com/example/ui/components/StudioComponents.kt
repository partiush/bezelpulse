package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.engine.BezelGeometry
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCardHighlight
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import com.example.ui.theme.TitaniumCyanContainer

@Composable
fun StudioSectionCard(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun PrecisionSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    hint: String? = null,
    accentColor: Color = TitaniumCyan,
    testTagName: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ObsidianElevated
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (hint != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = ObsidianCardHighlight
            )
        )
    }
}

@Composable
fun PrecisionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTagName: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTagName != null) Modifier.testTag(testTagName) else Modifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = TitaniumCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ObsidianCardHighlight,
                uncheckedBorderColor = HairlineBorder
            )
        )
    }
}

/**
 * Press-and-hold hardware simulator pad button: triggers immediate onDown on touch start
 * and onUp on release/cancel so users experience true hold-boost physics.
 */
@Composable
fun TactileHoldButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onPressChange: (Boolean) -> Unit,
    testTagName: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .height(68.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Test $label bezel animation"
            }
            .pointerInput(label) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onPressChange(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onPressChange(false)
                }
            }
            .testTag(testTagName),
        shape = RoundedCornerShape(18.dp),
        color = if (isPressed) accentColor.copy(alpha = 0.24f) else ObsidianElevated,
        border = BorderStroke(
            width = if (isPressed) 1.5.dp else 1.dp,
            color = if (isPressed) accentColor else HairlineBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPressed) accentColor else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isPressed) accentColor else TextSecondary
            )
        }
    }
}

/**
 * Miniature Canvas rendering each BezelStyle with exact BezelGeometry curves.
 */
@Composable
fun BezelStyleMiniPreview(
    style: BezelStyle,
    fillArgb: Int,
    rimArgb: Int,
    modifier: Modifier = Modifier
) {
    val fillPath = remember { android.graphics.Path() }
    val rimPath = remember { android.graphics.Path() }

    Canvas(modifier = modifier) {
        // Subtle simulated screen frame background
        drawRect(color = Color(0xFF10131A))

        BezelGeometry.buildAndroidPath(
            outPath = fillPath,
            rimPath = rimPath,
            style = style,
            side = BezelSide.RIGHT,
            edgeX = size.width,
            centerY = size.height * 0.5f,
            lengthPx = size.height * 0.42f,
            depthPx = size.width * 0.30f,
            shoulderSpread = 0.6f,
            peakFlatness = 0.34f
        )

        val composeFill = fillPath.asComposePath()
        val composeRim = rimPath.asComposePath()

        if (style != BezelStyle.EDGE_GLOW) {
            drawPath(composeFill, color = Color(fillArgb))
        }
        drawPath(
            composeRim,
            color = Color(rimArgb).copy(alpha = 0.30f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            composeRim,
            color = Color(rimArgb),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private val CuratedSwatches = listOf(
    0xFF000000.toInt(), // True iOS Pitch Obsidian
    0xFF141821.toInt(), // Titanium Slate
    0xFFFFFFFF.toInt(), // Pure Ceramic
    0xFF38BDF8.toInt(), // Electric Cyan
    0xFF10B981.toInt(), // Emerald Pulse
    0xFFF59E0B.toInt(), // Signal Amber
    0xFFFB7185.toInt(), // Plasma Coral
    0xFFA855F7.toInt(), // Electric Violet
    0xFF3B82F6.toInt(), // Cobalt Blue
    0xFFEF4444.toInt()  // Crimson Laser
)

@Composable
fun ColorPalettePicker(
    title: String,
    selectedArgb: Int,
    onColorSelected: (Int) -> Unit,
    onColorFinished: (() -> Unit)? = null
) {
    var showCustomHsv by remember { mutableStateOf(false) }

    val hsvInitial = remember(selectedArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(selectedArgb, it) }
    }
    var hue by remember(selectedArgb) { mutableFloatStateOf(hsvInitial[0]) }
    var sat by remember(selectedArgb) { mutableFloatStateOf(hsvInitial[1]) }
    var brightness by remember(selectedArgb) { mutableFloatStateOf(hsvInitial[2]) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(selectedArgb))
                        .border(1.dp, TextSecondary.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Surface(
                onClick = { showCustomHsv = !showCustomHsv },
                shape = RoundedCornerShape(10.dp),
                color = if (showCustomHsv) TitaniumCyanContainer else ObsidianElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Custom HSV color mixer",
                        tint = if (showCustomHsv) TitaniumCyan else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (showCustomHsv) "Hide Mixer" else "HSV Mixer",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (showCustomHsv) TitaniumCyan else TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(CuratedSwatches) { swatchArgb ->
                val isSelected = (swatchArgb and 0xFFFFFF) == (selectedArgb and 0xFFFFFF)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(swatchArgb))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) TitaniumCyan else HairlineBorder,
                            shape = CircleShape
                        )
                        .clickable {
                            onColorSelected(swatchArgb)
                            onColorFinished?.invoke()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        val isLight = brightnessOf(swatchArgb) > 0.65f
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected color",
                            tint = if (isLight) ObsidianVoid else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = showCustomHsv) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianElevated)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Hue rainbow bar indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            )
                        )
                )
                PrecisionSliderRow(
                    label = "Hue",
                    valueText = "${hue.toInt()}°",
                    value = hue,
                    valueRange = 0f..360f,
                    onValueChange = {
                        hue = it
                        onColorSelected(AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness)))
                    },
                    onValueChangeFinished = onColorFinished
                )
                PrecisionSliderRow(
                    label = "Saturation",
                    valueText = "${(sat * 100).toInt()}%",
                    value = sat,
                    valueRange = 0f..1f,
                    onValueChange = {
                        sat = it
                        onColorSelected(AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness)))
                    },
                    onValueChangeFinished = onColorFinished
                )
                PrecisionSliderRow(
                    label = "Brightness",
                    valueText = "${(brightness * 100).toInt()}%",
                    value = brightness,
                    valueRange = 0f..1f,
                    onValueChange = {
                        brightness = it
                        onColorSelected(AndroidColor.HSVToColor(floatArrayOf(hue, sat, brightness)))
                    },
                    onValueChangeFinished = onColorFinished
                )
            }
        }
    }
}

private fun brightnessOf(argb: Int): Float {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return 0.299f * r + 0.587f * g + 0.114f * b
}

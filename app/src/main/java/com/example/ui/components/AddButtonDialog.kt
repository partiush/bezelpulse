package com.example.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.BezelRuntimeBus
import com.example.model.BezelSide
import com.example.model.HardwareKeyCatalog
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianCardHighlight
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PlasmaCoral
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumCyan
import com.example.ui.theme.TitaniumCyanContainer

private val ButtonAccentOptions = listOf(
    0xFF38BDF8.toInt(),
    0xFFA855F7.toInt(),
    0xFFFB7185.toInt(),
    0xFF10B981.toInt(),
    0xFFF59E0B.toInt(),
    0xFF60A5FA.toInt()
)

@Composable
fun AddButtonDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, keyCode: Int, glyph: String, side: BezelSide, accentArgb: Int) -> Unit
) {
    var name by remember { mutableStateOf("Action Key") }
    var selectedKeyCode by remember { mutableIntStateOf(KeyEvent.KEYCODE_CAMERA) }
    var selectedGlyph by remember { mutableStateOf("⚡") }
    var selectedSide by remember { mutableStateOf(BezelSide.LEFT) }
    var selectedAccent by remember { mutableIntStateOf(ButtonAccentOptions[1]) }
    var isListeningForKey by remember { mutableStateOf(false) }

    // Register live hardware key capture while "Listen for Physical Key" is active
    DisposableEffect(isListeningForKey) {
        if (isListeningForKey) {
            BezelRuntimeBus.onKeyCaptureListener = { capturedCode ->
                selectedKeyCode = capturedCode
                if (name == "Action Key" || name.isBlank()) {
                    name = HardwareKeyCatalog.keyCodeDisplayName(capturedCode)
                }
                isListeningForKey = false
            }
        }
        onDispose {
            BezelRuntimeBus.onKeyCaptureListener = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Add Hardware Button",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(22) },
                    label = { Text("Button Label") },
                    placeholder = { Text("e.g. Camera, AI Key, Trigger L") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_button_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TitaniumCyan,
                        unfocusedBorderColor = HairlineBorder,
                        focusedLabelColor = TitaniumCyan,
                        cursorColor = TitaniumCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Live Hardware Key Capture Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isListeningForKey = !isListeningForKey }
                        .testTag("capture_hardware_key_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isListeningForKey) TitaniumCyanContainer else ObsidianElevated,
                    border = BorderStroke(
                        width = if (isListeningForKey) 1.5.dp else 1.dp,
                        color = if (isListeningForKey) TitaniumCyan else HairlineBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sensors,
                            contentDescription = null,
                            tint = if (isListeningForKey) TitaniumCyan else SignalAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isListeningForKey) "Press any hardware button now..."
                                else "Mapped Key: ${HardwareKeyCatalog.keyCodeDisplayName(selectedKeyCode)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isListeningForKey) TitaniumCyan else TextPrimary
                            )
                            Text(
                                text = if (isListeningForKey) "Listening for Android KeyEvent (tap to cancel)"
                                else "KeyCode #$selectedKeyCode · Tap to auto-detect physical key",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Quick Key Presets
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Or pick a hardware key preset",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(HardwareKeyCatalog.presets, key = { it.keyCode }) { preset ->
                            val isSelected = selectedKeyCode == preset.keyCode
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        isListeningForKey = false
                                        selectedKeyCode = preset.keyCode
                                        name = preset.label
                                        selectedGlyph = preset.defaultGlyph
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) TitaniumCyanContainer else ObsidianElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TitaniumCyan else HairlineBorder
                                )
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) TitaniumCyan else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Bezel Side Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Bezel Edge",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BezelSide.entries.forEach { side ->
                            val isSelected = selectedSide == side
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedSide = side },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ObsidianCardHighlight else ObsidianElevated,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) TitaniumCyan else HairlineBorder
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${side.label} Edge",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) TitaniumCyan else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Micro-Glyph Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Micro-Glyph",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(HardwareKeyCatalog.customGlyphs) { glyph ->
                            val isSelected = selectedGlyph == glyph
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedGlyph = glyph },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) TitaniumCyanContainer else ObsidianElevated,
                                border = BorderStroke(1.dp, if (isSelected) TitaniumCyan else HairlineBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = glyph,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) TitaniumCyan else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Indicator Color Swatches
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Handle Indicator Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ButtonAccentOptions.forEach { argb ->
                            val isSelected = selectedAccent == argb
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else HairlineBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAccent = argb },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected indicator color",
                                        tint = ObsidianVoid,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, selectedKeyCode, selectedGlyph, selectedSide, selectedAccent)
                },
                modifier = Modifier.testTag("confirm_add_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TitaniumCyan,
                    contentColor = ObsidianVoid
                )
            ) {
                Text("Add Button")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

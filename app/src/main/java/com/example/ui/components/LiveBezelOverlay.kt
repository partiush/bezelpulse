package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.BezelRuntimeBus
import com.example.service.BezelOverlayView

/**
 * Renders the exact same hardware-accelerated Choreographer-driven BezelOverlayView
 * inside MainActivity when the system-wide AccessibilityService is not yet running,
 * guaranteeing instant zero-latency edge animations right on first launch with no double-draw.
 */
@Composable
fun InAppBezelOverlay(
    isSystemServiceRunning: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSystemServiceRunning) return

    val context = LocalContext.current
    val overlayView = remember(context) { BezelOverlayView(context) }

    DisposableEffect(overlayView) {
        val listener = BezelRuntimeBus.ButtonListener { buttonId, isDown, isPulseOnly, holdOverrideMs ->
            overlayView.post {
                overlayView.onButtonEvent(buttonId, isDown, isPulseOnly, holdOverrideMs)
            }
        }
        BezelRuntimeBus.registerListener(listener)
        onDispose {
            BezelRuntimeBus.unregisterListener(listener)
            overlayView.stopAnimationLoop()
        }
    }

    AndroidView(
        factory = { overlayView },
        modifier = modifier.fillMaxSize()
    )
}

package com.example

import android.content.Context
import android.graphics.Path
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.data.BezelConfigCodec
import com.example.engine.BezelGeometry
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.ButtonConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun appNameMatchesResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("BezelPulse", context.getString(R.string.app_name))
    }

    @Test
    fun configJsonExportAndImportPreservesCustomButtonsAndPhysics() {
        val customBtn = ButtonConfig(
            id = "custom_cam",
            name = "Camera",
            shortLabel = "Cam",
            glyph = "◎",
            keyCode = KeyEvent.KEYCODE_CAMERA,
            isBuiltIn = false,
            side = BezelSide.RIGHT,
            verticalPositionFraction = 0.78f,
            lengthDp = 54f
        )
        val original = BezelConfig(
            style = BezelStyle.MAGNETIC_WAVE,
            maxDepthDp = 22f,
            stiffness = 640f,
            dampingRatio = 0.72f,
            pauseInLandscape = false
        ).addButton(customBtn)

        val json = BezelConfigCodec.exportConfigJson(original)
        val imported = BezelConfigCodec.importConfigJson(json, BezelConfig()).getOrThrow()

        assertEquals(BezelStyle.MAGNETIC_WAVE, imported.style)
        assertEquals(22f, imported.maxDepthDp, 0.01f)
        assertEquals(640f, imported.stiffness, 0.01f)
        assertEquals(0.72f, imported.dampingRatio, 0.01f)
        assertEquals(false, imported.pauseInLandscape)
        assertTrue(imported.buttons.any { it.id == "custom_cam" && it.keyCode == KeyEvent.KEYCODE_CAMERA })
    }

    @Test
    fun geometryBuildsNonEmptyPathsForAllStylesAndEdges() {
        val fillPath = Path()
        val rimPath = Path()

        for (style in BezelStyle.entries) {
            for (side in BezelSide.entries) {
                val edgeX = if (side == BezelSide.RIGHT) 1080f else 0f
                BezelGeometry.buildAndroidPath(
                    outPath = fillPath,
                    rimPath = rimPath,
                    style = style,
                    side = side,
                    edgeX = edgeX,
                    centerY = 800f,
                    lengthPx = 180f,
                    depthPx = 42f,
                    shoulderSpread = 0.58f,
                    peakFlatness = 0.35f
                )
                assertFalse("Fill path must not be empty for $style / $side", fillPath.isEmpty)
                assertFalse("Rim path must not be empty for $style / $side", rimPath.isEmpty)
            }
        }
    }
}

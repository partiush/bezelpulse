package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.data.BezelConfigCodec
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.BezelStyle
import com.example.model.ButtonConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}

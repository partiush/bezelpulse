package com.example

import com.example.engine.ButtonSpringState
import com.example.engine.SpringAnalyzer
import com.example.model.BezelConfig
import com.example.model.BezelSide
import com.example.model.ButtonConfig
import com.example.model.ButtonIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BezelPhysicsTest {

    @Test
    fun springSettlesBackToZeroAfterRelease() {
        val config = BezelConfig(stiffness = 800f, dampingRatio = 0.95f, minPressHoldMs = 100L)
        val spring = ButtonSpringState(ButtonIds.VOLUME_UP)

        var nowMs = 1000L
        spring.onPress(nowMs)
        repeat(15) {
            nowMs += 16L
            spring.step(0.016f, config, nowMs)
        }
        assertTrue("Spring should protrude while pressed", spring.displacement > 0.75f)

        spring.onRelease(nowMs, config.minPressHoldMs)
        var stillMoving = true
        repeat(90) {
            nowMs += 16L
            stillMoving = spring.step(0.016f, config, nowMs)
        }

        assertFalse("Spring should come to rest after release", stillMoving)
        assertEquals(0f, spring.displacement, 0.001f)
    }

    @Test
    fun underdampedSpringProducesGreaterOvershootThanCriticallyDamped() {
        val bouncy = SpringAnalyzer.analyze(stiffness = 500f, dampingRatio = 0.45f)
        val critical = SpringAnalyzer.analyze(stiffness = 860f, dampingRatio = 0.98f)

        assertTrue(
            "Expected bouncy overshoot (${bouncy.peakOvershootPercent}%) > critical (${critical.peakOvershootPercent}%)",
            bouncy.peakOvershootPercent > critical.peakOvershootPercent
        )
    }

    @Test
    fun sustainedHoldAppliesHoldBoostMultiplier() {
        val config = BezelConfig(
            stiffness = 900f,
            dampingRatio = 0.95f,
            holdBoostMultiplier = 1.35f
        )
        val spring = ButtonSpringState(ButtonIds.VOLUME_DOWN)

        var nowMs = 5000L
        spring.onPress(nowMs)
        repeat(40) {
            nowMs += 16L
            spring.step(0.016f, config, nowMs)
        }

        assertTrue("Sustained hold should exceed 1.0 displacement", spring.displacement > 1.15f)
    }

    @Test
    fun customButtonsCanBeAddedAndRemovedWhileBuiltInsStayProtected() {
        val initial = BezelConfig()
        val initialCount = initial.buttons.size

        val added = initial.addButton(
            ButtonConfig(
                id = "custom_action",
                name = "Action",
                shortLabel = "Action",
                glyph = "⚡",
                keyCode = 27,
                isBuiltIn = false,
                side = BezelSide.LEFT
            )
        )
        assertEquals(initialCount + 1, added.buttons.size)

        val afterRemovingBuiltIn = added.removeButton(ButtonIds.VOLUME_UP)
        assertEquals(initialCount + 1, afterRemovingBuiltIn.buttons.size)

        val afterRemovingCustom = afterRemovingBuiltIn.removeButton("custom_action")
        assertEquals(initialCount, afterRemovingCustom.buttons.size)
    }
}

package dev.douglasneto.beatside.wheel

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import dev.douglasneto.beatside.menu.SettingKey
import dev.douglasneto.beatside.settings.WheelSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.cos
import kotlin.math.sin

/**
 * Glue test for the ClickWheel composable: real pointer input goes through pointerInput,
 * WheelTracker and the long-press timeout, and comes out as WheelEvents.
 * Robolectric 4.14 supports up to API 35 and cannot instrument on JDK 25+ ("Unsupported class
 * file major version 69"). Run with JDK 17–24; CI uses JDK 17.
 * See specs/001-click-wheel-nav/NOTES.md (open item: project JDK for Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClickWheelTest {

    @get:Rule val compose = createComposeRule()

    private val events = mutableListOf<WheelEvent>()

    /** Feedback outputs and dispatched events, in the order they happened. */
    private val log = mutableListOf<String>()
    private val fakeFeedback = FeedbackPlayer(
        sound = object : ClickSound { override fun play() { log += "click" } },
        haptics = object : HapticOutput {
            override fun tick() { log += "tick-haptic" }
            override fun press() { log += "press-haptic" }
        },
    )

    private var settings by mutableStateOf(WheelSettings())

    private fun setWheel() {
        compose.setContent {
            ClickWheel(
                settings = settings,
                onEvent = { events += it; log += "event:$it" },
                modifier = Modifier.size(200.dp).testTag("wheel"),
                feedback = fakeFeedback,
            )
        }
    }

    private fun dragDetents(detents: Int) {
        compose.onNodeWithTag("wheel").performTouchInput {
            val r = width / 2f
            down(Offset.onRing(center, r, -90.0))
            for (deg in 1..(detents * 15 + 5)) moveTo(Offset.onRing(center, r, -90.0 + deg))
            up()
        }
        compose.waitForIdle()
    }

    private fun Offset.Companion.onRing(center: Offset, radius: Float, degrees: Double) = Offset(
        center.x + radius * 0.8f * cos(Math.toRadians(degrees)).toFloat(),
        center.y + radius * 0.8f * sin(Math.toRadians(degrees)).toFloat(),
    )

    @Test fun `dragging 50 degrees clockwise on the ring emits three ticks`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput {
            val r = width / 2f
            down(Offset.onRing(center, r, -90.0))
            for (deg in 1..50) moveTo(Offset.onRing(center, r, -90.0 + deg))
            up()
        }
        compose.waitForIdle()
        assertEquals(List(3) { WheelEvent.Tick(+1) }, events)
    }

    @Test fun `dragging 20 degrees counter-clockwise emits one tick up`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput {
            val r = width / 2f
            down(Offset.onRing(center, r, 0.0))
            for (deg in 1..20) moveTo(Offset.onRing(center, r, -deg.toDouble()))
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf(WheelEvent.Tick(-1)), events)
    }

    @Test fun `tapping the center emits Press(Center)`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(listOf(WheelEvent.Press(WheelButton.Center)), events)
    }

    @Test fun `tapping the top of the ring emits Press(Menu)`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput {
            down(Offset.onRing(center, width / 2f, -90.0)); up()
        }
        compose.waitForIdle()
        assertEquals(listOf(WheelEvent.Press(WheelButton.Menu)), events)
    }

    @Test fun `holding the center for 500 ms emits LongPress and no Press`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput {
            down(center)
            advanceEventTime(600)
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf(WheelEvent.LongPress(WheelButton.Center)), events)
    }

    @Test fun `holding a ring button emits only Press on release`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput {
            down(Offset.onRing(center, width / 2f, 0.0))
            advanceEventTime(600)
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf(WheelEvent.Press(WheelButton.Next)), events)
    }

    // Feedback wiring (T031): FeedbackPlayer runs for every event, before the event is dispatched,
    // with the settings current at that moment.

    @Test fun `each tick clicks and buzzes before it is dispatched`() {
        setWheel()
        dragDetents(2)
        val tick = "event:${WheelEvent.Tick(+1)}"
        assertEquals(listOf("click", "tick-haptic", tick, "click", "tick-haptic", tick), log)
    }

    @Test fun `a press buzzes and never clicks`() {
        setWheel()
        compose.onNodeWithTag("wheel").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(listOf("press-haptic", "event:${WheelEvent.Press(WheelButton.Center)}"), log)
    }

    @Test fun `turning Clicker off applies to the very next tick`() {
        setWheel()
        dragDetents(1)
        assertEquals(1, log.count { it == "click" })
        settings = settings.toggled(SettingKey.Clicker)
        compose.waitForIdle()
        log.clear()
        dragDetents(1)
        assertEquals(listOf("tick-haptic", "event:${WheelEvent.Tick(+1)}"), log)
    }

    @Test fun `turning Haptics off applies to the very next press`() {
        setWheel()
        settings = settings.toggled(SettingKey.Haptics)
        compose.waitForIdle()
        compose.onNodeWithTag("wheel").performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(listOf("event:${WheelEvent.Press(WheelButton.Center)}"), log)
    }
}

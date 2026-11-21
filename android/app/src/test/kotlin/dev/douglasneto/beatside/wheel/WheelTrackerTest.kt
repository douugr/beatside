package dev.douglasneto.beatside.wheel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** Mirrors the test cases in docs/WHEEL.md. */
class WheelTrackerTest {

    private val diameter = 200f
    private val radius = diameter / 2f
    private val ringRadius = 80f // between the center button (36) and the edge (100)

    private fun pointAt(degrees: Float, distance: Float = ringRadius): Pair<Float, Float> {
        val rad = Math.toRadians(degrees.toDouble())
        return Pair(radius + distance * cos(rad).toFloat(), radius + distance * sin(rad).toFloat())
    }

    private fun tracker() = WheelTracker(WheelSensitivity.Normal, tapSlopPx = 8f)

    /** Drags along the ring from [from] by [by] degrees in 1° samples. Returns the sum of ticks. */
    private fun WheelTracker.drag(from: Float, by: Float): Int {
        val (x0, y0) = pointAt(from)
        assertTrue(down(x0, y0, diameter))
        var total = 0
        val steps = kotlin.math.abs(by).toInt()
        val sign = if (by >= 0) 1f else -1f
        for (i in 1..steps) {
            val (x, y) = pointAt(from + sign * i)
            total += move(x, y).sumOf { (it as WheelEvent.Tick).direction }
        }
        up()
        return total
    }

    @Test
    fun `normalize maps angles to the half-open interval`() {
        assertEquals(-170f, WheelTracker.normalize(190f), 0.001f)
        assertEquals(170f, WheelTracker.normalize(-190f), 0.001f)
        assertEquals(180f, WheelTracker.normalize(180f), 0.001f)
        assertEquals(180f, WheelTracker.normalize(-180f), 0.001f)
        assertEquals(180f, WheelTracker.normalize(540f), 0.001f)
    }

    @Test
    fun `slow clockwise drag of 50 degrees emits three ticks`() {
        assertEquals(3, tracker().drag(from = -90f, by = 50f))
    }

    @Test
    fun `counter-clockwise drag emits negative ticks`() {
        assertEquals(-3, tracker().drag(from = -90f, by = -50f))
    }

    @Test
    fun `crossing the 0 and 360 boundary does not burst`() {
        // From 170° to -170° (190°) is +20° clockwise: exactly one tick.
        assertEquals(1, tracker().drag(from = 170f, by = 20f))
    }

    @Test
    fun `small back and forth never ticks`() {
        val t = tracker()
        val (x0, y0) = pointAt(0f)
        t.down(x0, y0, diameter)
        repeat(5) {
            val (x1, y1) = pointAt(10f)
            assertTrue(t.move(x1, y1).isEmpty())
            val (x2, y2) = pointAt(0f)
            assertTrue(t.move(x2, y2).isEmpty())
        }
    }

    @Test
    fun `dead zone samples are ignored and re-anchor the angle`() {
        val t = tracker()
        val (x0, y0) = pointAt(0f)
        t.down(x0, y0, diameter)
        // Jump through the center to the opposite side.
        assertTrue(t.move(radius + 2f, radius + 2f).isEmpty())
        val (x1, y1) = pointAt(180f)
        assertTrue("re-anchoring must not emit ticks", t.move(x1, y1).isEmpty())
        val (x2, y2) = pointAt(200f)
        assertEquals(listOf<WheelEvent>(WheelEvent.Tick(+1)), t.move(x2, y2))
    }

    @Test
    fun `tap zones follow the button table`() {
        assertEquals(WheelButton.Center, WheelTracker.buttonAt(0f, 0f, radius))
        assertEquals(WheelButton.Menu, WheelTracker.buttonAt(0f, -ringRadius, radius))
        assertEquals(WheelButton.Next, WheelTracker.buttonAt(ringRadius, 0f, radius))
        assertEquals(WheelButton.PlayPause, WheelTracker.buttonAt(0f, ringRadius, radius))
        assertEquals(WheelButton.Previous, WheelTracker.buttonAt(-ringRadius, 0f, radius))
        assertEquals(null, WheelTracker.buttonAt(radius + 1f, 0f, radius))
        // Half a degree on each side of every zone edge.
        assertEquals(WheelButton.Menu, zoneAtAngle(-45.5f))
        assertEquals(WheelButton.Next, zoneAtAngle(-44.5f))
        assertEquals(WheelButton.Next, zoneAtAngle(44.5f))
        assertEquals(WheelButton.PlayPause, zoneAtAngle(45.5f))
        assertEquals(WheelButton.PlayPause, zoneAtAngle(134.5f))
        assertEquals(WheelButton.Previous, zoneAtAngle(135.5f))
        assertEquals(WheelButton.Previous, zoneAtAngle(-135.5f))
        assertEquals(WheelButton.Menu, zoneAtAngle(-134.5f))
    }

    private fun zoneAtAngle(degrees: Float): WheelButton? {
        val (x, y) = pointAt(degrees)
        return WheelTracker.buttonAt(x - radius, y - radius, radius)
    }

    @Test
    fun `tap emits press and long press suppresses it`() {
        val t = tracker()
        val (x, y) = pointAt(0f)
        t.down(x, y, diameter)
        assertEquals(listOf<WheelEvent>(WheelEvent.Press(WheelButton.Next)), t.up())

        t.down(radius, radius, diameter)
        assertEquals(listOf<WheelEvent>(WheelEvent.LongPress(WheelButton.Center)), t.longPressTimeout())
        assertTrue(t.up().isEmpty())
    }

    @Test
    fun `touch starting in the center never rotates`() {
        val t = tracker()
        assertTrue(t.down(radius, radius, diameter))
        val (x, y) = pointAt(0f)
        assertTrue(t.move(x, y).isEmpty())
        assertTrue("moved beyond slop: no press", t.up().isEmpty())
    }

    @Test
    fun `touch outside the wheel is rejected`() {
        assertFalse(tracker().down(0f, 0f, diameter))
    }
}

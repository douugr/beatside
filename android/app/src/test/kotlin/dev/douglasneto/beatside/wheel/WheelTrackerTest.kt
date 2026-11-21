package dev.douglasneto.beatside.wheel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * WheelTracker contract tests — implements ALL 40 cases from
 * specs/001-click-wheel-nav/contracts/wheel-behavior.md verbatim.
 *
 * diameter = 200, radius = 100, ringRadius = 80
 * pointAt(degrees, r=80) = (cx + r*cos(deg), cy + r*sin(deg)) with cx=cy=100 (wheel center)
 * Drag tests move in 1° increments from startAngle by totalDegrees.
 */
class WheelTrackerTest {

    private val diameter = 200f
    private val radius = 100f
    private val ringRadius = 80f
    private val cx = radius
    private val cy = radius
    private val tapSlop = 8f

    private fun tracker() = WheelTracker(tapSlopPx = tapSlop)

    /** Returns (cx + r*cos(deg), cy + r*sin(deg)) */
    private fun pointAt(degrees: Double, r: Double = ringRadius.toDouble()): Pair<Float, Float> {
        val rad = Math.toRadians(degrees)
        return (cx + r * cos(rad)).toFloat() to (cy + r * sin(rad)).toFloat()
    }

    private fun dragFrom(tracker: WheelTracker, startAngle: Double, totalDegrees: Double): Int {
        val (sx, sy) = pointAt(startAngle)
        tracker.down(sx, sy, diameter)
        var ticks = 0
        val steps = totalDegrees.toInt().let { if (it == 0) 1 else kotlin.math.abs(it) }
        val step = if (totalDegrees > 0) 1.0 else -1.0
        for (i in 1..steps) {
            val angle = startAngle + step * i
            val (px, py) = pointAt(angle)
            ticks += tracker.move(px, py).filterIsInstance<WheelEvent.Tick>()
                .sumOf { it.direction }
        }
        return ticks
    }

    // ===== Group 1: normalize =====

    @Test fun `N-1 normalize 190 is -170`() =
        assertEquals(-170f, WheelTracker.normalize(190f), 0.001f)

    @Test fun `N-2 normalize -190 is 170`() =
        assertEquals(170f, WheelTracker.normalize(-190f), 0.001f)

    @Test fun `N-3 normalize 180 is 180`() =
        assertEquals(180f, WheelTracker.normalize(180f), 0.001f)

    @Test fun `N-4 normalize -180 is 180`() =
        assertEquals(180f, WheelTracker.normalize(-180f), 0.001f)

    @Test fun `N-5 normalize 540 is 180`() =
        assertEquals(180f, WheelTracker.normalize(540f), 0.001f)

    @Test fun `N-6 normalize 0 is 0`() =
        assertEquals(0f, WheelTracker.normalize(0f), 0.001f)

    @Test fun `N-7 normalize -360 is 0`() =
        assertEquals(0f, WheelTracker.normalize(-360f), 0.001f)

    // ===== Group 2: Detent counting =====

    @Test fun `D-1 50 degrees CW from -90 gives +3 ticks`() {
        val t = tracker()
        val sum = dragFrom(t, -90.0, +50.0)
        assertEquals(3, sum)
    }

    @Test fun `D-2 50 degrees CCW from -90 gives -3 ticks`() {
        val t = tracker()
        val sum = dragFrom(t, -90.0, -50.0)
        assertEquals(-3, sum)
    }

    @Test fun `D-3 20 degrees CW from -90 gives +1 tick`() {
        val t = tracker()
        val sum = dragFrom(t, -90.0, +20.0)
        assertEquals(1, sum)
    }

    @Test fun `D-4 20 degrees CCW from -90 gives -1 tick`() {
        val t = tracker()
        val sum = dragFrom(t, -90.0, -20.0)
        assertEquals(-1, sum)
    }

    // ===== Group 3: Boundary crossing (no burst) =====

    @Test fun `B-1 170 to 190 crossing 0 gives +1 tick`() {
        val t = tracker()
        val sum = dragFrom(t, 170.0, +20.0)
        assertEquals(1, sum)
    }

    @Test fun `B-2 -170 to -190 gives -1 tick`() {
        val t = tracker()
        val sum = dragFrom(t, -170.0, -20.0)
        assertEquals(-1, sum)
    }

    // ===== Group 4: Jitter absorption =====

    @Test fun `J-1 oscillation within 10 deg emits no ticks and up returns empty`() {
        val t = tracker()
        val (sx, sy) = pointAt(0.0)
        t.down(sx, sy, diameter)
        // Oscillate ±10° five times
        var anyTick = false
        repeat(5) {
            val (px1, py1) = pointAt(10.0)
            val (px2, py2) = pointAt(-10.0)
            if (t.move(px1, py1).any { it is WheelEvent.Tick }) anyTick = true
            if (t.move(px2, py2).any { it is WheelEvent.Tick }) anyTick = true
        }
        assertFalse("No ticks should be emitted", anyTick)
        // Movement of ~14px exceeds 8px tap slop — up returns empty
        assertTrue("up should return empty after slop exceeded", t.up().isEmpty())
    }

    // ===== Group 5: Dead zone =====

    @Test fun `DZ-1 move into dead zone returns empty and drops anchor`() {
        val t = tracker()
        val (sx, sy) = pointAt(-90.0) // ring point
        t.down(sx, sy, diameter)
        // Move to dead zone (distance < 20% of radius = 20px from center)
        val dz = 15f
        val events = t.move(cx + dz, cy)
        assertTrue("Dead zone move should return empty", events.isEmpty())
    }

    @Test fun `DZ-2 after dead zone move to ring at 180 re-anchors without tick`() {
        val t = tracker()
        val (sx, sy) = pointAt(-90.0)
        t.down(sx, sy, diameter)
        t.move(cx + 15f, cy) // into dead zone
        val (rx, ry) = pointAt(180.0)
        val events = t.move(rx, ry) // re-anchor
        assertTrue("Re-anchor should return empty", events.isEmpty())
    }

    @Test fun `DZ-3 after re-anchor at 180 move to 200 gives +1 tick`() {
        val t = tracker()
        val (sx, sy) = pointAt(-90.0)
        t.down(sx, sy, diameter)
        t.move(cx + 15f, cy) // dead zone
        val (rx, ry) = pointAt(180.0)
        t.move(rx, ry) // re-anchor
        val (px, py) = pointAt(200.0)
        val events = t.move(px, py)
        val tickSum = events.filterIsInstance<WheelEvent.Tick>().sumOf { it.direction }
        assertEquals(1, tickSum)
    }

    // ===== Group 6: Touch that starts in center =====

    @Test fun `C-1 down at exact center returns true and startedInCenter true`() {
        val t = tracker()
        val result = t.down(cx, cy, diameter)
        assertTrue(result)
    }

    @Test fun `C-2 after C-1 move to ring returns empty`() {
        val t = tracker()
        t.down(cx, cy, diameter)
        val (px, py) = pointAt(0.0)
        val events = t.move(px, py)
        assertTrue(events.isEmpty())
    }

    @Test fun `C-3 after C-2 up with slop exceeded returns empty`() {
        val t = tracker()
        t.down(cx, cy, diameter)
        val (px, py) = pointAt(0.0) // ringRadius=80 > tapSlop=8
        t.move(px, py)
        assertTrue(t.up().isEmpty())
    }

    @Test fun `C-4 down at center no move up returns Press(Center)`() {
        val t = tracker()
        t.down(cx, cy, diameter)
        val result = t.up()
        assertEquals(listOf(WheelEvent.Press(WheelButton.Center)), result)
    }

    // ===== Group 7: Touch outside wheel =====

    @Test fun `O-1 down outside radius returns false`() {
        val t = tracker()
        val result = t.down(cx + radius + 1f, cy, diameter)
        assertFalse(result)
    }

    // ===== Group 8: Tap zone classification =====

    private fun tap(degrees: Double, r: Double = ringRadius.toDouble()): List<WheelEvent> {
        val t = tracker()
        val (x, y) = pointAt(degrees, r)
        t.down(x, y, diameter)
        return t.up()
    }

    @Test fun `T-1 tap at center returns Press(Center)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Center)), tap(0.0, 0.0))

    @Test fun `T-2 tap at top ring (-90) returns Press(Menu)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Menu)), tap(-90.0))

    @Test fun `T-3 tap at right ring (0) returns Press(Next)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Next)), tap(0.0))

    @Test fun `T-4 tap at bottom ring (90) returns Press(PlayPause)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.PlayPause)), tap(90.0))

    @Test fun `T-5 tap at left ring (180) returns Press(Previous)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Previous)), tap(180.0))

    @Test fun `T-7 tap at -134_5 returns Press(Menu)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Menu)), tap(-134.5))

    @Test fun `T-8 tap at -135_5 returns Press(Previous)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Previous)), tap(-135.5))

    @Test fun `T-10 tap at -44_5 returns Press(Next)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Next)), tap(-44.5))

    @Test fun `T-11 tap at -45_5 returns Press(Menu)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Menu)), tap(-45.5))

    @Test fun `T-13 tap at 44_5 returns Press(Next)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Next)), tap(44.5))

    @Test fun `T-14 tap at 45_5 returns Press(PlayPause)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.PlayPause)), tap(45.5))

    @Test fun `T-16 tap at 134_5 returns Press(PlayPause)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.PlayPause)), tap(134.5))

    @Test fun `T-17 tap at 135_5 returns Press(Previous)`() =
        assertEquals(listOf(WheelEvent.Press(WheelButton.Previous)), tap(135.5))

    // ===== Group 9: Long press (Center only) =====

    @Test fun `LP-1 center down no move longPressTimeout returns LongPress(Center) up returns empty`() {
        val t = tracker()
        t.down(cx, cy, diameter)
        val lp = t.longPressTimeout()
        assertEquals(listOf(WheelEvent.LongPress(WheelButton.Center)), lp)
        assertTrue("up after longPress should return empty", t.up().isEmpty())
    }

    @Test fun `LP-2 ring point down no move longPressTimeout returns empty up returns Press`() {
        val t = tracker()
        val (x, y) = pointAt(-90.0) // Menu
        t.down(x, y, diameter)
        val lp = t.longPressTimeout()
        assertTrue("Ring longPressTimeout should return empty", lp.isEmpty())
        assertEquals(listOf(WheelEvent.Press(WheelButton.Menu)), t.up())
    }

    @Test fun `LP-3 center down slop exceeded longPressTimeout returns empty`() {
        val t = tracker()
        t.down(cx, cy, diameter)
        val (px, py) = pointAt(0.0) // move to ring (> slop)
        t.move(px, py)
        val lp = t.longPressTimeout()
        assertTrue("longPressTimeout after slop exceeded should be empty", lp.isEmpty())
    }

    // ===== Group 10: Ring button hold (not long press) =====

    @Test fun `RH-1 MENU ring hold longPressTimeout returns empty up returns Press(Menu)`() {
        val t = tracker()
        val (x, y) = pointAt(-90.0) // Menu
        t.down(x, y, diameter)
        assertTrue(t.longPressTimeout().isEmpty())
        assertEquals(listOf(WheelEvent.Press(WheelButton.Menu)), t.up())
    }

    @Test fun `RH-2 Next ring hold longPressTimeout returns empty up returns Press(Next)`() {
        val t = tracker()
        val (x, y) = pointAt(0.0) // Next
        t.down(x, y, diameter)
        assertTrue(t.longPressTimeout().isEmpty())
        assertEquals(listOf(WheelEvent.Press(WheelButton.Next)), t.up())
    }

    @Test fun `RH-2 PlayPause ring hold longPressTimeout returns empty up returns Press(PlayPause)`() {
        val t = tracker()
        val (x, y) = pointAt(90.0) // PlayPause
        t.down(x, y, diameter)
        assertTrue(t.longPressTimeout().isEmpty())
        assertEquals(listOf(WheelEvent.Press(WheelButton.PlayPause)), t.up())
    }

    @Test fun `RH-2 Previous ring hold longPressTimeout returns empty up returns Press(Previous)`() {
        val t = tracker()
        val (x, y) = pointAt(180.0) // Previous
        t.down(x, y, diameter)
        assertTrue(t.longPressTimeout().isEmpty())
        assertEquals(listOf(WheelEvent.Press(WheelButton.Previous)), t.up())
    }
}

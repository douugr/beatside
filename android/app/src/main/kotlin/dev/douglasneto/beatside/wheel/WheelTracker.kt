package dev.douglasneto.beatside.wheel

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Turns touch samples into [WheelEvent]s.
 *
 * Pure logic — no Android imports — so it runs in plain JVM unit tests.
 * Coordinates are relative to the wheel's top-left corner, in pixels, with y growing down.
 *
 * Call order for one gesture: [down], any number of [move], optionally
 * [longPressTimeout] (after [LONG_PRESS_MILLIS]), then [up] or [cancel].
 */
class WheelTracker(
    private val tapSlopPx: Float,
) {
    private var active = false
    private var radius = 0f
    private var startX = 0f
    private var startY = 0f
    private var startButton = WheelButton.Center
    private var startedInCenter = false
    private var lastAngle: Float? = null
    private var accumulator = 0f
    private var emittedTick = false
    private var movedBeyondSlop = false
    private var longPressFired = false

    /**
     * Starts a gesture. [diameter] is the wheel's size in pixels.
     * Returns false (and ignores the gesture) when the touch is outside the wheel.
     */
    fun down(x: Float, y: Float, diameter: Float): Boolean {
        radius = diameter / 2f
        val button = buttonAt(x - radius, y - radius, radius) ?: return false
        active = true
        startX = x
        startY = y
        startButton = button
        startedInCenter = button == WheelButton.Center
        lastAngle = if (startedInCenter) null else angleOf(x - radius, y - radius)
        accumulator = 0f
        emittedTick = false
        movedBeyondSlop = false
        longPressFired = false
        return true
    }

    fun move(x: Float, y: Float): List<WheelEvent> {
        if (!active) return emptyList()
        if (hypot(x - startX, y - startY) > tapSlopPx) movedBeyondSlop = true
        if (startedInCenter || longPressFired) return emptyList()

        val dx = x - radius
        val dy = y - radius
        if (hypot(dx, dy) < DEAD_ZONE_RATIO * radius) {
            lastAngle = null
            return emptyList()
        }

        val angle = angleOf(dx, dy)
        val previous = lastAngle
        lastAngle = angle
        if (previous == null) return emptyList()

        accumulator += normalize(angle - previous)
        val step = STEP_DEGREES
        val events = mutableListOf<WheelEvent>()
        while (accumulator >= step) {
            events += WheelEvent.Tick(+1)
            accumulator -= step
        }
        while (accumulator <= -step) {
            events += WheelEvent.Tick(-1)
            accumulator += step
        }
        if (events.isNotEmpty()) emittedTick = true
        return events
    }

    /** Call once, [LONG_PRESS_MILLIS] after [down], if the gesture is still active. */
    fun longPressTimeout(): List<WheelEvent> {
        if (!active || longPressFired || emittedTick || movedBeyondSlop) return emptyList()
        // FR-010: long press is only valid for the Center button.
        if (startButton != WheelButton.Center) return emptyList()
        longPressFired = true
        return listOf(WheelEvent.LongPress(startButton))
    }

    fun up(): List<WheelEvent> {
        if (!active) return emptyList()
        active = false
        if (longPressFired || emittedTick || movedBeyondSlop) return emptyList()
        return listOf(WheelEvent.Press(startButton))
    }

    fun cancel() {
        active = false
    }

    companion object {
        const val CENTER_RATIO = 0.36f
        const val DEAD_ZONE_RATIO = 0.20f
        const val STEP_DEGREES = 15f
        const val TAP_SLOP_DP = 8f
        const val LONG_PRESS_MILLIS = 500L

        /** Maps any angle to (-180°, 180°]. */
        fun normalize(degrees: Float): Float {
            var d = degrees % 360f
            if (d > 180f) d -= 360f
            if (d <= -180f) d += 360f
            return d
        }

        /** Angle in degrees, screen coordinates: -90° is up, larger is clockwise. */
        fun angleOf(dx: Float, dy: Float): Float =
            Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        /** Which button sits at an offset from the wheel's center, or null outside the wheel. */
        fun buttonAt(dx: Float, dy: Float, radius: Float): WheelButton? {
            val distance = hypot(dx, dy)
            if (distance > radius) return null
            if (distance < CENTER_RATIO * radius) return WheelButton.Center
            val angle = angleOf(dx, dy)
            return when {
                angle >= -135f && angle < -45f -> WheelButton.Menu
                angle >= -45f && angle < 45f -> WheelButton.Next
                angle >= 45f && angle < 135f -> WheelButton.PlayPause
                else -> WheelButton.Previous
            }
        }
    }
}

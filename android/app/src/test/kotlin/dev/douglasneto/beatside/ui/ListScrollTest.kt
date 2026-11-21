package dev.douglasneto.beatside.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FR-016: the list scrolls the minimum amount needed to keep the highlighted row fully visible.
 * Viewport is [0, 100); rows are 20 px tall. Positive delta scrolls toward later rows.
 */
class ListScrollTest {

    private fun delta(itemStart: Int, itemEnd: Int) =
        ListScroll.deltaToReveal(itemStart, itemEnd, viewportStart = 0, viewportEnd = 100)

    @Test fun `fully visible row needs no scroll`() = assertEquals(0, delta(40, 60))

    @Test fun `row flush with both edges needs no scroll`() {
        assertEquals(0, delta(0, 20))
        assertEquals(0, delta(80, 100))
    }

    @Test fun `row cut off at the bottom scrolls just enough to reveal it`() = assertEquals(10, delta(90, 110))

    @Test fun `row cut off at the top scrolls back just enough`() = assertEquals(-5, delta(-5, 15))

    @Test fun `row entirely below scrolls until its bottom edge is at the viewport bottom`() =
        assertEquals(60, delta(140, 160))

    @Test fun `row entirely above scrolls until its top edge is at the viewport top`() =
        assertEquals(-60, delta(-60, -40))

    @Test fun `row taller than the viewport aligns its top edge`() {
        assertEquals(10, delta(10, 130))
        assertEquals(-10, delta(-10, 130))
    }
}

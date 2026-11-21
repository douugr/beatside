package dev.douglasneto.beatside.ui

/** Pure scroll math for the menu list (FR-016). No Compose imports, so it is unit-tested. */
object ListScroll {
    /**
     * Pixels to scroll so the row [itemStart, itemEnd) is fully inside the viewport
     * [viewportStart, viewportEnd), moving as little as possible. Positive scrolls toward later
     * rows. A row taller than the viewport is aligned to the top edge.
     */
    fun deltaToReveal(itemStart: Int, itemEnd: Int, viewportStart: Int, viewportEnd: Int): Int = when {
        itemStart < viewportStart -> itemStart - viewportStart
        itemEnd > viewportEnd -> minOf(itemEnd - viewportEnd, itemStart - viewportStart)
        else -> 0
    }
}

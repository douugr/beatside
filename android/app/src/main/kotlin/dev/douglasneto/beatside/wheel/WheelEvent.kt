package dev.douglasneto.beatside.wheel

/** What the wheel reports to screens. Screens never see raw touches. */
sealed interface WheelEvent {
    /** One detent. +1 is clockwise (down a list), -1 counter-clockwise. */
    data class Tick(val direction: Int) : WheelEvent
    data class Press(val button: WheelButton) : WheelEvent
    /** Only ever emitted for Center (FR-010). */
    data class LongPress(val button: WheelButton) : WheelEvent
}

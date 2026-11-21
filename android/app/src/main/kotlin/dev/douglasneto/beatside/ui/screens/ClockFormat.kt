package dev.douglasneto.beatside.ui.screens

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The Clock's formatting decision (FR-024, SC-006). Pure: the screen supplies the device's
 * 12/24-hour setting and the locale; this decides the pattern. Twin of ClockFormat.swift.
 */
object ClockFormat {
    const val PATTERN_24_HOUR = "HH:mm:ss"
    const val PATTERN_12_HOUR = "h:mm:ss a"

    fun pattern(is24Hour: Boolean): String = if (is24Hour) PATTERN_24_HOUR else PATTERN_12_HOUR

    fun format(time: LocalTime, is24Hour: Boolean, locale: Locale): String =
        DateTimeFormatter.ofPattern(pattern(is24Hour), locale).format(time)
}

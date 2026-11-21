package dev.douglasneto.beatside.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

/**
 * FR-024 / SC-006: which pattern the Clock uses for the device's 12/24-hour setting.
 * Pure: the platform only supplies is24Hour. Twin of ClockFormatTests.swift.
 */
class ClockFormatTest {

    private fun fmt(h: Int, m: Int, s: Int, is24Hour: Boolean) =
        ClockFormat.format(LocalTime.of(h, m, s), is24Hour, Locale.US)

    @Test fun `24-hour setting uses HH mm ss`() = assertEquals("HH:mm:ss", ClockFormat.pattern(is24Hour = true))

    @Test fun `12-hour setting uses hh mm ss with am pm`() = assertEquals("hh:mm:ss a", ClockFormat.pattern(is24Hour = false))

    @Test fun `evening time in 24-hour format`() = assertEquals("19:05:09", fmt(19, 5, 9, is24Hour = true))

    @Test fun `evening time in 12-hour format`() = assertEquals("07:05:09 PM", fmt(19, 5, 9, is24Hour = false))

    @Test fun `morning time in 12-hour format`() = assertEquals("09:30:00 AM", fmt(9, 30, 0, is24Hour = false))

    @Test fun `midnight is 00 in 24-hour and 12 AM in 12-hour`() {
        assertEquals("00:00:00", fmt(0, 0, 0, is24Hour = true))
        assertEquals("12:00:00 AM", fmt(0, 0, 0, is24Hour = false))
    }

    @Test fun `noon is 12 in both formats`() {
        assertEquals("12:00:00", fmt(12, 0, 0, is24Hour = true))
        assertEquals("12:00:00 PM", fmt(12, 0, 0, is24Hour = false))
    }
}

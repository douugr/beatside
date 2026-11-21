package dev.douglasneto.beatside.wheel

import dev.douglasneto.beatside.menu.SettingKey
import dev.douglasneto.beatside.settings.WheelSettings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FeedbackPlayer decides, per event and with the settings at that moment, whether to click and
 * which haptic to fire (FR-018, FR-019, FR-021). Fakes record the outputs; no real audio.
 * The iOS twin is FeedbackPlayerTests.swift.
 */
class FeedbackPlayerTest {

    private val log = mutableListOf<String>()
    private val player = FeedbackPlayer(
        sound = object : ClickSound { override fun play() { log += "click" } },
        haptics = object : HapticOutput {
            override fun tick() { log += "tick-haptic" }
            override fun press() { log += "press-haptic" }
        },
    )

    private val allOn = WheelSettings()
    private val clickerOff = WheelSettings(clickerEnabled = false)
    private val hapticsOff = WheelSettings(hapticsEnabled = false)
    private val allOff = WheelSettings(clickerEnabled = false, hapticsEnabled = false)

    private fun outputs(event: WheelEvent, settings: WheelSettings): List<String> {
        log.clear()
        player.play(event, settings)
        return log.toList()
    }

    // FR-018: every tick clicks and fires a light haptic, each unless disabled

    @Test fun `tick with everything on clicks and fires the tick haptic`() {
        assertEquals(listOf("click", "tick-haptic"), outputs(WheelEvent.Tick(+1), allOn))
        assertEquals(listOf("click", "tick-haptic"), outputs(WheelEvent.Tick(-1), allOn))
    }

    @Test fun `tick with Clicker off fires only the haptic`() =
        assertEquals(listOf("tick-haptic"), outputs(WheelEvent.Tick(+1), clickerOff))

    @Test fun `tick with Haptics off only clicks`() =
        assertEquals(listOf("click"), outputs(WheelEvent.Tick(+1), hapticsOff))

    @Test fun `tick with both off does nothing`() =
        assertEquals(emptyList<String>(), outputs(WheelEvent.Tick(+1), allOff))

    // FR-019: every press fires a medium haptic and never clicks

    @Test fun `every button press fires the press haptic and never clicks`() {
        for (button in WheelButton.entries) {
            assertEquals("press $button", listOf("press-haptic"), outputs(WheelEvent.Press(button), allOn))
        }
    }

    @Test fun `press with Haptics off does nothing even with Clicker on`() =
        assertEquals(emptyList<String>(), outputs(WheelEvent.Press(WheelButton.Center), hapticsOff))

    @Test fun `long press fires the press haptic and never clicks`() {
        assertEquals(listOf("press-haptic"), outputs(WheelEvent.LongPress(WheelButton.Center), allOn))
        assertEquals(emptyList<String>(), outputs(WheelEvent.LongPress(WheelButton.Center), hapticsOff))
    }

    // FR-021: a toggle applies to the very next event

    @Test fun `toggling Clicker applies to the very next tick`() {
        var settings = allOn
        assertEquals(listOf("click", "tick-haptic"), outputs(WheelEvent.Tick(+1), settings))
        settings = settings.toggled(SettingKey.Clicker)
        assertEquals(listOf("tick-haptic"), outputs(WheelEvent.Tick(+1), settings))
        settings = settings.toggled(SettingKey.Clicker)
        assertEquals(listOf("click", "tick-haptic"), outputs(WheelEvent.Tick(+1), settings))
    }

    @Test fun `toggling Haptics applies to the very next press`() {
        var settings = allOn
        assertEquals(listOf("press-haptic"), outputs(WheelEvent.Press(WheelButton.Center), settings))
        settings = settings.toggled(SettingKey.Haptics)
        assertEquals(emptyList<String>(), outputs(WheelEvent.Press(WheelButton.Center), settings))
        assertEquals(listOf("click"), outputs(WheelEvent.Tick(+1), settings))
    }
}

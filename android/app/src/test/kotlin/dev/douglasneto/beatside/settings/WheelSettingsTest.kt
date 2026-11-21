package dev.douglasneto.beatside.settings

import dev.douglasneto.beatside.menu.SettingKey
import org.junit.Assert.assertEquals
import org.junit.Test

/** WheelSettings (data-model.md): both default to true; toggling flips only the named field. */
class WheelSettingsTest {

    @Test fun `defaults are both enabled`() {
        assertEquals(WheelSettings(clickerEnabled = true, hapticsEnabled = true), WheelSettings())
    }

    @Test fun `toggling Clicker flips only clickerEnabled`() {
        assertEquals(WheelSettings(clickerEnabled = false, hapticsEnabled = true), WheelSettings().toggled(SettingKey.Clicker))
    }

    @Test fun `toggling Haptics flips only hapticsEnabled`() {
        assertEquals(WheelSettings(clickerEnabled = true, hapticsEnabled = false), WheelSettings().toggled(SettingKey.Haptics))
    }

    @Test fun `toggling twice restores the original value`() {
        val s = WheelSettings()
        assertEquals(s, s.toggled(SettingKey.Clicker).toggled(SettingKey.Clicker))
        assertEquals(s, s.toggled(SettingKey.Haptics).toggled(SettingKey.Haptics))
    }

    @Test fun `isOn reports the field for each key`() {
        val s = WheelSettings(clickerEnabled = false, hapticsEnabled = true)
        assertEquals(false, s.isOn(SettingKey.Clicker))
        assertEquals(true, s.isOn(SettingKey.Haptics))
    }
}

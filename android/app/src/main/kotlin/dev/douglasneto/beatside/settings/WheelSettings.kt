package dev.douglasneto.beatside.settings

import dev.douglasneto.beatside.menu.SettingKey

/**
 * Wheel feedback settings. Session-only: not persisted across launches (FR-030).
 * Toggling is the only mutation; the change applies to the next wheel event (FR-021).
 */
data class WheelSettings(
    val clickerEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
) {
    fun isOn(key: SettingKey): Boolean = when (key) {
        SettingKey.Clicker -> clickerEnabled
        SettingKey.Haptics -> hapticsEnabled
    }

    fun toggled(key: SettingKey): WheelSettings = when (key) {
        SettingKey.Clicker -> copy(clickerEnabled = !clickerEnabled)
        SettingKey.Haptics -> copy(hapticsEnabled = !hapticsEnabled)
    }
}

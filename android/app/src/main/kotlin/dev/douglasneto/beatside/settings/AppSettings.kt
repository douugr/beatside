package dev.douglasneto.beatside.settings

import dev.douglasneto.beatside.menu.SettingKey
import dev.douglasneto.beatside.wheel.WheelSensitivity

/** In-memory for now. Phase 4 persists these with DataStore. */
data class AppSettings(
    val clicker: Boolean = true,
    val haptics: Boolean = true,
    val sensitivity: WheelSensitivity = WheelSensitivity.Normal,
) {
    fun isOn(key: SettingKey): Boolean = when (key) {
        SettingKey.Clicker -> clicker
        SettingKey.Haptics -> haptics
    }

    fun toggled(key: SettingKey): AppSettings = when (key) {
        SettingKey.Clicker -> copy(clicker = !clicker)
        SettingKey.Haptics -> copy(haptics = !haptics)
    }
}

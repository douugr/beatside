package dev.douglasneto.beatside.menu

import dev.douglasneto.beatside.wheel.WheelEvent

/** A side effect the navigator cannot handle on its own. */
sealed interface MenuEffect {
    /** Flip a setting; handled by the UI layer. */
    data class ToggleSetting(val key: SettingKey) : MenuEffect

    /** The event has no meaning to the menus here (e.g. Play/Pause); route it elsewhere. */
    data class Unhandled(val event: WheelEvent) : MenuEffect
}

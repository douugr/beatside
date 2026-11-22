package dev.douglasneto.beatside.wheel

import dev.douglasneto.beatside.settings.WheelSettings

/** Plays the click sound. */
interface ClickSound {
    fun play()
}

/** Fires haptics: a light one per tick, a medium one per press. */
interface HapticOutput {
    fun tick()
    fun press()
}

/**
 * Turns wheel events into feedback. Pure: no Android imports, so the rules are unit-tested with
 * fakes. [settings] is read on every call, so a toggle applies to the very next event (FR-021).
 *
 * - Tick: click if Clicker is on, light haptic if Haptics is on (FR-018).
 * - Press / LongPress: medium haptic if Haptics is on, never a click (FR-019).
 */
class FeedbackPlayer(
    private val sound: ClickSound,
    private val haptics: HapticOutput,
) {
    fun play(event: WheelEvent, settings: WheelSettings) {
        when (event) {
            is WheelEvent.Tick -> {
                if (settings.clickerEnabled) sound.play()
                if (settings.hapticsEnabled) haptics.tick()
            }
            is WheelEvent.Press, is WheelEvent.LongPress ->
                if (settings.hapticsEnabled) haptics.press()
        }
    }
}

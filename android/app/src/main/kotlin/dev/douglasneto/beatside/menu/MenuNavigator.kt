package dev.douglasneto.beatside.menu

import dev.douglasneto.beatside.wheel.WheelButton
import dev.douglasneto.beatside.wheel.WheelEvent

/** The result of one reduction: the new state and an optional effect for the UI layer. */
data class MenuResult(val state: MenuState, val effect: MenuEffect? = null)

/**
 * Pure reducer: (state, wheel event) → (new state, optional effect). No I/O.
 *
 * - Ticks move the selection in a submenu and clamp at both ends (FR-015).
 * - Center opens the selected submenu or screen, or emits [MenuEffect.ToggleSetting] (FR-012).
 * - Menu goes back one level; at the root it does nothing (FR-013).
 * - Next / Previous / Play/Pause and long press are [MenuEffect.Unhandled] (FR-011).
 * - On a [MenuNode.Screen], every event except Menu belongs to the screen: [MenuEffect.Unhandled].
 */
object MenuNavigator {

    fun reduce(state: MenuState, event: WheelEvent): MenuResult {
        if (event is WheelEvent.Press && event.button == WheelButton.Menu) {
            return MenuResult(back(state))
        }

        val frame = state.current
        val node = frame.node as? MenuNode.Submenu
            ?: return MenuResult(state, MenuEffect.Unhandled(event))

        return when (event) {
            is WheelEvent.Tick -> {
                val last = (node.children.size - 1).coerceAtLeast(0)
                val index = (frame.selectedIndex + event.direction).coerceIn(0, last)
                MenuResult(state.replaceCurrent(frame.copy(selectedIndex = index)))
            }

            is WheelEvent.Press ->
                if (event.button == WheelButton.Center) select(state, node, frame.selectedIndex)
                else MenuResult(state, MenuEffect.Unhandled(event))

            is WheelEvent.LongPress -> MenuResult(state, MenuEffect.Unhandled(event))
        }
    }

    /** Pops one level; at the root returns [state] unchanged. */
    fun back(state: MenuState): MenuState =
        if (state.canGoBack) MenuState(state.stack.dropLast(1)) else state

    private fun select(state: MenuState, node: MenuNode.Submenu, index: Int): MenuResult {
        val child = node.children.getOrNull(index) ?: return MenuResult(state)
        return when (child) {
            is MenuNode.Submenu, is MenuNode.Screen ->
                MenuResult(MenuState(state.stack + MenuFrame(child)))
            is MenuNode.Toggle ->
                MenuResult(state, MenuEffect.ToggleSetting(child.key))
        }
    }

    private fun MenuState.replaceCurrent(frame: MenuFrame) =
        MenuState(stack.dropLast(1) + frame)
}

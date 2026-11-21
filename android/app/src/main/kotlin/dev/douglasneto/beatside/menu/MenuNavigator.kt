package dev.douglasneto.beatside.menu

import dev.douglasneto.beatside.wheel.WheelButton
import dev.douglasneto.beatside.wheel.WheelEvent

data class MenuFrame(val node: MenuNode, val selectedIndex: Int = 0)

data class MenuState(val stack: List<MenuFrame>) {
    val current: MenuFrame get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    companion object {
        fun root(node: MenuNode): MenuState = MenuState(listOf(MenuFrame(node)))
    }
}

/** Side effects the navigator cannot handle itself. */
sealed interface MenuEffect {
    data class ToggleSetting(val key: SettingKey) : MenuEffect

    /** The event means nothing to the menus (for example Play/Pause): route it elsewhere. */
    data class Unhandled(val event: WheelEvent) : MenuEffect
}

data class MenuResult(val state: MenuState, val effect: MenuEffect? = null)

/**
 * Pure reducer: (state, wheel event) → (new state, optional effect).
 *
 * - Ticks move the selection in a submenu and clamp at both ends.
 * - Center enters the selected submenu or screen, or flips a toggle.
 * - Menu goes back one level; at the root it does nothing.
 * - On a [MenuNode.Screen], ticks and Center belong to the screen and come back as
 *   [MenuEffect.Unhandled].
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

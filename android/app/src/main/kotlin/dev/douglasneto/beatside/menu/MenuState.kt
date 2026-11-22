package dev.douglasneto.beatside.menu

/** The navigation stack. Never empty; the root is always at index 0. */
data class MenuState(val stack: List<MenuFrame>) {
    init {
        require(stack.isNotEmpty()) { "MenuState stack must not be empty" }
    }

    val current: MenuFrame get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    companion object {
        fun root(node: MenuNode): MenuState = MenuState(listOf(MenuFrame(node)))
    }
}

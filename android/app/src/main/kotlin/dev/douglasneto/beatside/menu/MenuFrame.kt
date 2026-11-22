package dev.douglasneto.beatside.menu

/** One entry on the navigation stack. [selectedIndex] is 0 for leaf screens. */
data class MenuFrame(val node: MenuNode, val selectedIndex: Int = 0)

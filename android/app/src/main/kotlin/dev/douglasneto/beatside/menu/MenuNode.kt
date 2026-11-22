package dev.douglasneto.beatside.menu

/**
 * One entry in the menu tree. The whole app is a tree of these.
 *
 * [titleKey] is a locale-neutral string resource key (e.g. "menu_music"), resolved by the UI
 * layer, so this core stays free of Android imports (FR-029).
 */
sealed interface MenuNode {
    val titleKey: String

    /** A list of more entries. */
    data class Submenu(override val titleKey: String, val children: List<MenuNode>) : MenuNode

    /** A leaf screen, drawn by the UI layer according to [route]. */
    data class Screen(override val titleKey: String, val route: Route) : MenuNode

    /** An on/off setting, shown as "On"/"Off" on its row. Never pushed onto the stack. */
    data class Toggle(override val titleKey: String, val key: SettingKey) : MenuNode
}

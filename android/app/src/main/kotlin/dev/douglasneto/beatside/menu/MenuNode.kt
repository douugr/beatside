package dev.douglasneto.beatside.menu

/** One entry in the menu tree. The whole app is a tree of these. */
sealed interface MenuNode {
    val title: String

    /** A list of more entries. */
    data class Submenu(override val title: String, val children: List<MenuNode>) : MenuNode

    /** A custom screen, drawn by the UI layer according to [route]. */
    data class Screen(override val title: String, val route: Route) : MenuNode

    /** An on/off setting, shown as "On"/"Off" on the right of its row. */
    data class Toggle(override val title: String, val key: SettingKey) : MenuNode
}

enum class Route { NowPlaying, Clock, About, ComingSoon }

enum class SettingKey { Clicker, Haptics }

/** The Phase 0 menu tree. Music entries become real in Phase 2. */
object BeatSideMenu {
    val root = MenuNode.Submenu(
        title = "BeatSide",
        children = listOf(
            MenuNode.Submenu(
                title = "Music",
                children = listOf(
                    MenuNode.Screen("Playlists", Route.ComingSoon),
                    MenuNode.Screen("Artists", Route.ComingSoon),
                    MenuNode.Screen("Albums", Route.ComingSoon),
                    MenuNode.Screen("Songs", Route.ComingSoon),
                    MenuNode.Screen("Genres", Route.ComingSoon),
                ),
            ),
            MenuNode.Submenu(
                title = "Extras",
                children = listOf(
                    MenuNode.Screen("Clock", Route.Clock),
                    MenuNode.Screen("Stopwatch", Route.ComingSoon),
                    MenuNode.Screen("Games", Route.ComingSoon),
                ),
            ),
            MenuNode.Submenu(
                title = "Settings",
                children = listOf(
                    MenuNode.Toggle("Clicker", SettingKey.Clicker),
                    MenuNode.Toggle("Haptics", SettingKey.Haptics),
                    MenuNode.Screen("About", Route.About),
                ),
            ),
            MenuNode.Screen("Shuffle Songs", Route.ComingSoon),
            MenuNode.Screen("Now Playing", Route.NowPlaying),
        ),
    )
}

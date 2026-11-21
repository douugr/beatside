package dev.douglasneto.beatside.menu

/** The static menu tree for this slice (data-model.md BeatSideMenu). Keys only, no display text. */
object BeatSideMenu {
    val root = MenuNode.Submenu(
        titleKey = "beatside",
        children = listOf(
            MenuNode.Submenu(
                titleKey = "menu_music",
                children = listOf(
                    MenuNode.Screen("menu_playlists", Route.ComingSoon),
                    MenuNode.Screen("menu_artists", Route.ComingSoon),
                    MenuNode.Screen("menu_albums", Route.ComingSoon),
                    MenuNode.Screen("menu_songs", Route.ComingSoon),
                    MenuNode.Screen("menu_genres", Route.ComingSoon),
                ),
            ),
            MenuNode.Submenu(
                titleKey = "menu_extras",
                children = listOf(
                    MenuNode.Screen("menu_clock", Route.Clock),
                    MenuNode.Screen("menu_stopwatch", Route.ComingSoon),
                    MenuNode.Screen("menu_games", Route.ComingSoon),
                ),
            ),
            MenuNode.Submenu(
                titleKey = "menu_settings",
                children = listOf(
                    MenuNode.Toggle("menu_clicker", SettingKey.Clicker),
                    MenuNode.Toggle("menu_haptics", SettingKey.Haptics),
                    MenuNode.Screen("menu_about", Route.About),
                ),
            ),
            MenuNode.Screen("menu_shuffle_songs", Route.ShuffleSongs),
            MenuNode.Screen("menu_now_playing", Route.NowPlaying),
        ),
    )
}

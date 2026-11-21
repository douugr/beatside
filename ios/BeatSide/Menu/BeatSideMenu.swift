/// The static menu tree for this slice (data-model.md BeatSideMenu). Keys only, no display text.
/// Keys match the Android `BeatSideMenu.kt`.
enum BeatSideMenu {
    static let root = MenuNode.submenu(
        titleKey: "app_name",
        children: [
            .submenu(
                titleKey: "menu_music",
                children: [
                    .screen(titleKey: "menu_playlists", route: .comingSoon),
                    .screen(titleKey: "menu_artists", route: .comingSoon),
                    .screen(titleKey: "menu_albums", route: .comingSoon),
                    .screen(titleKey: "menu_songs", route: .comingSoon),
                    .screen(titleKey: "menu_genres", route: .comingSoon),
                ]
            ),
            .submenu(
                titleKey: "menu_extras",
                children: [
                    .screen(titleKey: "menu_clock", route: .clock),
                    .screen(titleKey: "menu_stopwatch", route: .comingSoon),
                    .screen(titleKey: "menu_games", route: .comingSoon),
                ]
            ),
            .submenu(
                titleKey: "menu_settings",
                children: [
                    .toggle(titleKey: "menu_clicker", key: .clicker),
                    .toggle(titleKey: "menu_haptics", key: .haptics),
                    .screen(titleKey: "menu_about", route: .about),
                ]
            ),
            .screen(titleKey: "menu_shuffle_songs", route: .shuffleSongs),
            .screen(titleKey: "menu_now_playing", route: .nowPlaying),
        ]
    )
}

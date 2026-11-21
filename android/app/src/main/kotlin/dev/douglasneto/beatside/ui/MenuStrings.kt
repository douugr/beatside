package dev.douglasneto.beatside.ui

import androidx.annotation.StringRes
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.menu.MenuNode

/**
 * Explicit titleKey → string resource mapping for the menu tree.
 *
 * Deliberately not `Resources.getIdentifier`: release builds shrink resources, and a string
 * referenced only by name could be stripped. MenuStringsTest fails if a BeatSideMenu key is
 * missing here.
 */
object MenuStrings {
    private val byKey: Map<String, Int> = mapOf(
        "app_name" to R.string.app_name,
        "menu_music" to R.string.menu_music,
        "menu_playlists" to R.string.menu_playlists,
        "menu_artists" to R.string.menu_artists,
        "menu_albums" to R.string.menu_albums,
        "menu_songs" to R.string.menu_songs,
        "menu_genres" to R.string.menu_genres,
        "menu_extras" to R.string.menu_extras,
        "menu_clock" to R.string.menu_clock,
        "menu_stopwatch" to R.string.menu_stopwatch,
        "menu_games" to R.string.menu_games,
        "menu_settings" to R.string.menu_settings,
        "menu_clicker" to R.string.menu_clicker,
        "menu_haptics" to R.string.menu_haptics,
        "menu_about" to R.string.menu_about,
        "menu_shuffle_songs" to R.string.menu_shuffle_songs,
        "menu_now_playing" to R.string.menu_now_playing,
    )

    @StringRes
    fun resId(titleKey: String): Int? = byKey[titleKey]

    @StringRes
    fun titleRes(node: MenuNode): Int =
        resId(node.titleKey) ?: error("No string resource mapped for titleKey '${node.titleKey}'")
}

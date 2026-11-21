package dev.douglasneto.beatside.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.menu.MenuNode
import dev.douglasneto.beatside.menu.MenuState
import dev.douglasneto.beatside.settings.WheelSettings

/** Localized pieces for screen-reader labels. Resources in the app, a fake in tests. */
interface AccessibilityStrings {
    fun title(titleKey: String): String
    fun submenuRow(title: String): String
    fun toggleRow(title: String, isOn: Boolean): String
    fun screenAndRow(screen: String, row: String): String
}

/**
 * Screen-reader labels for menu rows (FR-028) and what to announce after an event (FR-031).
 * Pure apart from the [AccessibilityStrings] it is given. Twin of MenuAccessibility.swift.
 */
object MenuAccessibility {

    /** "Music, submenu", "Clicker, On", or just the name for a leaf screen. */
    fun rowLabel(node: MenuNode, settings: WheelSettings, strings: AccessibilityStrings): String {
        val title = strings.title(node.titleKey)
        return when (node) {
            is MenuNode.Submenu -> strings.submenuRow(title)
            is MenuNode.Screen -> title
            is MenuNode.Toggle -> strings.toggleRow(title, settings.isOn(node.key))
        }
    }

    /**
     * What to say after an event took the app from [before] to [after], or null if nothing a
     * listener needs changed: a new screen is announced by its title (plus its highlighted row
     * for a submenu), a new selection by its row label, and a toggled setting by its row label
     * with the new state.
     */
    fun announcement(
        before: MenuState,
        after: MenuState,
        settingsBefore: WheelSettings,
        settingsAfter: WheelSettings,
        strings: AccessibilityStrings,
    ): String? {
        val frame = after.current
        val screenChanged = after.stack.size != before.stack.size || frame.node != before.current.node
        val selectedRow = (frame.node as? MenuNode.Submenu)?.children?.getOrNull(frame.selectedIndex)
            ?.let { rowLabel(it, settingsAfter, strings) }
        return when {
            screenChanged -> {
                val title = strings.title(frame.node.titleKey)
                if (selectedRow != null) strings.screenAndRow(title, selectedRow) else title
            }
            frame.selectedIndex != before.current.selectedIndex -> selectedRow
            settingsAfter != settingsBefore -> selectedRow
            else -> null
        }
    }
}

/** [AccessibilityStrings] from string resources, so labels follow the device language. */
class ResourceAccessibilityStrings(private val resources: Resources) : AccessibilityStrings {
    override fun title(titleKey: String) =
        resources.getString(MenuStrings.resId(titleKey) ?: error("No string resource mapped for titleKey '$titleKey'"))
    override fun submenuRow(title: String) = resources.getString(R.string.a11y_row_submenu, title)
    override fun toggleRow(title: String, isOn: Boolean) = resources.getString(
        R.string.a11y_row_toggle,
        title,
        resources.getString(if (isOn) R.string.setting_on else R.string.setting_off),
    )
    override fun screenAndRow(screen: String, row: String) = resources.getString(R.string.a11y_screen_and_row, screen, row)
}

@Composable
fun rememberAccessibilityStrings(): AccessibilityStrings {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current
    return remember(resources, configuration) { ResourceAccessibilityStrings(resources) }
}

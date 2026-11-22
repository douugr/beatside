package dev.douglasneto.beatside

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import dev.douglasneto.beatside.menu.BeatSideMenu
import dev.douglasneto.beatside.menu.MenuNode
import dev.douglasneto.beatside.ui.Announcer
import dev.douglasneto.beatside.ui.LcdTitleTestTag
import dev.douglasneto.beatside.ui.MenuStrings
import dev.douglasneto.beatside.wheel.ClickSound
import dev.douglasneto.beatside.wheel.FeedbackPlayer
import dev.douglasneto.beatside.wheel.HapticOutput
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Accessibility (T050; FR-027, FR-028, FR-031, SC-004, SC-007) through the real app, English.
 * Robolectric 4.14 supports up to API 35, hence @Config(sdk = [35]).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccessibilityTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val announcements = mutableListOf<String>()
    private val feedbackLog = mutableListOf<String>()
    private val fakeFeedback = FeedbackPlayer(
        sound = object : ClickSound { override fun play() { feedbackLog += "click" } },
        haptics = object : HapticOutput {
            override fun tick() { feedbackLog += "tick-haptic" }
            override fun press() { feedbackLog += "press-haptic" }
        },
    )

    @Before fun setUp() {
        compose.setContent {
            BeatSideApp(feedback = fakeFeedback, announcer = Announcer { announcements += it })
        }
    }

    private val wheel get() = compose.onNodeWithTag(ClickWheelTestTag)

    private fun action(label: String) {
        wheel.performCustomAccessibilityActionWithLabel(label)
        compose.waitForIdle()
    }

    private fun next() = action("Next item")
    private fun previous() = action("Previous item")
    private fun select() = action("Select")
    private fun back() = action("Go back")

    private fun title(node: MenuNode) = compose.activity.getString(MenuStrings.titleRes(node))

    // FR-031: the four actions

    @Test fun `the wheel exposes exactly the four actions`() {
        val actions = wheel.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)
        assertEquals(listOf("Next item", "Previous item", "Select", "Go back"), actions?.map { it.label })
    }

    @Test fun `actions go through the navigator and the same feedback as the wheel`() {
        next()
        assertEquals(listOf("click", "tick-haptic"), feedbackLog)
        compose.onNodeWithText("Extras").assertExists()
        feedbackLog.clear()
        select()
        assertEquals(listOf("press-haptic"), feedbackLog)
        compose.onNodeWithText("Clock").assertExists()
        feedbackLog.clear()
        back()
        assertEquals(listOf("press-haptic"), feedbackLog)
        compose.onNodeWithText("Music").assertExists()
    }

    @Test fun `each action that changes the selection or the screen announces the new label`() {
        next() // Extras row
        select() // into Extras
        select() // Clock
        back() // Extras
        back() // root
        previous() // Music row
        previous() // clamped at the first row: nothing to announce
        assertEquals(
            listOf("Extras, submenu", "Extras, Clock", "Clock", "Extras, Clock", "BeatSide, Extras, submenu", "Music, submenu"),
            announcements,
        )
    }

    @Test fun `toggling a setting announces the row with its new state`() {
        next(); next(); select() // Settings
        select() // Clicker
        assertEquals("Clicker, Off", announcements.last())
    }

    // FR-027 / SC-004: wheel and button labels

    @Test fun `the wheel and its five buttons have labels`() {
        compose.onNodeWithContentDescription("Wheel").assertExists()
        for (label in listOf("Select button", "Menu button", "Next button", "Previous button", "Play/Pause button")) {
            compose.onNodeWithContentDescription(label).assertExists()
        }
    }

    // FR-028 / SC-004: row labels with role or state

    @Test fun `rows are labelled with their name and role or state`() {
        for (label in listOf("Music, submenu", "Extras, submenu", "Settings, submenu", "Shuffle Songs", "Now Playing")) {
            compose.onNodeWithContentDescription(label).assertExists()
        }
        next(); next(); select()
        for (label in listOf("Clicker, On", "Haptics, On", "About")) {
            compose.onNodeWithContentDescription(label).assertExists()
        }
    }

    // SC-007: every leaf is reachable, and the root reachable again, with the actions alone

    @Test fun `every leaf can be reached and left using only the accessibility actions`() {
        val visited = mutableListOf<String>()

        fun currentTitle() = compose.onNodeWithTag(LcdTitleTestTag)
            .fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }

        fun explore(node: MenuNode.Submenu) {
            node.children.forEachIndexed { index, child ->
                repeat(index) { next() }
                if (child is MenuNode.Toggle) {
                    repeat(index) { previous() }
                    return@forEachIndexed
                }
                select()
                assertEquals(title(child), currentTitle())
                if (child is MenuNode.Submenu) explore(child) else visited += title(child)
                back()
                assertEquals(title(node), currentTitle())
                repeat(index) { previous() }
            }
        }

        explore(BeatSideMenu.root)
        // 5 Music items + 3 Extras + About + Shuffle Songs + Now Playing (toggles are not screens).
        assertEquals(11, visited.size)
        assertEquals("BeatSide", currentTitle())
    }
}

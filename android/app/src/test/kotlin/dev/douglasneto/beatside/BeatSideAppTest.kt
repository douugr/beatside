package dev.douglasneto.beatside

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import dev.douglasneto.beatside.wheel.ClickSound
import dev.douglasneto.beatside.wheel.FeedbackPlayer
import dev.douglasneto.beatside.wheel.HapticOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.cos
import kotlin.math.sin

/**
 * US2 (open items and go back) driven through the real app: touches on the wheel and the
 * system back dispatcher. Robolectric runs in the default (English) locale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BeatSideAppTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    /** Feedback outputs, recorded by fakes so the Settings → feedback chain is observable. */
    private val feedbackLog = mutableListOf<String>()
    private val fakeFeedback = FeedbackPlayer(
        sound = object : ClickSound { override fun play() { feedbackLog += "click" } },
        haptics = object : HapticOutput {
            override fun tick() { feedbackLog += "tick-haptic" }
            override fun press() { feedbackLog += "press-haptic" }
        },
    )

    @Before fun setUp() {
        compose.setContent { BeatSideApp(feedback = fakeFeedback) }
    }

    private fun TouchInjectionScope.ring(degrees: Double) = Offset(
        center.x + width / 2f * 0.8f * cos(Math.toRadians(degrees)).toFloat(),
        center.y + width / 2f * 0.8f * sin(Math.toRadians(degrees)).toFloat(),
    )

    private fun wheel(block: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag(ClickWheelTestTag).performTouchInput(block)
        compose.waitForIdle()
    }

    private fun tapCenter() = wheel { down(center); up() }
    private fun tapRing(degrees: Double) = wheel { down(ring(degrees)); up() }
    private fun tapMenu() = tapRing(-90.0)

    /** Clockwise detents, 15° each plus margin, starting on the left of the ring. */
    private fun rotate(detents: Int) = wheel {
        down(ring(180.0))
        for (deg in 1..(detents * 15 + 5)) moveTo(ring(180.0 + deg))
        up()
    }

    private fun shows(text: String) = compose.onNodeWithText(text).assertExists()
    private fun hides(text: String) = compose.onNodeWithText(text).assertDoesNotExist()

    @Test fun `center opens the highlighted submenu`() {
        tapCenter()
        shows("Playlists")
        hides("Extras")
    }

    @Test fun `center on Settings opens it and MENU returns to the root`() {
        rotate(2)
        tapCenter()
        shows("Clicker")
        shows("Haptics")
        hides("Music")

        tapMenu()
        shows("Music")
        shows("Settings")
        hides("Clicker")
    }

    @Test fun `MENU at the root does nothing`() {
        tapMenu()
        shows("BeatSide")
        shows("Music")
        shows("Now Playing")
    }

    @Test fun `center on a leaf shows Coming soon and MENU goes back`() {
        tapCenter() // Music
        tapCenter() // Playlists
        shows("Coming soon")
        shows("Press MENU to go back.")
        hides("Artists")

        tapMenu()
        shows("Playlists")
        shows("Artists")
        hides("Coming soon")
    }

    @Test fun `system back in a submenu behaves like MENU`() {
        tapCenter()
        shows("Playlists")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        shows("Extras")
        hides("Playlists")
        assertFalse(compose.activity.isFinishing)
    }

    @Test fun `system back at the root is left to the system`() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertTrue(compose.activity.isFinishing)
    }

    @Test fun `long press on center does not open anything`() {
        wheel { down(center); advanceEventTime(600); up() }
        shows("Music")
        hides("Playlists")
    }

    @Test fun `Next, Previous and Play-Pause do not navigate`() {
        tapRing(0.0)
        tapRing(180.0)
        tapRing(90.0)
        shows("Music")
        hides("Playlists")
    }

    // US3 Settings (T034): toggle rows show On/Off, flip on Center, and the new value applies to
    // the very next tick or press (FR-020, FR-021).

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun openSettings() {
        rotate(2)
        tapCenter()
        shows("Clicker")
    }

    @Test fun `Settings shows Clicker and Haptics On by default`() {
        openSettings()
        assertEquals(2, count("On"))
        assertEquals(0, count("Off"))
    }

    @Test fun `Center on a toggle flips its row between On and Off`() {
        openSettings()
        tapCenter() // Clicker
        assertEquals(1, count("On"))
        assertEquals(1, count("Off"))
        tapCenter()
        assertEquals(2, count("On"))
        shows("Clicker") // still in Settings: toggles never navigate
    }

    @Test fun `turning Clicker off silences the very next tick`() {
        openSettings()
        tapCenter() // Clicker -> Off
        feedbackLog.clear()
        rotate(1)
        assertEquals(listOf("tick-haptic"), feedbackLog)
    }

    @Test fun `turning Haptics off stops the very next press haptic`() {
        openSettings()
        rotate(1) // Haptics
        feedbackLog.clear()
        tapCenter() // Haptics -> Off; this press still uses the old setting
        assertEquals(listOf("press-haptic"), feedbackLog)
        assertEquals(1, count("Off"))
        feedbackLog.clear()
        tapCenter() // Haptics -> On; Haptics was off for this press
        assertEquals(emptyList<String>(), feedbackLog)
        feedbackLog.clear()
        rotate(1) // About: click and haptic again
        assertEquals(listOf("click", "tick-haptic"), feedbackLog)
    }

    // US4 leaf screens (T036-T038): Clock and About are real; every other leaf is Coming soon.

    private val time24 = Regex("""\d{2}:\d{2}:\d{2}""")
    private val time12 = Regex("""\d{2}:\d{2}:\d{2} (AM|PM)""")

    private fun textMatching(regex: Regex) = SemanticsMatcher("text matches $regex") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.any { regex.matches(it.text) } == true
    }

    private fun showsTime(regex: Regex) = compose.onNode(textMatching(regex)).assertExists()

    private fun setHourFormat(value: String) {
        Settings.System.putString(compose.activity.contentResolver, Settings.System.TIME_12_24, value)
    }

    private fun openClock() {
        rotate(1) // Extras
        tapCenter()
        tapCenter() // Clock
        shows("Clock")
    }

    @Test fun `Clock shows the time in 24-hour format when the device uses 24-hour`() {
        setHourFormat("24")
        openClock()
        showsTime(time24)
        hides("Coming soon")
    }

    @Test fun `Clock shows the time in 12-hour format when the device uses 12-hour`() {
        setHourFormat("12")
        openClock()
        showsTime(time12)
    }

    @Test fun `Clock switches format while open when the 12-24 setting changes`() {
        setHourFormat("12")
        openClock()
        showsTime(time12)
        setHourFormat("24")
        compose.runOnUiThread { compose.activity.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED)) }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        compose.waitForIdle()
        showsTime(time24)
    }

    @Test fun `About shows the app name, the build version and the author`() {
        rotate(2) // Settings
        tapCenter()
        rotate(2) // About
        tapCenter()
        shows("BeatSide")
        shows("Version ${BuildConfig.VERSION_NAME}")
        shows("by Douglas Rodrigues Neto")
        hides("Coming soon")
    }

    @Test fun `Stopwatch and Games stay Coming soon`() {
        rotate(1) // Extras
        tapCenter()
        rotate(1) // Stopwatch
        tapCenter()
        shows("Coming soon")
        tapMenu()
        rotate(1) // Games
        tapCenter()
        shows("Coming soon")
    }

    @Test fun `Shuffle Songs and Now Playing stay Coming soon`() {
        rotate(3) // Shuffle Songs
        tapCenter()
        shows("Coming soon")
        tapMenu()
        rotate(1) // Now Playing
        tapCenter()
        shows("Coming soon")
    }
}

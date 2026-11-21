package dev.douglasneto.beatside

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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

    @Before fun setUp() {
        compose.setContent { BeatSideApp() }
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
}

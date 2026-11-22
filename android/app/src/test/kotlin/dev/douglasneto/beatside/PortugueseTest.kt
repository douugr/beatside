package dev.douglasneto.beatside

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SC-005 / FR-028 / FR-029: the running app in Brazilian Portuguese. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR")
class PortugueseTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() {
        compose.setContent { BeatSideApp() }
    }

    @Test fun `root menu is in Portuguese`() {
        for (text in listOf("Música", "Extras", "Configurações", "Músicas aleatórias", "Tocando agora")) {
            compose.onNodeWithText(text).assertExists()
        }
    }

    @Test fun `row labels say the role in Portuguese`() {
        compose.onNodeWithContentDescription("Música, submenu").assertExists()
        compose.onNodeWithContentDescription("Tocando agora").assertExists()
    }

    @Test fun `wheel actions and buttons are in Portuguese`() {
        val actions = compose.onNodeWithTag(ClickWheelTestTag).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions)?.map { it.label }
        assertEquals(listOf("Próximo item", "Item anterior", "Selecionar", "Voltar"), actions)
        compose.onNodeWithContentDescription("Botão Menu").assertExists()
    }
}

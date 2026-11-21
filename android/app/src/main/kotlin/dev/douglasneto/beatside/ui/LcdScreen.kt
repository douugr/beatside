package dev.douglasneto.beatside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.douglasneto.beatside.menu.MenuNode
import dev.douglasneto.beatside.menu.MenuState
import dev.douglasneto.beatside.settings.WheelSettings

/**
 * The display: a title bar with the current node's title, then its content. Submenus show
 * their rows; leaf screens show a placeholder until RouteScreen lands in T027.
 */
@Composable
fun LcdScreen(state: MenuState, settings: WheelSettings, modifier: Modifier = Modifier) {
    val frame = state.current
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .border(2.dp, LcdColors.Border, shape)
            .background(LcdColors.Background),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(LcdColors.TitleBar)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(MenuStrings.titleRes(frame.node)),
                style = TextStyle(color = LcdColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(LcdColors.Border))
        when (val node = frame.node) {
            // A new list (fresh scroll position) per stack level.
            is MenuNode.Submenu -> key(state.stack.size) {
                MenuListContent(node, frame.selectedIndex, settings)
            }
            is MenuNode.Screen, is MenuNode.Toggle -> Box(Modifier.fillMaxSize())
        }
    }
}

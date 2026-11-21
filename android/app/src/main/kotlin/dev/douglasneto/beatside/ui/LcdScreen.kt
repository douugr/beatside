package dev.douglasneto.beatside.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.douglasneto.beatside.menu.MenuNode
import dev.douglasneto.beatside.menu.MenuState
import dev.douglasneto.beatside.menu.Route
import dev.douglasneto.beatside.settings.AppSettings
import dev.douglasneto.beatside.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The "LCD": a title bar and whatever the current menu frame shows. */
@Composable
fun LcdScreen(state: MenuState, settings: AppSettings, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val frame = state.current
    Column(
        modifier = modifier
            .border(2.dp, palette.lcdBorder, RoundedCornerShape(6.dp))
            .padding(2.dp)
            .background(palette.lcdBackground, RoundedCornerShape(4.dp)),
    ) {
        TitleBar(frame.node.title)
        Box(Modifier.fillMaxSize()) {
            when (val node = frame.node) {
                is MenuNode.Submenu -> MenuList(node, frame.selectedIndex, settings)
                is MenuNode.Screen -> RouteScreen(node.route)
                is MenuNode.Toggle -> Unit // Toggles are never pushed.
            }
        }
    }
}

@Composable
private fun TitleBar(title: String) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Brush.verticalGradient(listOf(palette.titleBarTop, palette.titleBarBottom)))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp, 12.dp)) // Phase 1: play/pause indicator.
        BasicText(
            text = title,
            modifier = Modifier.weight(1f),
            style = TextStyle(color = palette.lcdText, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BatteryIcon()
    }
}

@Composable
private fun BatteryIcon() {
    val palette = LocalPalette.current
    Canvas(Modifier.size(22.dp, 12.dp)) {
        val body = Size(size.width - 3.dp.toPx(), size.height)
        drawRect(palette.lcdText, size = body, style = Stroke(1.dp.toPx()))
        drawRect(
            palette.lcdText,
            topLeft = Offset(body.width, size.height * 0.3f),
            size = Size(2.dp.toPx(), size.height * 0.4f),
        )
        val inset = 2.dp.toPx()
        drawRect(
            Brush.verticalGradient(listOf(palette.selectionTop, palette.selectionBottom)),
            topLeft = Offset(inset, inset),
            size = Size(body.width - inset * 2, body.height - inset * 2),
        )
    }
}

@Composable
private fun MenuList(node: MenuNode.Submenu, selectedIndex: Int, settings: AppSettings) {
    val palette = LocalPalette.current
    val listState = rememberLazyListState()

    // Scroll only as much as needed to keep the selection visible.
    LaunchedEffect(selectedIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val first = visible.first().index
        val viewportEnd = listState.layoutInfo.viewportEndOffset
        val lastFullyVisible = (visible.lastOrNull { it.offset + it.size <= viewportEnd } ?: visible.first()).index
        when {
            selectedIndex < first -> listState.scrollToItem(selectedIndex)
            selectedIndex > lastFullyVisible ->
                listState.scrollToItem((selectedIndex - (lastFullyVisible - first)).coerceAtLeast(0))
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(node.children) { index, child ->
            val selected = index == selectedIndex
            val textColor = if (selected) palette.selectionText else palette.lcdText
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .then(
                        if (selected) Modifier.background(
                            Brush.verticalGradient(listOf(palette.selectionTop, palette.selectionBottom)),
                        ) else Modifier,
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(
                    text = child.title,
                    style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val trailing = when (child) {
                    is MenuNode.Submenu, is MenuNode.Screen -> "›"
                    is MenuNode.Toggle -> if (settings.isOn(child.key)) "On" else "Off"
                }
                BasicText(
                    text = trailing,
                    style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun RouteScreen(route: Route) {
    when (route) {
        Route.Clock -> ClockScreen()
        Route.NowPlaying -> CenteredMessage("Nothing playing", "Universal Now Playing arrives in Phase 1.")
        Route.About -> CenteredMessage("BeatSide 0.1.0", "A retro wheel music player.\n© 2026 Douglas Rodrigues Neto")
        Route.ComingSoon -> CenteredMessage("Coming soon", "Press MENU to go back.")
    }
}

@Composable
private fun ClockScreen() {
    val now by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    CenteredMessage(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), "Local time", headlineSize = 34)
}

@Composable
private fun CenteredMessage(headline: String, body: String, headlineSize: Int = 18) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            headline,
            style = TextStyle(color = palette.lcdText, fontSize = headlineSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        BasicText(
            body,
            modifier = Modifier.padding(top = 6.dp),
            style = TextStyle(color = palette.lcdText.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center),
        )
    }
}

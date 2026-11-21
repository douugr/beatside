package dev.douglasneto.beatside.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.menu.MenuNode
import dev.douglasneto.beatside.settings.WheelSettings

private val RowHeight = 34.dp

/**
 * The rows of a [MenuNode.Submenu]. The highlighted row is always kept fully visible, scrolling
 * the minimum amount (FR-016). Rows have a fixed height so the scroll math is exact.
 */
@Composable
fun MenuListContent(
    node: MenuNode.Submenu,
    selectedIndex: Int,
    settings: WheelSettings,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val rowPx = with(LocalDensity.current) { RowHeight.roundToPx() }

    LaunchedEffect(selectedIndex, rowPx) {
        val info = listState.layoutInfo
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        if (viewport <= 0) return@LaunchedEffect
        val scrolled = listState.firstVisibleItemIndex * rowPx + listState.firstVisibleItemScrollOffset
        val itemStart = selectedIndex * rowPx - scrolled
        val delta = ListScroll.deltaToReveal(itemStart, itemStart + rowPx, 0, viewport)
        if (delta != 0) {
            val target = scrolled + delta
            listState.scrollToItem(target / rowPx, target % rowPx)
        }
    }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize(), userScrollEnabled = false) {
        itemsIndexed(node.children) { index, child ->
            MenuRow(child, selected = index == selectedIndex, settings = settings)
        }
    }
}

@Composable
private fun MenuRow(child: MenuNode, selected: Boolean, settings: WheelSettings) {
    // FR-028: one screen-reader label with the name plus the role or state.
    val rowLabel = MenuAccessibility.rowLabel(child, settings, rememberAccessibilityStrings())
    val color = if (selected) LcdColors.SelectedText else LcdColors.Text
    val style = TextStyle(color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val suffix = when (child) {
        is MenuNode.Submenu, is MenuNode.Screen -> "›"
        is MenuNode.Toggle ->
            stringResource(if (settings.isOn(child.key)) R.string.setting_on else R.string.setting_off)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .semantics(mergeDescendants = true) { contentDescription = rowLabel }
            .then(if (selected) Modifier.background(LcdColors.Selection) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            stringResource(MenuStrings.titleRes(child)),
            modifier = Modifier.weight(1f),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(suffix, style = style)
    }
}

package dev.douglasneto.beatside.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.ui.LcdColors

/** Placeholder for leaf screens that are not built yet (FR-026). */
@Composable
fun ComingSoonScreen(modifier: Modifier = Modifier) {
    CenteredMessage(
        title = stringResource(R.string.coming_soon),
        hint = stringResource(R.string.press_menu_to_go_back),
        modifier = modifier,
    )
}

/** A bold message with a smaller hint underneath, centered on the LCD. */
@Composable
fun CenteredMessage(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            title,
            style = TextStyle(color = LcdColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        )
        BasicText(
            hint,
            style = TextStyle(color = LcdColors.Text, fontSize = 14.sp, textAlign = TextAlign.Center),
        )
    }
}

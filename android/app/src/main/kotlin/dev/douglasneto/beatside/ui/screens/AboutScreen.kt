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
import dev.douglasneto.beatside.BuildConfig
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.ui.LcdColors

/** App name, version and author (FR-025). The version comes from BuildConfig, never a literal. */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val body = TextStyle(color = LcdColors.Text, fontSize = 15.sp, textAlign = TextAlign.Center)
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(stringResource(R.string.app_name), style = body.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        BasicText(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = body)
        BasicText(stringResource(R.string.about_author), style = body)
    }
}

package dev.douglasneto.beatside.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Retro LCD palette shared by the screen composables. */
internal object LcdColors {
    val Background = Color(0xFFF2F4F0)
    val Text = Color(0xFF1B1B1B)
    val Border = Color(0xFF55595C)
    val TitleBar = Brush.verticalGradient(listOf(Color(0xFFFDFDFD), Color(0xFFD9DCDF)))
    val Selection = Brush.verticalGradient(listOf(Color(0xFF6DB0F2), Color(0xFF2B73C9)))
    val SelectedText = Color.White
}

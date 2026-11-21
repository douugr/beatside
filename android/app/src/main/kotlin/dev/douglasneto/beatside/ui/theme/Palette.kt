package dev.douglasneto.beatside.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors for one device "finish". Phase 4 adds black and color variants. */
@Immutable
data class Palette(
    val body: Color,
    val lcdBackground: Color,
    val lcdBorder: Color,
    val lcdText: Color,
    val titleBarTop: Color,
    val titleBarBottom: Color,
    val selectionTop: Color,
    val selectionBottom: Color,
    val selectionText: Color,
    val wheelRing: Color,
    val wheelRingShadow: Color,
    val wheelCenter: Color,
    val wheelLabel: Color,
)

val ClassicWhite = Palette(
    body = Color(0xFFF2F2F2),
    lcdBackground = Color(0xFFFFFFFF),
    lcdBorder = Color(0xFF2B2B2B),
    lcdText = Color(0xFF111111),
    titleBarTop = Color(0xFFF7F7F7),
    titleBarBottom = Color(0xFFD5D8DC),
    selectionTop = Color(0xFF6FB3F0),
    selectionBottom = Color(0xFF1F6FC9),
    selectionText = Color(0xFFFFFFFF),
    wheelRing = Color(0xFFE4E4E4),
    wheelRingShadow = Color(0xFFCACACA),
    wheelCenter = Color(0xFFF8F8F8),
    wheelLabel = Color(0xFFA3A3A3),
)

val LocalPalette = staticCompositionLocalOf { ClassicWhite }

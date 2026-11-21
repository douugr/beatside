package dev.douglasneto.beatside.ui

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Speaks a short text through the active screen reader. A fake records it in tests. */
fun interface Announcer {
    fun announce(text: String)
}

/**
 * Announces through the hosting [View]. `announceForAccessibility` is deprecated from API 36 in
 * favour of live regions, but it is still delivered by TalkBack and is the direct "announcement"
 * this feature asks for; it does nothing when no screen reader is running.
 */
class ViewAnnouncer(private val view: View) : Announcer {
    @Suppress("DEPRECATION")
    override fun announce(text: String) = view.announceForAccessibility(text)
}

@Composable
fun rememberViewAnnouncer(): Announcer {
    val view = LocalView.current
    return remember(view) { ViewAnnouncer(view) }
}

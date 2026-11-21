package dev.douglasneto.beatside

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Entry point. Portrait-only is locked in AndroidManifest.xml. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BeatSideApp() }
    }
}

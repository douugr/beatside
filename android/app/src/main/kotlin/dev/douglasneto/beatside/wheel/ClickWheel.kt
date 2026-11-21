package dev.douglasneto.beatside.wheel

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.settings.WheelSettings
import kotlinx.coroutines.CancellationException

private val RingLight = Color(0xFFF4F4F2)
private val RingDark = Color(0xFFD6D6D2)
private val RingEdge = Color(0xFFBDBDB8)
private val CenterLight = Color(0xFFE9E9E6)
private val CenterDark = Color(0xFFCFCFCB)
private val Glyph = Color(0xFF8C8C88)

/**
 * The wheel. Draws the ring and buttons, turns touches into [WheelEvent]s through a
 * [WheelTracker], plays feedback for each one with the current [settings], then reports it
 * through [onEvent]. It knows nothing about menus or screens (Constitution II).
 *
 * [feedback] defaults to the real click sound and view haptics; tests pass fakes.
 */
@Composable
fun ClickWheel(
    settings: WheelSettings,
    onEvent: (WheelEvent) -> Unit,
    modifier: Modifier = Modifier,
    feedback: FeedbackPlayer = rememberFeedbackPlayer(),
) {
    val tapSlopPx = with(LocalDensity.current) { WheelTracker.TAP_SLOP_DP.dp.toPx() }
    val tracker = remember(tapSlopPx) { WheelTracker(tapSlopPx) }
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentSettings by rememberUpdatedState(settings)
    val currentFeedback by rememberUpdatedState(feedback)
    // Feedback first, in the same frame as the gesture sample (Constitution II), then the event.
    val dispatch: (WheelEvent) -> Unit = { event ->
        currentFeedback.play(event, currentSettings)
        currentOnEvent(event)
    }
    val menuLabel = stringResource(R.string.wheel_menu)
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier
            .aspectRatio(1f)
            .pointerInput(tracker) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!tracker.down(down.position.x, down.position.y, size.width.toFloat())) {
                        return@awaitEachGesture
                    }
                    down.consume()
                    try {
                        var lastUptime = down.uptimeMillis
                        var longPressDue = true
                        while (true) {
                            val event = if (longPressDue) {
                                val remaining = WheelTracker.LONG_PRESS_MILLIS - (lastUptime - down.uptimeMillis)
                                withTimeoutOrNull(remaining.coerceAtLeast(0)) { awaitPointerEvent() }
                            } else {
                                awaitPointerEvent()
                            }
                            if (event == null) {
                                // Held for LONG_PRESS_MILLIS: the tracker decides if it counts (Center only).
                                longPressDue = false
                                tracker.longPressTimeout().forEach(dispatch)
                                continue
                            }
                            val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                            lastUptime = change.uptimeMillis
                            if (!change.pressed) {
                                change.consume()
                                tracker.up().forEach(dispatch)
                                break
                            }
                            if (change.positionChanged()) {
                                change.consume()
                                tracker.move(change.position.x, change.position.y).forEach(dispatch)
                            }
                        }
                    } catch (e: CancellationException) {
                        tracker.cancel()
                        throw e
                    }
                }
            }
            .drawBehind {
                drawWheel()
                val label = textMeasurer.measure(
                    menuLabel,
                    TextStyle(color = Glyph, fontSize = (size.minDimension * 0.055f / density).sp, fontWeight = FontWeight.Bold),
                )
                val r = size.minDimension / 2f
                drawText(
                    label,
                    topLeft = Offset(center.x - label.size.width / 2f, center.y - r * 0.68f - label.size.height / 2f),
                )
            },
    )
}

/** The real feedback: SoundPool click and haptics on the hosting view. Released on dispose. */
@Composable
fun rememberFeedbackPlayer(): FeedbackPlayer {
    val context = LocalContext.current
    val view = LocalView.current
    val sound = remember(context) { SoundPoolClickSound(context.applicationContext) }
    DisposableEffect(sound) { onDispose { sound.release() } }
    return remember(sound, view) { FeedbackPlayer(sound, ViewHaptics(view)) }
}

private fun DrawScope.drawWheel() {
    val r = size.minDimension / 2f
    drawCircle(Brush.radialGradient(listOf(RingLight, RingDark), center, r), r)
    drawCircle(RingEdge, r - 1.dp.toPx(), style = Stroke(1.dp.toPx()))

    val centerRadius = r * WheelTracker.CENTER_RATIO
    drawCircle(Brush.verticalGradient(listOf(CenterLight, CenterDark), center.y - centerRadius, center.y + centerRadius), centerRadius)
    drawCircle(RingEdge, centerRadius, style = Stroke(1.dp.toPx()))

    val g = r * 0.07f // glyph size
    val ringMid = r * 0.68f
    drawSkip(Offset(center.x + ringMid, center.y), g, forward = true)
    drawSkip(Offset(center.x - ringMid, center.y), g, forward = false)
    drawPlayPause(Offset(center.x, center.y + ringMid), g)
}

private fun DrawScope.triangle(left: Float, top: Float, width: Float, height: Float, pointsRight: Boolean) {
    val path = Path().apply {
        if (pointsRight) {
            moveTo(left, top); lineTo(left + width, top + height / 2f); lineTo(left, top + height)
        } else {
            moveTo(left + width, top); lineTo(left, top + height / 2f); lineTo(left + width, top + height)
        }
        close()
    }
    drawPath(path, Glyph)
}

/** Two triangles and a bar: ▶▶| for Next, |◀◀ for Previous. */
private fun DrawScope.drawSkip(at: Offset, g: Float, forward: Boolean) {
    val w = g * 0.9f
    val bar = g * 0.22f
    val total = w * 2 + bar
    val left = at.x - total / 2f
    val top = at.y - g / 2f
    if (forward) {
        triangle(left, top, w, g, pointsRight = true)
        triangle(left + w, top, w, g, pointsRight = true)
        drawRect(Glyph, Offset(left + 2 * w, top), Size(bar, g))
    } else {
        drawRect(Glyph, Offset(left, top), Size(bar, g))
        triangle(left + bar, top, w, g, pointsRight = false)
        triangle(left + bar + w, top, w, g, pointsRight = false)
    }
}

/** ▶‖ for Play/Pause. */
private fun DrawScope.drawPlayPause(at: Offset, g: Float) {
    val w = g * 0.9f
    val bar = g * 0.22f
    val gap = g * 0.2f
    val total = w + gap + bar * 2 + gap * 0.6f
    val left = at.x - total / 2f
    val top = at.y - g / 2f
    triangle(left, top, w, g, pointsRight = true)
    drawRect(Glyph, Offset(left + w + gap, top), Size(bar, g))
    drawRect(Glyph, Offset(left + w + gap + bar + gap * 0.6f, top), Size(bar, g))
}

package dev.douglasneto.beatside.wheel

import android.media.AudioAttributes
import android.media.SoundPool
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.douglasneto.beatside.R
import dev.douglasneto.beatside.ui.theme.LocalPalette
import dev.douglasneto.beatside.ui.theme.Palette

/**
 * The click wheel. Draws itself, turns touches into [WheelEvent]s through
 * [WheelTracker], and plays the click sound and haptics.
 */
@Composable
fun ClickWheel(
    onEvent: (WheelEvent) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    sensitivity: WheelSensitivity = WheelSensitivity.Normal,
    clickerEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val tapSlopPx = with(LocalDensity.current) { 8.dp.toPx() }
    val tracker = remember(tapSlopPx) { WheelTracker(tapSlopPx = tapSlopPx) }
    tracker.sensitivity = sensitivity

    val clickSound = rememberClickSound()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentClicker by rememberUpdatedState(clickerEnabled)
    val currentHaptics by rememberUpdatedState(hapticsEnabled)

    fun dispatch(events: List<WheelEvent>) {
        for (event in events) {
            when (event) {
                is WheelEvent.Tick -> {
                    if (currentClicker) clickSound.play()
                    if (currentHaptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                is WheelEvent.Press, is WheelEvent.LongPress -> {
                    if (currentHaptics) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
            currentOnEvent(event)
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(tracker) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val accepted = tracker.down(
                        x = down.position.x,
                        y = down.position.y,
                        diameter = this.size.width.toFloat(),
                    )
                    if (!accepted) return@awaitEachGesture
                    down.consume()

                    val endedBeforeLongPress = withTimeoutOrNull(WheelTracker.LONG_PRESS_MILLIS) {
                        trackUntilUp(down.id, tracker, ::dispatch)
                    }
                    if (endedBeforeLongPress == null) {
                        dispatch(tracker.longPressTimeout())
                        trackUntilUp(down.id, tracker, ::dispatch)
                    }
                }
            },
    ) {
        Canvas(Modifier.size(size)) { drawWheel(palette, textMeasurer) }
    }
}

/** Feeds moves to the tracker until the finger lifts. Returns true when the gesture ends. */
private suspend fun AwaitPointerEventScope.trackUntilUp(
    pointerId: PointerId,
    tracker: WheelTracker,
    dispatch: (List<WheelEvent>) -> Unit,
): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId }
        if (change == null) {
            tracker.cancel()
            return true
        }
        if (!change.pressed) {
            dispatch(tracker.up())
            change.consume()
            return true
        }
        dispatch(tracker.move(change.position.x, change.position.y))
        change.consume()
    }
}

private fun DrawScope.drawWheel(palette: Palette, textMeasurer: TextMeasurer) {
    val radius = size.minDimension / 2f
    val center = Offset(radius, radius)

    // Ring with a soft top-to-bottom shading.
    drawCircle(
        brush = Brush.verticalGradient(listOf(palette.wheelRing, palette.wheelRingShadow)),
        radius = radius,
        center = center,
    )
    // Center button.
    val centerRadius = radius * WheelTracker.CENTER_RATIO
    drawCircle(palette.wheelRingShadow, radius = centerRadius + 1.5f, center = center)
    drawCircle(
        brush = Brush.verticalGradient(
            listOf(palette.wheelCenter, palette.wheelRing),
            startY = center.y - centerRadius,
            endY = center.y + centerRadius,
        ),
        radius = centerRadius,
        center = center,
    )

    // Labels and icons, halfway between the center button and the edge.
    val labelDistance = radius * (1f + WheelTracker.CENTER_RATIO) / 2f
    val icon = radius * 0.07f
    val menuLayout = textMeasurer.measure(
        "MENU",
        TextStyle(color = palette.wheelLabel, fontSize = (radius * 0.085f).toSp(), fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    )
    drawText(
        menuLayout,
        topLeft = Offset(
            center.x - menuLayout.size.width / 2f,
            center.y - labelDistance - menuLayout.size.height / 2f,
        ),
    )
    drawSkip(Offset(center.x + labelDistance, center.y), icon, forward = true, palette)
    drawSkip(Offset(center.x - labelDistance, center.y), icon, forward = false, palette)
    drawPlayPause(Offset(center.x, center.y + labelDistance), icon, palette)
}

private fun DrawScope.triangle(tipX: Float, centerY: Float, width: Float, height: Float, pointsRight: Boolean) =
    Path().apply {
        val baseX = if (pointsRight) tipX - width else tipX + width
        moveTo(tipX, centerY)
        lineTo(baseX, centerY - height / 2f)
        lineTo(baseX, centerY + height / 2f)
        close()
    }

private fun DrawScope.drawSkip(at: Offset, s: Float, forward: Boolean, palette: Palette) {
    val dir = if (forward) 1f else -1f
    val h = s * 1.6f
    // Two triangles followed by a bar: ▶▶| (or mirrored |◀◀).
    drawPath(triangle(at.x, at.y, s, h, forward), palette.wheelLabel)
    drawPath(triangle(at.x + dir * s, at.y, s, h, forward), palette.wheelLabel)
    val barX = at.x + dir * (s + s * 0.15f)
    drawRect(
        palette.wheelLabel,
        topLeft = Offset(if (forward) barX else barX - s * 0.25f, at.y - h / 2f),
        size = androidx.compose.ui.geometry.Size(s * 0.25f, h),
    )
}

private fun DrawScope.drawPlayPause(at: Offset, s: Float, palette: Palette) {
    val h = s * 1.6f
    // ▶ then two bars.
    drawPath(triangle(at.x - s * 0.2f, at.y, s, h, pointsRight = true), palette.wheelLabel)
    val barW = s * 0.28f
    drawRect(palette.wheelLabel, Offset(at.x + s * 0.05f, at.y - h / 2f), androidx.compose.ui.geometry.Size(barW, h))
    drawRect(palette.wheelLabel, Offset(at.x + s * 0.05f + barW * 1.8f, at.y - h / 2f), androidx.compose.ui.geometry.Size(barW, h))
}

/** A preloaded click sound. Plays through the "sonification" stream, like UI sounds. */
private class ClickSound(private val pool: SoundPool, private val soundId: Int) {
    fun play() {
        pool.play(soundId, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun release() = pool.release()
}

@Composable
private fun rememberClickSound(): ClickSound {
    val context = LocalContext.current
    val sound = remember(context) {
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        ClickSound(pool, pool.load(context, R.raw.click, 1))
    }
    DisposableEffect(sound) { onDispose { sound.release() } }
    return sound
}

package dev.douglasneto.beatside.wheel

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import dev.douglasneto.beatside.R

/**
 * The click, played through a [SoundPool] with sonification attributes. No audio focus is
 * requested, so music in other apps is never paused or ducked.
 * Call [release] when the wheel leaves the composition.
 */
class SoundPoolClickSound(context: Context) : ClickSound {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    @Volatile private var loaded = false
    private val soundId: Int

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (id == soundId && status == 0) loaded = true }
        soundId = pool.load(context, R.raw.click, 1)
    }

    override fun play() {
        if (loaded) pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}

/** Haptics through the wheel's [View], so they follow the system touch-feedback setting. */
class ViewHaptics(private val view: View) : HapticOutput {
    override fun tick() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK
            else HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    override fun press() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY,
        )
    }
}

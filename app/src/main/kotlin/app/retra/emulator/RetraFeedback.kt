package app.retra.emulator

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/** Small, intentional feedback cues. Game audio is handled separately by [AudioOutput]. */
enum class FeedbackCue {
    TAP,
    GAME_BUTTON,
    CONFIRM,
    SAVE,
    ACHIEVEMENT,
    INVITE,
    ERROR
}

@Singleton
class RetraFeedbackEngine @Inject constructor(
    @ApplicationContext context: Context
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
        .also { pool ->
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) loadedSoundIds += sampleId
            }
        }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val sounds = mapOf(
        FeedbackCue.TAP to load(R.raw.retra_tap),
        FeedbackCue.CONFIRM to load(R.raw.retra_confirm),
        FeedbackCue.SAVE to load(R.raw.retra_save),
        FeedbackCue.ACHIEVEMENT to load(R.raw.retra_achievement),
        FeedbackCue.INVITE to load(R.raw.retra_invite),
        FeedbackCue.ERROR to load(R.raw.retra_error)
    )

    @Volatile private var hapticsEnabled = true
    @Volatile private var soundsEnabled = true
    @Volatile private var soundVolume = 0.55f

    fun configure(haptics: Boolean, sounds: Boolean, volume: Float) {
        hapticsEnabled = haptics
        soundsEnabled = sounds
        soundVolume = volume.coerceIn(0f, 1f)
    }

    fun emit(cue: FeedbackCue) {
        if (soundsEnabled && soundVolume > 0f) {
            sounds[cue]?.takeIf { it != 0 && it in loadedSoundIds }?.let { soundId ->
                soundPool.play(soundId, soundVolume, soundVolume, 1, 0, 1f)
            }
        }
        if (hapticsEnabled) vibrate(cue)
    }

    private fun load(@RawRes resource: Int): Int = soundPool.load(appContext, resource, 1)

    private fun vibrate(cue: FeedbackCue) {
        val target = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(
                when (cue) {
                    FeedbackCue.TAP, FeedbackCue.GAME_BUTTON -> VibrationEffect.EFFECT_TICK
                    FeedbackCue.CONFIRM, FeedbackCue.SAVE -> VibrationEffect.EFFECT_CLICK
                    FeedbackCue.ACHIEVEMENT, FeedbackCue.INVITE -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    FeedbackCue.ERROR -> VibrationEffect.EFFECT_HEAVY_CLICK
                }
            )
        } else {
            // API 26-28 has no predefined effects. Keep the fallback intentionally brief.
            VibrationEffect.createOneShot(
                when (cue) {
                    FeedbackCue.TAP, FeedbackCue.GAME_BUTTON -> 10L
                    FeedbackCue.CONFIRM, FeedbackCue.SAVE -> 16L
                    FeedbackCue.ACHIEVEMENT, FeedbackCue.INVITE -> 22L
                    FeedbackCue.ERROR -> 28L
                },
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        }
        target.vibrate(effect)
    }

    override fun close() {
        soundPool.release()
    }
}

package app.retra.emulator

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import app.retra.emulation.api.AudioPacket
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Bounded streaming output. Packets may be dropped rather than blocking emulation. */
@Singleton
class AudioOutput @Inject constructor(
    @ApplicationContext context: Context
) : AutoCloseable {
    private val lock = Any()
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setAcceptsDelayedFocusGain(false)
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    synchronized(lock) {
                        focusHeld = false
                        pause(abandonFocus = false)
                    }
                }
            }
        }
        .build()
    private var track: AudioTrack? = null
    private var formatKey: Pair<Int, Int>? = null
    private var enabled = false
    private var focusHeld = false

    fun start() {
        synchronized(lock) {
            val result = audioManager.requestAudioFocus(focusRequest)
            focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            enabled = focusHeld
            if (enabled) track?.play()
        }
    }

    fun pause() = pause(abandonFocus = true)

    fun write(packet: AudioPacket) {
        synchronized(lock) {
            if (!enabled) return
            val current = ensureTrack(packet.sampleRate, packet.channelCount)
            if (current.playState != AudioTrack.PLAYSTATE_PLAYING) current.play()
            current.write(packet.pcm16, 0, packet.pcm16.size, AudioTrack.WRITE_NON_BLOCKING)
        }
    }

    private fun pause(abandonFocus: Boolean) {
        synchronized(lock) {
            enabled = false
            runCatching { track?.pause() }
            runCatching { track?.flush() }
            if (abandonFocus && focusHeld) {
                audioManager.abandonAudioFocusRequest(focusRequest)
                focusHeld = false
            }
        }
    }

    private fun ensureTrack(sampleRate: Int, channels: Int): AudioTrack {
        val key = sampleRate to channels
        track?.takeIf { formatKey == key && it.state == AudioTrack.STATE_INITIALIZED }?.let { return it }
        releaseTrack()
        val channelMask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minimum = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = minimum.coerceAtLeast(sampleRate * channels * 2 / 10)
        val created = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        check(created.state == AudioTrack.STATE_INITIALIZED) { "Android could not initialize game audio." }
        track = created
        formatKey = key
        return created
    }

    override fun close() {
        synchronized(lock) {
            pause(abandonFocus = true)
            releaseTrack()
        }
    }

    private fun releaseTrack() {
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
        formatKey = null
    }
}

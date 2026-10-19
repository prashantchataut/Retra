package app.retra.emu

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build

/** Where the session sends audio. */
interface AudioSink {
    fun write(samples: ShortArray, frames: Int)
    /** 0 = empty, 1 = full. Used to nudge frame pacing so the buffer stays half full. */
    fun fillLevel(): Float
    fun setVolume(volume: Float)
    fun pause()
    fun resume()
    fun release()
}

/** Low-latency streaming AudioTrack (32768 Hz stereo s16) with non-blocking writes. */
class AudioOutput : AudioSink {
    private val bufferFrames: Int
    private val track: AudioTrack
    private var written = 0L
    private var baseline = 0L

    init {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(EmuCore.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minBytes = AudioTrack.getMinBufferSize(EmuCore.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        // ~4 video frames of audio (~67 ms): small enough to feel immediate, big enough to absorb jitter.
        bufferFrames = maxOf(minBytes / 4, 2200)
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferFrames * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
        if (Build.VERSION.SDK_INT >= 26) builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        track = builder.build()
        track.play()
    }

    override fun write(samples: ShortArray, frames: Int) {
        if (frames <= 0) return
        val n = track.write(samples, 0, frames * 2, AudioTrack.WRITE_NON_BLOCKING)
        if (n > 0) written += n / 2 // frames that didn't fit are dropped (buffer already full)
    }

    override fun fillLevel(): Float {
        var played = (track.playbackHeadPosition.toLong() and 0xFFFFFFFFL) - baseline
        if (played < 0) played += 1L shl 32
        val queued = written - played
        return (queued.toFloat() / bufferFrames).coerceIn(0f, 1f)
    }

    override fun setVolume(volume: Float) {
        track.setVolume(volume.coerceIn(0f, 1f))
    }

    override fun pause() {
        runCatching { track.pause(); track.flush() }
    }

    override fun resume() {
        written = 0
        baseline = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
        runCatching { track.play() }
    }

    override fun release() {
        runCatching { track.stop() }
        track.release()
    }
}

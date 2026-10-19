package app.retra.engine

import java.io.File

/**
 * Smoke test for the JNI bridge: loads a ROM, runs frames, round-trips a save state and a battery
 * save, and fails loudly (non-zero exit) if any of it misbehaves.
 *
 * Run through `engine/host/jni_smoke.sh <rom.gba>`.
 */
private fun check(what: String, ok: Boolean) {
    if (!ok) {
        System.err.println("FAIL: $what")
        kotlin.system.exitProcess(1)
    }
    println("ok: $what")
}

fun main(args: Array<String>) {
    val rom = File(args.firstOrNull() ?: error("usage: SmokeKt <rom.gba>")).readBytes()
    GbaCore().use { core ->
        val loaded = core.loadRom(rom)
        check("loadRom -> $loaded", loaded is LoadResult.Ok)
        val info = (loaded as LoadResult.Ok).info
        println("title='${info.title}' code='${info.gameCode}' save=${info.saveType} rtc=${info.hasRtc}")

        val video = IntArray(GbaCore.WIDTH * GbaCore.HEIGHT)
        val audio = ShortArray(2 * GbaCore.MAX_AUDIO_FRAMES)

        // Boot frames: the game must paint something and produce audio.
        var nonBlack = 0
        var audioFrames = 0
        repeat(120) {
            audioFrames = core.runFrame(video, audio)
            if (video.count { (it and 0xFFFFFF) != 0 } > 100) nonBlack++
        }
        check("video painted ($nonBlack/120 frames non-blank)", nonBlack > 60)
        check("audio produced ($audioFrames stereo frames)", audioFrames > 0)

        core.setKeys(GbaKey.START or GbaKey.A)
        repeat(10) { core.runFrame(video, audio) }
        core.setKeys(0)
        repeat(10) { core.runFrame(video, audio) }

        val state = core.saveState()
        check("saveState (${state.size} bytes)", state.isNotEmpty())
        check("loadState", core.loadState(state))

        val accepted = core.setCheats(listOf("0101F1C2:00000063"))
        check("setCheats accepted $accepted", accepted >= 0)

        core.setColorCorrection(true)
        core.setRtcOffsetSeconds(0)
        core.reset()
        repeat(5) { core.runFrame(video, audio) }

        val save = core.batterySave()
        if (save != null) {
            check("batterySave round-trip (${save.size} bytes)", core.loadBatterySave(save))
            core.consumeSaveDirty()
        } else {
            println("ok: no battery save (SaveType.NONE)")
        }
    }
    println("JNI smoke test passed")
}

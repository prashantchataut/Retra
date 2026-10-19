// Host smoke test of the Kotlin API + JNI bridge (see ../jni_smoke.sh).
import app.retra.engine.*
import java.io.File

fun main(args: Array<String>) {
    GbaCore().use { core ->
        val r = core.loadRom(File(args[0]).readBytes())
        println(r)
        val video = IntArray(GbaCore.WIDTH * GbaCore.HEIGHT)
        val audio = ShortArray(2 * GbaCore.MAX_AUDIO_FRAMES)
        var total = 0
        for (i in 0 until 600) {
            core.setKeys(if (i in 200..205 || i in 300..305 || i in 400..405) GbaKey.START else if (i > 500) GbaKey.RIGHT else 0)
            total += core.runFrame(video, audio)
        }
        println("audio=$total px=${Integer.toHexString(video[120 * 240 + 120])}")
        val st = core.saveState()
        println("state=${st.size} load=${core.loadState(st)}")
        println("sav=${core.batterySave()?.size} dirty=${core.consumeSaveDirty()}")
        println("cheats=${core.setCheats(listOf("02000000:01", "GS:12345678 9ABCDEF0", "bogus"))}")
        core.setColorCorrection(true)
        core.setRtcOffsetSeconds(3600)
        core.reset()
        println("after reset frames=${core.runFrame(video, audio)}")
        println(core.loadRom(ByteArray(16)))
    }
    println("closed ok")
}


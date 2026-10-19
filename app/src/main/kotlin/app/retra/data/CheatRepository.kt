package app.retra.data

import app.retra.core.AtomicFiles
import app.retra.core.RetraJson
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Per-game cheat lists in `cheats/<id>.json`. Blocking; call off the main thread. */
class CheatRepository(root: File) {
    private val dir = File(root, "cheats")
    private val serializer = ListSerializer(Cheat.serializer())

    private fun file(id: String) = File(dir, "$id.json")

    fun cheats(id: String): List<Cheat> = runCatching {
        val f = file(id)
        if (f.exists()) RetraJson.decodeFromString(serializer, f.readText()) else emptyList()
    }.getOrDefault(emptyList())

    fun save(id: String, cheats: List<Cheat>) {
        if (cheats.isEmpty()) file(id).delete() else AtomicFiles.writeText(file(id), RetraJson.encodeToString(serializer, cheats))
    }

    fun delete(id: String) {
        file(id).delete()
    }

    /** The code strings to hand to the engine. */
    fun enabledCodes(id: String): List<String> = cheats(id).filter { it.enabled }.map { it.code }
}

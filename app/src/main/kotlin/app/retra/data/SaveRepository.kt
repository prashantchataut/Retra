package app.retra.data

import app.retra.core.AtomicFiles
import java.io.File

/**
 * Battery saves (`saves/<id>.sav` + rotating backups), save states (`states/<id>/slotN.state|png`)
 * and screenshots (`screens/<id>.png` = last session, `screens/captures/` = user screenshots).
 * All methods are blocking; call them from a background thread.
 */
class SaveRepository(private val root: File, private val clock: () -> Long = System::currentTimeMillis) {
    val savesDir = File(root, "saves")
    private val backupsDir = File(savesDir, "backups")
    private val statesDir = File(root, "states")
    val screensDir = File(root, "screens")
    val capturesDir = File(screensDir, "captures")

    companion object {
        const val MAX_BACKUPS = 5
        const val AUTO_SLOT = 0
        const val SLOT_COUNT = 6
    }

    fun savFile(id: String) = File(savesDir, "$id.sav")

    fun readBatterySave(id: String): ByteArray? = savFile(id).takeIf { it.exists() && it.length() > 0 }?.readBytes()

    /**
     * Atomically replaces the battery save. When [backup] is true the previous file is first kept
     * in `saves/backups/<id>/`, and only the newest [MAX_BACKUPS] backups are retained.
     */
    fun writeBatterySave(id: String, data: ByteArray, backup: Boolean) {
        val target = savFile(id)
        if (backup && target.exists() && target.length() > 0) {
            val existing = target.readBytes()
            if (!existing.contentEquals(data)) {
                val dir = File(backupsDir, id)
                dir.mkdirs()
                var stamp = clock()
                while (File(dir, "$stamp.sav").exists()) stamp++
                AtomicFiles.write(File(dir, "$stamp.sav"), existing)
                pruneBackups(id)
            }
        }
        AtomicFiles.write(target, data)
    }

    fun backups(id: String): List<File> =
        File(backupsDir, id).listFiles { f -> f.name.endsWith(".sav") }
            ?.sortedByDescending { it.name.removeSuffix(".sav").toLongOrNull() ?: 0L }
            ?: emptyList()

    private fun pruneBackups(id: String) {
        backups(id).drop(MAX_BACKUPS).forEach { it.delete() }
    }

    fun stateFile(id: String, slot: Int) = File(File(statesDir, id), "slot$slot.state")
    fun thumbFile(id: String, slot: Int) = File(File(statesDir, id), "slot$slot.png")

    fun writeState(id: String, slot: Int, state: ByteArray, thumbnailPng: ByteArray?) {
        AtomicFiles.write(stateFile(id, slot), state)
        if (thumbnailPng != null) AtomicFiles.write(thumbFile(id, slot), thumbnailPng)
    }

    fun readState(id: String, slot: Int): ByteArray? = stateFile(id, slot).takeIf { it.exists() }?.readBytes()

    fun hasAutoState(id: String) = stateFile(id, AUTO_SLOT).exists()

    fun slots(id: String): List<StateSlot> = (0..SLOT_COUNT).map { slot ->
        val f = stateFile(id, slot)
        val t = thumbFile(id, slot)
        StateSlot(
            slot = slot,
            exists = f.exists(),
            savedAt = if (f.exists()) f.lastModified() else null,
            thumbnailPath = if (f.exists() && t.exists()) t.absolutePath else null,
        )
    }

    fun deleteState(id: String, slot: Int) {
        stateFile(id, slot).delete()
        thumbFile(id, slot).delete()
    }

    /** Screenshot of the end of the last session, used as cover art and on the Continue card. */
    fun lastScreenFile(id: String) = File(screensDir, "$id.png")

    fun writeLastScreen(id: String, png: ByteArray) = AtomicFiles.write(lastScreenFile(id), png)

    fun newCaptureFile(id: String): File {
        capturesDir.mkdirs()
        return File(capturesDir, "$id-${clock()}.png")
    }

    fun deleteAll(id: String) {
        savFile(id).delete()
        File(backupsDir, id).deleteRecursively()
        File(statesDir, id).deleteRecursively()
        lastScreenFile(id).delete()
    }

    fun savesBytes(): Long = savesDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    fun statesBytes(): Long = statesDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    fun screensBytes(): Long = screensDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
}

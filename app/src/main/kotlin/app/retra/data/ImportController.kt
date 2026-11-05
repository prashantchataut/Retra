package app.retra.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.retra.core.AtomicFiles
import app.retra.core.FolderPlan
import app.retra.core.SaveFiles
import app.retra.core.RomImport
import app.retra.patcher.PatchArchive
import app.retra.patcher.Patcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads user-picked or shared files and routes them: games to the library, patches to the Patcher. */
class ImportController(
    private val context: Context,
    private val library: LibraryRepository,
    private val messages: Messages,
    private val saves: SaveRepository? = null,
) {
    data class Outcome(val added: List<Game>, val duplicates: List<Game>, val errors: List<String>, val patch: IncomingFile.Patch?)

    private val patchExt = setOf("ips", "ups", "bps", "xdelta", "vcdiff", "xdelta3", "delta")

    fun displayName(uri: Uri): String {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }

    suspend fun readBytes(uri: Uri, max: Long = 64L * 1024 * 1024): ByteArray = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Couldn't open the file.")
        input.use {
            val bytes = it.readNBytesCompat((max + 1).toInt())
            if (bytes.size > max) throw java.io.IOException("This file is too big for a GBA game or patch.")
            bytes
        }
    }

    suspend fun importUris(uris: List<Uri>): Outcome {
        val added = ArrayList<Game>()
        val dups = ArrayList<Game>()
        val errors = ArrayList<String>()
        var patch: IncomingFile.Patch? = null
        for (uri in uris) {
            val name = displayName(uri)
            val bytes = try {
                readBytes(uri)
            } catch (e: Exception) {
                errors += "$name: ${e.message ?: "couldn't be read"}"
                continue
            }
            if (isPatch(bytes, name)) {
                patch = stashPatch(bytes, name)
                continue
            }
            when (val r = library.import(bytes, name)) {
                is ImportResult.Added -> added += r.game
                is ImportResult.Duplicate -> dups += r.game
                is ImportResult.Error -> {
                    // A zip without a ROM might hold a patch.
                    if (RomImport.isZip(bytes) && PatchArchive.extractSingle(bytes) != null) patch = stashPatch(bytes, name)
                    else errors += "$name: ${r.message}"
                }
            }
        }
        report(added, dups, errors)
        return Outcome(added, dups, errors, patch)
    }

    private fun report(added: List<Game>, dups: List<Game>, errors: List<String>) {
        when {
            added.size == 1 && errors.isEmpty() -> messages.show("Added ${added[0].title}", MessageTone.SUCCESS)
            added.size > 1 && errors.isEmpty() -> messages.show("Added ${added.size} games", MessageTone.SUCCESS)
            added.isEmpty() && dups.isNotEmpty() && errors.isEmpty() ->
                messages.show(if (dups.size == 1) "${dups[0].title} is already in your games" else "Those games are already in your library")
            errors.isNotEmpty() && added.isEmpty() -> messages.show(errors.first().substringAfter(": "), MessageTone.ERROR)
            errors.isNotEmpty() -> messages.show("Added ${added.size}, ${errors.size} couldn't be imported", MessageTone.INFO)
        }
    }

    // ---- Folders and .sav files ----

    data class FolderProgress(val done: Int, val total: Int, val current: String)

    data class FolderOutcome(
        val scanned: Int,
        val added: List<Game>,
        val duplicates: List<Game>,
        val savesLoaded: Int,
        val errors: List<String>,
        val patch: IncomingFile.Patch?,
    )

    /** Lists the importable files in a picked folder (up to 4 levels deep, 3000 files). */
    suspend fun scanFolder(tree: Uri): Pair<FolderPlan.Plan, Map<String, Uri>> = withContext(Dispatchers.IO) {
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, tree)
        val entries = ArrayList<FolderPlan.Entry>()
        val uris = HashMap<String, Uri>()
        fun walk(dir: androidx.documentfile.provider.DocumentFile?, depth: Int) {
            if (dir == null || depth > 4 || entries.size >= 3000) return
            for (f in dir.listFiles()) {
                if (f.isDirectory) walk(f, depth + 1)
                else {
                    val name = f.name ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in FolderPlan.romExtensions || ext in FolderPlan.patchExtensions || ext in SaveFiles.extensions) {
                        val key = f.uri.toString()
                        entries += FolderPlan.Entry(name, key, f.length())
                        uris[key] = f.uri
                    }
                }
            }
        }
        walk(root, 0)
        FolderPlan.plan(entries) to uris
    }

    /** Adds every game in a folder; a save next to a game (same file name) is loaded into it. */
    suspend fun importFolder(tree: Uri, onProgress: (FolderProgress) -> Unit = {}): FolderOutcome {
        val (plan, uris) = scanFolder(tree)
        val added = ArrayList<Game>()
        val dups = ArrayList<Game>()
        val errors = ArrayList<String>()
        var loaded = 0
        var patch: IncomingFile.Patch? = null
        val total = plan.roms.size + plan.patches.size
        var done = 0
        for (rom in plan.roms) {
            onProgress(FolderProgress(done++, total, rom.name))
            val uri = uris[rom.uri] ?: continue
            val bytes = try { readBytes(uri) } catch (e: Exception) { errors += "${rom.name}: ${e.message ?: "couldn't be read"}"; continue }
            val game = when (val r = library.import(bytes, rom.name)) {
                is ImportResult.Added -> r.game.also { added += it }
                is ImportResult.Duplicate -> r.game.also { dups += it }
                is ImportResult.Error -> { errors += "${rom.name}: ${r.message}"; null }
            } ?: continue
            val save = plan.pairs[rom]
            if (save != null) {
                val isNew = game in added
                val hasSave = saves?.readBatterySave(game.id) != null
                if (isNew || !hasSave) {
                    when (val r = loadSaveFile(game, uris[save.uri] ?: continue)) {
                        is SaveFiles.Result.Ok -> loaded++
                        is SaveFiles.Result.Error -> errors += "${save.name}: ${r.message}"
                    }
                }
            }
        }
        for (p in plan.patches.take(1)) {
            onProgress(FolderProgress(done++, total, p.name))
            val uri = uris[p.uri] ?: continue
            runCatching { stashPatch(readBytes(uri), p.name) }.onSuccess { patch = it }
        }
        onProgress(FolderProgress(total, total, ""))
        val outcome = FolderOutcome(plan.roms.size + plan.saves.size + plan.patches.size, added, dups, loaded, errors, patch)
        messages.show(folderSummary(outcome), if (added.isEmpty() && loaded == 0) MessageTone.INFO else MessageTone.SUCCESS)
        return outcome
    }

    fun folderSummary(o: FolderOutcome): String {
        val parts = ArrayList<String>()
        if (o.added.isNotEmpty()) parts += "${o.added.size} game${if (o.added.size == 1) "" else "s"} added"
        if (o.savesLoaded > 0) parts += "${o.savesLoaded} save${if (o.savesLoaded == 1) "" else "s"} loaded"
        if (o.duplicates.isNotEmpty()) parts += "${o.duplicates.size} already in your library"
        if (o.errors.isNotEmpty()) parts += "${o.errors.size} skipped"
        return if (parts.isEmpty()) "No GBA games found in that folder." else parts.joinToString(", ")
    }

    /** Reads a save file, fits it to [game]'s save chip and writes it (the old save is backed up). */
    suspend fun loadSaveFile(game: Game, uri: Uri): SaveFiles.Result = withContext(Dispatchers.IO) {
        val repo = saves ?: return@withContext SaveFiles.Result.Error("Saves aren't available right now.")
        val bytes = try { readBytes(uri, max = 1024L * 1024) } catch (e: Exception) {
            return@withContext SaveFiles.Result.Error(e.message ?: "Couldn't read that file.")
        }
        val result = SaveFiles.normalize(bytes, game.saveType)
        if (result is SaveFiles.Result.Ok) {
            repo.writeBatterySave(game.id, result.data, backup = true)
            // A Continue snapshot would resurrect the old progress over the freshly loaded save.
            repo.deleteState(game.id, SaveRepository.AUTO_SLOT)
        }
        result
    }

    fun isPatch(bytes: ByteArray, name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return Patcher.detect(bytes) != null || ext in patchExt
    }

    /** Patches are kept in cache until the Patcher picks them up. */
    suspend fun stashPatch(bytes: ByteArray, name: String): IncomingFile.Patch = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "incoming").apply { mkdirs() }
        val safe = name.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(80).ifBlank { "patch" }
        val f = File(dir, "${System.currentTimeMillis()}-$safe")
        AtomicFiles.write(f, bytes)
        IncomingFile.Patch(f.absolutePath, name)
    }
}

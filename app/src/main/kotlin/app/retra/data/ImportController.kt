package app.retra.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.retra.core.AtomicFiles
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

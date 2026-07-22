package app.retra.emulator.data

import android.content.Context
import android.net.Uri
import app.retra.core.rom.Sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BundledPatchRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun prepareHeartAndSoulV121(): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.assets.open(ASSET_PATH).use { input ->
                input.readBytes().also { require(it.size <= MAX_PATCH_BYTES) { "Bundled patch exceeds Retra's safety limit." } }
            }
            require(Sha256.of(bytes).equals(EXPECTED_SHA256, ignoreCase = true)) {
                "Bundled Heart & Soul patch failed its SHA-256 integrity check."
            }
            val directory = File(context.filesDir, "reviewed-patches").apply { mkdirs() }
            val target = File(directory, "$EXPECTED_SHA256.ups")
            if (!target.isFile || target.length() != bytes.size.toLong()) {
                val temporary = File(directory, ".${target.name}.tmp")
                try {
                    FileOutputStream(temporary).use { output ->
                        output.write(bytes)
                        output.fd.sync()
                    }
                    try {
                        Files.move(
                            temporary.toPath(),
                            target.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                        )
                    } catch (_: Exception) {
                        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally {
                    if (temporary.exists()) temporary.delete()
                }
            }
            Uri.parse(target.toURI().toString())
        }
    }

    private companion object {
        const val ASSET_PATH = "patches/pokemon_hns_v1_2_1.ups"
        const val EXPECTED_SHA256 = "c8e70f448b481d2980266ca8f021aa8b3c462f4e582cf80228ced4636c6154eb"
        const val MAX_PATCH_BYTES = 32 * 1024 * 1024
    }
}

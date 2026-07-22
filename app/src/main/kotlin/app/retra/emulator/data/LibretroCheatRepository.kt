package app.retra.emulator.data

import app.retra.core.cheats.RetroArchCheatParser
import app.retra.core.model.GameRecord
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LibretroCheatRepository @Inject constructor(
    private val cheatRepository: CheatRepository
) {
    suspend fun installFor(game: GameRecord): CheatPackImportOutcome = withContext(Dispatchers.IO) {
        val title = game.canonicalTitle?.trim().orEmpty().ifBlank { game.title.trim() }
        if (title.isBlank()) {
            return@withContext CheatPackImportOutcome.Rejected("Sync game metadata first so Retra can identify the matching cheat file.")
        }
        val candidates = buildList {
            add(title)
            val withoutRevision = title.replace(Regex(" \\(Rev [^)]+\\)$", RegexOption.IGNORE_CASE), "")
            if (withoutRevision != title) add(withoutRevision)
        }.distinct()

        var lastReason = "No matching Libretro cheat file was found for $title."
        for (candidate in candidates) {
            when (val download = download(candidate)) {
                is DownloadResult.Found -> {
                    return@withContext cheatRepository.importRetroArchBytes(
                        game = game,
                        fileName = "$candidate.cht",
                        bytes = download.bytes,
                        provider = "Libretro Database (CC-BY-SA-4.0)"
                    )
                }
                is DownloadResult.Missing -> lastReason = download.reason
                is DownloadResult.Failed -> return@withContext CheatPackImportOutcome.Rejected(download.reason)
            }
        }
        CheatPackImportOutcome.Rejected(lastReason)
    }

    private fun download(title: String): DownloadResult {
        val encoded = URLEncoder.encode("$title.cht", StandardCharsets.UTF_8.name()).replace("+", "%20")
        val url = "$RAW_BASE/$encoded"
        val uri = URI(url)
        if (!uri.scheme.equals("https", true) || !uri.host.equals("raw.githubusercontent.com", true)) {
            return DownloadResult.Failed("The Libretro cheat source origin changed and was blocked.")
        }
        val connection = uri.toURL().openConnection() as? HttpsURLConnection
            ?: return DownloadResult.Failed("The Libretro cheat source did not use HTTPS.")
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "text/plain, application/octet-stream")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("User-Agent", "Retra/2.0 Android")
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    if (connection.contentLengthLong > RetroArchCheatParser.MAX_FILE_BYTES) {
                        DownloadResult.Failed("Libretro cheat file exceeds 512 KiB.")
                    } else {
                        val output = ByteArrayOutputStream()
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(8 * 1024)
                            var total = 0
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                total += read
                                if (total > RetroArchCheatParser.MAX_FILE_BYTES) {
                                    return DownloadResult.Failed("Libretro cheat file exceeds 512 KiB.")
                                }
                                output.write(buffer, 0, read)
                            }
                        }
                        DownloadResult.Found(output.toByteArray())
                    }
                }
                HttpURLConnection.HTTP_NOT_FOUND -> DownloadResult.Missing("No matching Libretro cheat file was found for $title.")
                else -> DownloadResult.Failed("Libretro cheat source returned HTTP $code.")
            }
        } catch (error: Exception) {
            DownloadResult.Failed(error.message ?: "Libretro cheat download failed.")
        } finally {
            connection.disconnect()
        }
    }

    private sealed interface DownloadResult {
        data class Found(val bytes: ByteArray) : DownloadResult
        data class Missing(val reason: String) : DownloadResult
        data class Failed(val reason: String) : DownloadResult
    }

    companion object {
        private const val RAW_BASE = "https://raw.githubusercontent.com/libretro/libretro-database/master/cht/Nintendo%20-%20Game%20Boy%20Advance"
    }
}

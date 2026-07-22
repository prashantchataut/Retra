package app.retra.core.cheats

import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class InvalidCheatCatalogException(message: String) : IllegalArgumentException(message)

data class CheatCatalogEntry(
    val id: String,
    val title: String,
    val description: String,
    val provider: String,
    val gameSha256: String,
    val gameCode: String?,
    val revision: Int?,
    val downloadUrl: String,
    val packSha256: String,
    val license: String,
    val distributionPermission: String,
    val sourcePageUrl: String?
)

data class CheatCatalog(
    val catalogId: String,
    val name: String,
    val provider: String,
    val sourcePageUrl: String,
    val entries: List<CheatCatalogEntry>
)

object RetraCheatCatalogParser {
    const val HEADER = "RETRA-CHEAT-INDEX-1"
    const val MAX_CATALOG_BYTES = 1024 * 1024
    const val MAX_ENTRIES = 256
    private const val MAX_LINE_LENGTH = 4096

    private val globalKeys = setOf("catalogId", "name", "provider", "sourcePageUrl")
    private val entryKeys = setOf(
        "id", "title", "description", "provider", "gameSha256", "gameCode", "revision",
        "downloadUrl", "packSha256", "license", "distributionPermission", "sourcePageUrl"
    )
    private val idPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")
    private val hashPattern = Regex("[0-9a-fA-F]{64}")
    private val gameCodePattern = Regex("[A-Za-z0-9]{4}")

    fun parse(bytes: ByteArray): CheatCatalog {
        if (bytes.isEmpty()) throw InvalidCheatCatalogException("Cheat index is empty.")
        if (bytes.size > MAX_CATALOG_BYTES) throw InvalidCheatCatalogException("Cheat index exceeds 1 MiB.")
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = runCatching { decoder.decode(ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { throw InvalidCheatCatalogException("Cheat index is not valid UTF-8.") }
        val lines = text.lineSequence().mapIndexed { index, raw -> IndexedLine(index + 1, raw) }.toList()
        lines.forEach { line ->
            if (line.raw.length > MAX_LINE_LENGTH) {
                throw InvalidCheatCatalogException("Line ${line.number} exceeds $MAX_LINE_LENGTH characters.")
            }
        }
        val meaningful = lines.filterNot { it.value.isBlank() || it.value.startsWith('#') }
        if (meaningful.firstOrNull()?.value != HEADER) {
            throw InvalidCheatCatalogException("Cheat index header $HEADER is missing.")
        }

        val globals = linkedMapOf<String, String>()
        val entries = mutableListOf<Map<String, String>>()
        var current: LinkedHashMap<String, String>? = null
        meaningful.drop(1).forEach { line ->
            when (line.value) {
                "[pack]" -> {
                    if (current != null) throw InvalidCheatCatalogException("Nested [pack] block at line ${line.number}.")
                    current = linkedMapOf()
                }
                "[/pack]" -> {
                    val complete = current ?: throw InvalidCheatCatalogException("Unexpected [/pack] at line ${line.number}.")
                    entries += complete
                    if (entries.size > MAX_ENTRIES) throw InvalidCheatCatalogException("Cheat index exceeds $MAX_ENTRIES entries.")
                    current = null
                }
                else -> {
                    val separator = line.value.indexOf('=')
                    if (separator <= 0) throw InvalidCheatCatalogException("Expected key=value at line ${line.number}.")
                    val key = line.value.substring(0, separator).trim()
                    val value = line.value.substring(separator + 1).trim()
                    if (value.isEmpty()) throw InvalidCheatCatalogException("Empty value for $key at line ${line.number}.")
                    if (current == null) {
                        if (key !in globalKeys) throw InvalidCheatCatalogException("Unknown index field $key at line ${line.number}.")
                        if (globals.put(key, value) != null) throw InvalidCheatCatalogException("Duplicate index field $key.")
                    } else {
                        if (key !in entryKeys) throw InvalidCheatCatalogException("Unknown pack field $key at line ${line.number}.")
                        if (current!!.put(key, value) != null) throw InvalidCheatCatalogException("Duplicate pack field $key at line ${line.number}.")
                    }
                }
            }
        }
        if (current != null) throw InvalidCheatCatalogException("Unclosed [pack] block.")
        if (entries.isEmpty()) throw InvalidCheatCatalogException("Cheat index contains no packs.")

        val catalogId = globals.required("catalogId").bounded("catalogId", 80)
        if (!idPattern.matches(catalogId)) throw InvalidCheatCatalogException("catalogId must use safe ASCII characters.")
        val sourcePageUrl = validateHttpsPage(globals.required("sourcePageUrl"), "sourcePageUrl")
        val parsedEntries = entries.mapIndexed { index, values -> parseEntry(index, values, globals.required("provider")) }
        val duplicateId = parsedEntries.groupingBy(CheatCatalogEntry::id).eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicateId != null) throw InvalidCheatCatalogException("Duplicate pack ID: $duplicateId.")

        return CheatCatalog(
            catalogId = catalogId,
            name = globals.required("name").bounded("name", 200),
            provider = globals.required("provider").bounded("provider", 200),
            sourcePageUrl = sourcePageUrl,
            entries = parsedEntries
        )
    }

    fun matches(entry: CheatCatalogEntry, gameSha256: String, gameCode: String, revision: Int): Boolean {
        if (!entry.gameSha256.equals(gameSha256, ignoreCase = true)) return false
        if (entry.gameCode != null && !entry.gameCode.equals(gameCode, ignoreCase = true)) return false
        if (entry.revision != null && entry.revision != revision) return false
        return true
    }

    private fun parseEntry(index: Int, values: Map<String, String>, fallbackProvider: String): CheatCatalogEntry {
        fun field(name: String) = values[name] ?: throw InvalidCheatCatalogException("Pack $index is missing $name.")
        val id = field("id").bounded("packs[$index].id", 80)
        if (!idPattern.matches(id)) throw InvalidCheatCatalogException("packs[$index].id must use safe ASCII characters.")
        val gameHash = field("gameSha256")
        if (!hashPattern.matches(gameHash)) throw InvalidCheatCatalogException("packs[$index].gameSha256 must contain 64 hexadecimal characters.")
        val packHash = field("packSha256")
        if (!hashPattern.matches(packHash)) throw InvalidCheatCatalogException("packs[$index].packSha256 must contain 64 hexadecimal characters.")
        val gameCode = values["gameCode"]?.uppercase()?.also {
            if (!gameCodePattern.matches(it)) throw InvalidCheatCatalogException("packs[$index].gameCode must contain four letters or digits.")
        }
        val revision = values["revision"]?.toIntOrNull()?.also {
            if (it !in 0..255) throw InvalidCheatCatalogException("packs[$index].revision must be between 0 and 255.")
        }
        if (values.containsKey("revision") && revision == null) {
            throw InvalidCheatCatalogException("packs[$index].revision must be an integer.")
        }
        val downloadUrl = field("downloadUrl").bounded("packs[$index].downloadUrl", 2048)
        runCatching { RetraCodesDownloadPolicy.validate(CheatPackDownloadRequest(downloadUrl, packHash)) }
            .getOrElse { throw InvalidCheatCatalogException("packs[$index]: ${it.message ?: "unsafe pack URL"}") }
        val sourcePage = values["sourcePageUrl"]?.let { validateHttpsPage(it, "packs[$index].sourcePageUrl") }
        return CheatCatalogEntry(
            id = id,
            title = field("title").bounded("packs[$index].title", 200),
            description = field("description").bounded("packs[$index].description", 2000),
            provider = values["provider"]?.bounded("packs[$index].provider", 200) ?: fallbackProvider,
            gameSha256 = gameHash.lowercase(),
            gameCode = gameCode,
            revision = revision,
            downloadUrl = downloadUrl,
            packSha256 = packHash.lowercase(),
            license = field("license").bounded("packs[$index].license", 200),
            distributionPermission = field("distributionPermission").bounded("packs[$index].distributionPermission", 2000),
            sourcePageUrl = sourcePage
        )
    }

    private fun validateHttpsPage(value: String, field: String): String {
        val bounded = value.bounded(field, 2048)
        val uri = runCatching { URI(bounded) }.getOrElse { throw InvalidCheatCatalogException("$field is malformed.") }
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
            throw InvalidCheatCatalogException("$field must use HTTPS.")
        }
        if (uri.userInfo != null || uri.fragment != null) throw InvalidCheatCatalogException("$field cannot contain credentials or a fragment.")
        return uri.normalize().toString()
    }

    private fun Map<String, String>.required(name: String): String =
        this[name] ?: throw InvalidCheatCatalogException("Missing required index field $name.")

    private fun String.bounded(field: String, maximum: Int): String {
        if (length > maximum) throw InvalidCheatCatalogException("$field exceeds $maximum characters.")
        return this
    }

    private data class IndexedLine(val number: Int, val raw: String) {
        val value: String get() = raw.trim()
    }
}

package app.retra.core.cheats

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class InvalidCheatPackException(message: String) : IllegalArgumentException(message)

enum class CheatFormat { GAMESHARK, CODEBREAKER, ACTION_REPLAY, RAW }
enum class CheatRisk { SAFE, CAUTION, EXPERIMENTAL }
enum class CheatCategory {
    CURRENCY,
    HEALTH,
    EXPERIENCE,
    ENCOUNTERS,
    INVENTORY,
    UNLOCKABLES,
    MOVEMENT,
    DIFFICULTY,
    QUALITY_OF_LIFE,
    VISUAL,
    DEBUGGING,
    EXPERIMENTAL
}

data class CheatDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: CheatCategory,
    val format: CheatFormat,
    val risk: CheatRisk,
    val codeLines: List<String>,
    val dependencies: Set<String>,
    val conflicts: Set<String>
)

data class CheatPack(
    val version: Int,
    val provider: String,
    val gameSha256: String,
    val gameCode: String?,
    val revision: Int?,
    val region: String?,
    val cheats: List<CheatDefinition>
)

data class CheatProfile(
    val name: String,
    val enabledCheatIds: Set<String>
)

data class CheatPackMatch(
    val compatible: Boolean,
    val reasons: List<String>
)

data class CheatProfileValidation(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

object RetraCodesParser {
    const val MAX_PACK_SIZE_BYTES = 512 * 1024
    private const val MAX_LINE_LENGTH = 512
    private const val MAX_CHEATS = 512
    private const val MAX_CODES_PER_CHEAT = 128
    private val idPattern = Regex("[a-z0-9][a-z0-9._-]{0,63}")
    private val hashPattern = Regex("[0-9a-fA-F]{64}")
    private val gameCodePattern = Regex("[A-Za-z0-9]{4}")
    private val genericCodePattern = Regex("[0-9A-Fa-f]{4,16}(?:[ :][0-9A-Fa-f]{2,16}){0,3}")
    private val rawCodePattern = Regex("([0-9A-Fa-f]{8}):(1|2|4):([0-9A-Fa-f]{1,8})")

    fun parse(bytes: ByteArray): CheatPack {
        if (bytes.size > MAX_PACK_SIZE_BYTES) throw InvalidCheatPackException("Retra Codes pack exceeds 512 KiB.")
        val text = decodeUtf8Strict(bytes)
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        if (lines.size > 10_000) throw InvalidCheatPackException("Retra Codes pack contains too many lines.")
        lines.forEachIndexed { index, line ->
            if (line.length > MAX_LINE_LENGTH) throw InvalidCheatPackException("Line ${index + 1} exceeds $MAX_LINE_LENGTH characters.")
        }

        val meaningful = lines.mapIndexedNotNull { index, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#')) null else IndexedLine(index + 1, trimmed)
        }
        if (meaningful.firstOrNull()?.text != "RETRA-CODES-1") {
            throw InvalidCheatPackException("Retra Codes header RETRA-CODES-1 is missing.")
        }

        val globals = linkedMapOf<String, String>()
        val cheats = mutableListOf<CheatDefinition>()
        var current: MutableCheat? = null
        for (line in meaningful.drop(1)) {
            when (line.text) {
                "[cheat]" -> {
                    if (current != null) throw InvalidCheatPackException("Nested [cheat] block at line ${line.number}.")
                    current = MutableCheat(line.number)
                }
                "[/cheat]" -> {
                    val complete = current ?: throw InvalidCheatPackException("Unexpected [/cheat] at line ${line.number}.")
                    cheats += complete.build()
                    if (cheats.size > MAX_CHEATS) throw InvalidCheatPackException("Retra Codes pack exceeds $MAX_CHEATS cheats.")
                    current = null
                }
                else -> {
                    val separator = line.text.indexOf('=')
                    if (separator <= 0) throw InvalidCheatPackException("Expected key=value at line ${line.number}.")
                    val key = line.text.substring(0, separator).trim()
                    val value = line.text.substring(separator + 1).trim()
                    if (value.isEmpty()) throw InvalidCheatPackException("Empty value for $key at line ${line.number}.")
                    if (current == null) {
                        if (key !in GLOBAL_KEYS) throw InvalidCheatPackException("Unknown pack field $key at line ${line.number}.")
                        if (globals.put(key, value) != null) throw InvalidCheatPackException("Duplicate pack field $key.")
                    } else {
                        current.put(key, value, line.number)
                    }
                }
            }
        }
        if (current != null) throw InvalidCheatPackException("Unclosed [cheat] block beginning at line ${current.startLine}.")
        if (cheats.isEmpty()) throw InvalidCheatPackException("Retra Codes pack contains no cheats.")

        val provider = globals.required("provider").bounded("Provider", 120)
        val gameHash = globals.required("gameSha256")
        if (!hashPattern.matches(gameHash)) throw InvalidCheatPackException("gameSha256 must contain exactly 64 hexadecimal characters.")
        val gameCode = globals["gameCode"]?.also {
            if (!gameCodePattern.matches(it)) throw InvalidCheatPackException("gameCode must contain four letters or digits.")
        }?.uppercase()
        val revision = globals["revision"]?.toIntOrNull()?.also {
            if (it !in 0..255) throw InvalidCheatPackException("revision must be between 0 and 255.")
        }
        if (globals.containsKey("revision") && revision == null) throw InvalidCheatPackException("revision must be an integer.")
        val region = globals["region"]?.bounded("Region", 40)

        val duplicateId = cheats.groupingBy(CheatDefinition::id).eachCount().entries.firstOrNull { it.value > 1 }?.key
        if (duplicateId != null) throw InvalidCheatPackException("Duplicate cheat ID: $duplicateId.")
        val knownIds = cheats.mapTo(mutableSetOf(), CheatDefinition::id)
        cheats.forEach { cheat ->
            val unknownDependency = cheat.dependencies.firstOrNull { it !in knownIds }
            if (unknownDependency != null) throw InvalidCheatPackException("Cheat ${cheat.id} depends on unknown cheat $unknownDependency.")
            val unknownConflict = cheat.conflicts.firstOrNull { it !in knownIds }
            if (unknownConflict != null) throw InvalidCheatPackException("Cheat ${cheat.id} conflicts with unknown cheat $unknownConflict.")
            if (cheat.id in cheat.dependencies || cheat.id in cheat.conflicts) {
                throw InvalidCheatPackException("Cheat ${cheat.id} cannot depend on or conflict with itself.")
            }
        }

        return CheatPack(
            version = 1,
            provider = provider,
            gameSha256 = gameHash.lowercase(),
            gameCode = gameCode,
            revision = revision,
            region = region,
            cheats = cheats
        )
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: Exception) {
        throw InvalidCheatPackException("Retra Codes pack is not valid UTF-8 text.")
    }

    private fun Map<String, String>.required(key: String): String =
        this[key] ?: throw InvalidCheatPackException("Required pack field $key is missing.")

    private fun String.bounded(label: String, maximum: Int): String {
        if (length > maximum) throw InvalidCheatPackException("$label exceeds $maximum characters.")
        return this
    }

    private data class IndexedLine(val number: Int, val text: String)

    private class MutableCheat(val startLine: Int) {
        private val single = linkedMapOf<String, String>()
        private val codes = mutableListOf<String>()

        fun put(key: String, value: String, line: Int) {
            if (key !in CHEAT_KEYS) throw InvalidCheatPackException("Unknown cheat field $key at line $line.")
            if (key == "code") {
                if (codes.size >= MAX_CODES_PER_CHEAT) throw InvalidCheatPackException("Cheat at line $startLine contains too many code lines.")
                codes += value
            } else if (single.put(key, value) != null) {
                throw InvalidCheatPackException("Duplicate cheat field $key in block at line $startLine.")
            }
        }

        fun build(): CheatDefinition {
            val id = required("id").lowercase()
            if (!idPattern.matches(id)) throw InvalidCheatPackException("Invalid cheat ID $id at line $startLine.")
            val name = required("name").bounded("Cheat name", 120)
            val description = single["description"]?.bounded("Cheat description", 500).orEmpty()
            val category = enumValue<CheatCategory>("category")
            val format = enumValue<CheatFormat>("format")
            val risk = single["risk"]?.let { parseEnum<CheatRisk>(it, "risk", startLine) } ?: CheatRisk.CAUTION
            if (codes.isEmpty()) throw InvalidCheatPackException("Cheat $id has no code lines.")
            codes.forEach { code -> validateCode(format, code, id) }
            return CheatDefinition(
                id = id,
                name = name,
                description = description,
                category = category,
                format = format,
                risk = risk,
                codeLines = codes.map { it.uppercase() },
                dependencies = parseIds(single["depends"], "depends", id),
                conflicts = parseIds(single["conflicts"], "conflicts", id)
            )
        }

        private fun required(key: String): String =
            single[key] ?: throw InvalidCheatPackException("Cheat block at line $startLine is missing $key.")

        private inline fun <reified T : Enum<T>> enumValue(key: String): T = parseEnum(required(key), key, startLine)
    }

    fun match(pack: CheatPack, gameSha256: String, gameCode: String, revision: Int): CheatPackMatch {
        val reasons = buildList {
            if (!pack.gameSha256.equals(gameSha256, ignoreCase = true)) add("ROM SHA-256 does not match.")
            if (pack.gameCode != null && !pack.gameCode.equals(gameCode, ignoreCase = true)) add("Game code does not match.")
            if (pack.revision != null && pack.revision != revision) add("ROM revision does not match.")
        }
        return CheatPackMatch(reasons.isEmpty(), reasons)
    }

    private fun validateCode(format: CheatFormat, code: String, cheatId: String) {
        val valid = when (format) {
            CheatFormat.RAW -> {
                val match = rawCodePattern.matchEntire(code)
                if (match == null) false else {
                    val width = match.groupValues[2].toInt()
                    val valueDigits = match.groupValues[3].length
                    valueDigits <= width * 2
                }
            }
            else -> genericCodePattern.matches(code)
        }
        if (!valid) throw InvalidCheatPackException("Cheat $cheatId contains an invalid ${format.name} code line: $code")
    }

    private fun parseIds(value: String?, field: String, cheatId: String): Set<String> {
        if (value == null) return emptySet()
        return value.split(',').map { it.trim().lowercase() }.filter(String::isNotEmpty).onEach {
            if (!idPattern.matches(it)) throw InvalidCheatPackException("Cheat $cheatId has invalid $field ID $it.")
        }.toSet()
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String, field: String, line: Int): T =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw InvalidCheatPackException("Unknown $field value $value at line $line.")

    private val GLOBAL_KEYS = setOf("provider", "gameSha256", "gameCode", "revision", "region")
    private val CHEAT_KEYS = setOf("id", "name", "description", "category", "format", "risk", "code", "depends", "conflicts")
}

object CheatConflictAnalyzer {
    private val rawCodePattern = Regex("([0-9A-F]{8}):(1|2|4):([0-9A-F]{1,8})")

    fun validate(pack: CheatPack, profile: CheatProfile): CheatProfileValidation {
        val byId = pack.cheats.associateBy(CheatDefinition::id)
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val enabled = profile.enabledCheatIds.mapNotNull { id ->
            byId[id] ?: run {
                errors += "Profile references unknown cheat $id."
                null
            }
        }
        val enabledIds = enabled.mapTo(mutableSetOf(), CheatDefinition::id)
        enabled.forEach { cheat ->
            cheat.dependencies.filterNot(enabledIds::contains).forEach { missing ->
                errors += "${cheat.name} requires ${byId[missing]?.name ?: missing}."
            }
            cheat.conflicts.filter(enabledIds::contains).forEach { conflict ->
                errors += "${cheat.name} conflicts with ${byId[conflict]?.name ?: conflict}."
            }
            if (cheat.risk == CheatRisk.EXPERIMENTAL) warnings += "${cheat.name} is experimental."
        }

        val writes = linkedMapOf<Pair<Long, Int>, Pair<Long, CheatDefinition>>()
        enabled.filter { it.format == CheatFormat.RAW }.forEach cheatLoop@ { cheat ->
            cheat.codeLines.forEach codeLoop@ { line ->
                val match = rawCodePattern.matchEntire(line) ?: return@codeLoop
                val address = match.groupValues[1].toLong(16)
                val width = match.groupValues[2].toInt()
                val value = match.groupValues[3].toLong(16)
                val key = address to width
                val previous = writes.putIfAbsent(key, value to cheat)
                if (previous != null && previous.first != value) {
                    errors += "${cheat.name} and ${previous.second.name} write different values to 0x${address.toString(16).uppercase()}."
                }
            }
        }
        return CheatProfileValidation(errors.isEmpty(), errors.distinct(), warnings.distinct())
    }
}

data class CheatPackDownloadRequest(
    val url: String,
    val expectedSha256: String
)

object RetraCodesDownloadPolicy {
    private val shaPattern = Regex("[0-9a-fA-F]{64}")

    fun validate(request: CheatPackDownloadRequest): java.net.URI {
        require(shaPattern.matches(request.expectedSha256)) { "Expected SHA-256 must contain 64 hexadecimal characters." }
        val uri = runCatching { java.net.URI(request.url) }.getOrElse { throw IllegalArgumentException("Invalid cheat-pack URL.") }
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) { "Cheat-pack URL must use HTTPS." }
        require(uri.userInfo == null && uri.fragment == null) { "Cheat-pack URL cannot contain credentials or a fragment." }
        val host = uri.host.lowercase()
        require(!isLocalHost(host)) { "Cheat-pack URL cannot target a local or private host." }
        val path = uri.path.orEmpty().lowercase()
        require(path.endsWith(".rcc") || path.endsWith(".txt")) { "Cheat-pack URL must identify an .rcc or .txt file." }
        return uri
    }

    fun validateRedirect(origin: java.net.URI, current: java.net.URI, location: String, visited: Set<java.net.URI>): java.net.URI {
        val target = current.resolve(location)
        val validated = validate(CheatPackDownloadRequest(target.toString(), "0".repeat(64)))
        require(validated.host.equals(origin.host, ignoreCase = true)) { "Cross-host cheat-pack redirects are blocked." }
        require(validated !in visited) { "Cheat-pack redirect loop detected." }
        return validated
    }

    private fun isLocalHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) return true
        if (host == "::1" || host.startsWith("127.")) return true
        val parts = host.split('.').mapNotNull(String::toIntOrNull)
        if (parts.size == 4) {
            val a = parts[0]
            val b = parts[1]
            if (a == 10 || a == 127 || (a == 192 && b == 168) || (a == 172 && b in 16..31) || (a == 169 && b == 254)) return true
        }
        return false
    }
}

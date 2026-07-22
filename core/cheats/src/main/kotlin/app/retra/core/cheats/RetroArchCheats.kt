package app.retra.core.cheats

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class InvalidRetroArchCheatException(message: String) : IllegalArgumentException(message)

object RetroArchCheatParser {
    const val MAX_FILE_BYTES: Int = 512 * 1024
    private const val MAX_CHEATS = 512
    private const val MAX_LINE_LENGTH = 8_192
    private val keyPattern = Regex("[A-Za-z0-9_]+")
    private val indexedKeyPattern = Regex("cheat(\\d+)_(desc|code|enable|handler|big_endian|memory_search_size|repeat_count|repeat_add_to_address|repeat_add_to_value|rumble_type|rumble_value|rumble_port|rumble_primary_strength|rumble_primary_duration|rumble_secondary_strength|rumble_secondary_duration)")
    private val supportedCode = Regex("[0-9A-Fa-f]{4,16}(?:[ :][0-9A-Fa-f]{2,16}){0,3}")

    fun parseForGame(
        bytes: ByteArray,
        provider: String,
        gameSha256: String,
        gameCode: String? = null,
        revision: Int? = null
    ): CheatPack {
        if (bytes.isEmpty()) throw InvalidRetroArchCheatException("RetroArch cheat file is empty.")
        if (bytes.size > MAX_FILE_BYTES) throw InvalidRetroArchCheatException("RetroArch cheat file exceeds 512 KiB.")
        if (!Regex("[0-9A-Fa-f]{64}").matches(gameSha256)) {
            throw InvalidRetroArchCheatException("A valid target ROM SHA-256 is required.")
        }
        val text = decodeUtf8Strict(bytes)
        val fields = linkedMapOf<String, String>()
        text.replace("\r\n", "\n").replace('\r', '\n').lineSequence().forEachIndexed { index, raw ->
            if (raw.length > MAX_LINE_LENGTH) throw InvalidRetroArchCheatException("Line ${index + 1} is too long.")
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith('#')) return@forEachIndexed
            val equals = line.indexOf('=')
            if (equals <= 0) throw InvalidRetroArchCheatException("Expected key = value at line ${index + 1}.")
            val key = line.substring(0, equals).trim()
            if (!keyPattern.matches(key)) throw InvalidRetroArchCheatException("Invalid field name at line ${index + 1}.")
            val value = parseValue(line.substring(equals + 1).trim(), index + 1)
            if (fields.put(key, value) != null) throw InvalidRetroArchCheatException("Duplicate field $key.")
        }

        val count = fields["cheats"]?.toIntOrNull()
            ?: throw InvalidRetroArchCheatException("RetroArch field 'cheats' is missing or invalid.")
        if (count !in 1..MAX_CHEATS) throw InvalidRetroArchCheatException("RetroArch cheat count must be between 1 and $MAX_CHEATS.")

        fields.keys.forEach { key ->
            if (key == "cheats") return@forEach
            val match = indexedKeyPattern.matchEntire(key)
                ?: throw InvalidRetroArchCheatException("Unsupported RetroArch field $key.")
            if (match.groupValues[1].toInt() !in 0 until count) {
                throw InvalidRetroArchCheatException("Field $key references a cheat outside the declared range.")
            }
        }

        val definitions = (0 until count).mapNotNull { index ->
            val description = fields["cheat${index}_desc"]?.trim().orEmpty()
                .ifBlank { "Cheat ${index + 1}" }
                .take(120)
            val payload = fields["cheat${index}_code"]?.trim().orEmpty()
            if (payload.isBlank()) return@mapNotNull null
            val lines = payload
                .replace("\\n", "+")
                .split('+', ';', '\n')
                .map(String::trim)
                .filter(String::isNotEmpty)
            if (lines.isEmpty() || lines.size > 128 || lines.any { !supportedCode.matches(it) }) {
                // Community databases sometimes include placeholders such as ???? for cheats that
                // require a runtime-selected value. Skip only that unsafe definition instead of
                // rejecting every concrete cheat in the file.
                return@mapNotNull null
            }
            CheatDefinition(
                id = uniqueId(description, index),
                name = description,
                description = "Imported from a RetroArch .cht file and bound to this exact ROM hash.",
                category = classify(description),
                format = CheatFormat.GAMESHARK,
                risk = CheatRisk.CAUTION,
                codeLines = lines.map(String::uppercase),
                dependencies = emptySet(),
                conflicts = emptySet()
            )
        }
        if (definitions.isEmpty()) {
            throw InvalidRetroArchCheatException("RetroArch cheat file contains no supported concrete codes.")
        }

        return CheatPack(
            version = 1,
            provider = provider.take(120).ifBlank { "RetroArch cheat import" },
            gameSha256 = gameSha256.lowercase(),
            gameCode = gameCode?.takeIf { Regex("[A-Za-z0-9]{4}").matches(it) }?.uppercase(),
            revision = revision?.takeIf { it in 0..255 },
            region = null,
            cheats = definitions
        )
    }

    fun encodeRetraCodes(pack: CheatPack): ByteArray = buildString {
        appendLine("RETRA-CODES-1")
        appendLine("provider=${sanitize(pack.provider)}")
        appendLine("gameSha256=${pack.gameSha256}")
        pack.gameCode?.let { appendLine("gameCode=$it") }
        pack.revision?.let { appendLine("revision=$it") }
        appendLine()
        pack.cheats.forEach { cheat ->
            appendLine("[cheat]")
            appendLine("id=${cheat.id}")
            appendLine("name=${sanitize(cheat.name)}")
            appendLine("description=${sanitize(cheat.description)}")
            appendLine("category=${cheat.category.name}")
            appendLine("format=${cheat.format.name}")
            appendLine("risk=${cheat.risk.name}")
            cheat.codeLines.forEach { appendLine("code=$it") }
            appendLine("[/cheat]")
            appendLine()
        }
    }.encodeToByteArray()

    private fun parseValue(token: String, line: Int): String {
        if (token.startsWith('"')) {
            if (token.length < 2 || !token.endsWith('"')) {
                throw InvalidRetroArchCheatException("Unterminated quoted value at line $line.")
            }
            val inner = token.substring(1, token.length - 1)
            val output = StringBuilder()
            var index = 0
            while (index < inner.length) {
                val character = inner[index++]
                if (character == '\\') {
                    if (index >= inner.length) throw InvalidRetroArchCheatException("Invalid escape at line $line.")
                    when (val escaped = inner[index++]) {
                        '\\', '"' -> output.append(escaped)
                        'n' -> output.append("\\n")
                        else -> throw InvalidRetroArchCheatException("Unsupported escape \\$escaped at line $line.")
                    }
                } else {
                    output.append(character)
                }
            }
            return output.toString()
        }
        return token
    }

    private fun uniqueId(description: String, index: Int): String {
        val slug = description.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(48)
            .ifBlank { "cheat" }
        return "$slug-${index + 1}".take(64)
    }

    private fun classify(description: String): CheatCategory {
        val value = description.lowercase()
        return when {
            listOf("money", "cash", "coin").any(value::contains) -> CheatCategory.CURRENCY
            listOf("hp", "health", "life").any(value::contains) -> CheatCategory.HEALTH
            listOf("exp", "experience", "level").any(value::contains) -> CheatCategory.EXPERIENCE
            listOf("encounter", "wild", "pokemon").any(value::contains) -> CheatCategory.ENCOUNTERS
            listOf("item", "inventory", "berry", "ball").any(value::contains) -> CheatCategory.INVENTORY
            listOf("unlock", "badge", "dex").any(value::contains) -> CheatCategory.UNLOCKABLES
            listOf("walk", "movement", "speed").any(value::contains) -> CheatCategory.MOVEMENT
            else -> CheatCategory.QUALITY_OF_LIFE
        }
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("[\\r\\n=\\[\\]]"), " ")
        .trim()
        .take(500)

    private fun decodeUtf8Strict(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: Exception) {
        throw InvalidRetroArchCheatException("RetroArch cheat file is not valid UTF-8 text.")
    }
}

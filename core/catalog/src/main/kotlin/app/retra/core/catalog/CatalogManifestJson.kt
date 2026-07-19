package app.retra.core.catalog

import app.retra.core.download.CatalogDownloadPolicy
import app.retra.core.model.CatalogContentKind
import app.retra.core.model.CatalogEntry
import app.retra.core.model.CatalogManifest
import app.retra.core.model.CompatibilityStatus
import app.retra.core.rom.CatalogValidationResult
import app.retra.core.rom.CatalogValidator
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class InvalidCatalogManifestException(message: String) : IllegalArgumentException(message)

object CatalogManifestJson {
    const val MAX_MANIFEST_BYTES: Int = 2 * 1024 * 1024
    const val MAX_GAMES: Int = 512
    private const val MAX_DEPTH = 32
    private const val MAX_CONTAINER_ITEMS = 4096
    private const val MAX_STRING_CHARS = 65_536

    private val manifestFields = setOf(
        "catalogVersion", "catalogId", "name", "description", "owner", "sourceUrl", "contentPolicy", "games"
    )
    private val gameFields = setOf(
        "id", "title", "description", "creator", "version", "downloadUrl", "sha256", "fileSize",
        "license", "distributionPermission", "artworkUrl", "tags", "compatibility",
        "contentKind", "sourcePageUrl"
    )

    fun parse(bytes: ByteArray): CatalogManifest {
        if (bytes.isEmpty()) throw InvalidCatalogManifestException("Catalog manifest is empty.")
        if (bytes.size > MAX_MANIFEST_BYTES) {
            throw InvalidCatalogManifestException("Catalog manifest exceeds Retra's 2 MiB limit.")
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = runCatching { decoder.decode(ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { throw InvalidCatalogManifestException("Catalog manifest is not valid UTF-8.") }
        val root = JsonParser(text, MAX_DEPTH, MAX_CONTAINER_ITEMS, MAX_STRING_CHARS).parse()
            .asObject("Catalog root")
        rejectUnknown(root, manifestFields, "Catalog root")
        val gamesNode = root.requiredArray("games")
        if (gamesNode.size > MAX_GAMES) {
            throw InvalidCatalogManifestException("Catalog contains more than $MAX_GAMES games.")
        }
        val manifest = CatalogManifest(
            catalogVersion = root.requiredLong("catalogVersion").toIntExact("catalogVersion"),
            catalogId = root.requiredString("catalogId").bounded("catalogId", 80),
            name = root.requiredString("name").bounded("name", 200),
            description = root.requiredString("description").bounded("description", 4_000),
            owner = root.requiredString("owner").bounded("owner", 200),
            sourceUrl = root.requiredString("sourceUrl").bounded("sourceUrl", 2_048),
            contentPolicy = root.requiredString("contentPolicy").bounded("contentPolicy", 80),
            games = gamesNode.mapIndexed(::parseEntry)
        )
        when (val validation = CatalogValidator.validate(manifest)) {
            CatalogValidationResult.Valid -> Unit
            is CatalogValidationResult.Invalid -> throw InvalidCatalogManifestException(
                "Catalog schema validation failed: ${validation.reasons.joinToString(" ")}"
            )
        }
        manifest.games.forEach { entry ->
            if (entry.contentKind == CatalogContentKind.EXTERNAL) return@forEach
            runCatching { CatalogDownloadPolicy.validateEntry(entry) }.getOrElse { error ->
                throw InvalidCatalogManifestException("${entry.id}: ${error.message ?: "unsafe download metadata"}")
            }
        }
        return manifest
    }

    private fun parseEntry(index: Int, value: JsonValue): CatalogEntry {
        val objectValue = value.asObject("games[$index]")
        rejectUnknown(objectValue, gameFields, "games[$index]")
        val tags = objectValue.requiredArray("tags")
        if (tags.size > 32) throw InvalidCatalogManifestException("games[$index].tags exceeds 32 values.")
        val compatibilityName = objectValue.requiredString("compatibility").uppercase()
        val compatibility = runCatching { CompatibilityStatus.valueOf(compatibilityName) }
            .getOrElse { throw InvalidCatalogManifestException("games[$index].compatibility is unsupported.") }
        val contentKindName = objectValue.optionalString("contentKind")?.uppercase() ?: CatalogContentKind.GAME.name
        val contentKind = runCatching { CatalogContentKind.valueOf(contentKindName) }
            .getOrElse { throw InvalidCatalogManifestException("games[$index].contentKind is unsupported.") }
        val sourcePageUrl = objectValue.optionalString("sourcePageUrl")?.bounded("games[$index].sourcePageUrl", 2_048)
        if (contentKind == CatalogContentKind.EXTERNAL && sourcePageUrl.isNullOrBlank()) {
            throw InvalidCatalogManifestException("games[$index].sourcePageUrl is required for EXTERNAL entries.")
        }
        return CatalogEntry(
            id = objectValue.requiredString("id").bounded("games[$index].id", 80),
            title = objectValue.requiredString("title").bounded("games[$index].title", 200),
            description = objectValue.requiredString("description").bounded("games[$index].description", 4_000),
            creator = objectValue.requiredString("creator").bounded("games[$index].creator", 200),
            version = objectValue.requiredString("version").bounded("games[$index].version", 80),
            downloadUrl = objectValue.requiredString("downloadUrl").bounded("games[$index].downloadUrl", 2_048),
            sha256 = objectValue.requiredString("sha256").bounded("games[$index].sha256", 64),
            fileSize = objectValue.requiredLong("fileSize"),
            license = objectValue.requiredString("license").bounded("games[$index].license", 200),
            distributionPermission = objectValue.requiredString("distributionPermission")
                .bounded("games[$index].distributionPermission", 4_000),
            artworkUrl = objectValue.optionalString("artworkUrl")?.bounded("games[$index].artworkUrl", 2_048),
            tags = tags.mapIndexed { tagIndex, tag ->
                tag.asString("games[$index].tags[$tagIndex]").bounded("games[$index].tags[$tagIndex]", 64)
            },
            compatibility = compatibility,
            contentKind = contentKind,
            sourcePageUrl = sourcePageUrl
        )
    }

    private fun rejectUnknown(value: Map<String, JsonValue>, allowed: Set<String>, location: String) {
        val unknown = value.keys - allowed
        if (unknown.isNotEmpty()) {
            throw InvalidCatalogManifestException("$location contains unsupported fields: ${unknown.sorted().joinToString()}.")
        }
    }

    private fun Map<String, JsonValue>.requiredString(name: String): String =
        this[name]?.asString(name) ?: throw InvalidCatalogManifestException("Missing required field: $name.")

    private fun Map<String, JsonValue>.optionalString(name: String): String? = when (val value = this[name]) {
        null, JsonValue.Null -> null
        else -> value.asString(name)
    }

    private fun Map<String, JsonValue>.requiredLong(name: String): Long =
        this[name]?.asLong(name) ?: throw InvalidCatalogManifestException("Missing required field: $name.")

    private fun Map<String, JsonValue>.requiredArray(name: String): List<JsonValue> =
        this[name]?.asArray(name) ?: throw InvalidCatalogManifestException("Missing required field: $name.")

    private fun String.bounded(field: String, maximum: Int): String {
        if (length > maximum) throw InvalidCatalogManifestException("$field exceeds $maximum characters.")
        return this
    }

    private fun Long.toIntExact(field: String): Int {
        if (this !in Int.MIN_VALUE..Int.MAX_VALUE) {
            throw InvalidCatalogManifestException("$field is outside the supported integer range.")
        }
        return toInt()
    }
}

private sealed interface JsonValue {
    data class ObjectValue(val values: LinkedHashMap<String, JsonValue>) : JsonValue
    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val token: String) : JsonValue
    data class BooleanValue(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

private fun JsonValue.asObject(location: String): Map<String, JsonValue> =
    (this as? JsonValue.ObjectValue)?.values
        ?: throw InvalidCatalogManifestException("$location must be a JSON object.")

private fun JsonValue.asArray(location: String): List<JsonValue> =
    (this as? JsonValue.ArrayValue)?.values
        ?: throw InvalidCatalogManifestException("$location must be a JSON array.")

private fun JsonValue.asString(location: String): String =
    (this as? JsonValue.StringValue)?.value
        ?: throw InvalidCatalogManifestException("$location must be a JSON string.")

private fun JsonValue.asLong(location: String): Long {
    val token = (this as? JsonValue.NumberValue)?.token
        ?: throw InvalidCatalogManifestException("$location must be an integer.")
    if (token.contains('.') || token.contains('e', ignoreCase = true)) {
        throw InvalidCatalogManifestException("$location must be an integer.")
    }
    return token.toLongOrNull() ?: throw InvalidCatalogManifestException("$location is outside the supported integer range.")
}

private class JsonParser(
    private val text: String,
    private val maxDepth: Int,
    private val maxContainerItems: Int,
    private val maxStringChars: Int
) {
    private var index = 0

    fun parse(): JsonValue {
        skipWhitespace()
        val result = parseValue(0)
        skipWhitespace()
        if (index != text.length) fail("Unexpected trailing content")
        return result
    }

    private fun parseValue(depth: Int): JsonValue {
        if (depth > maxDepth) fail("JSON nesting exceeds $maxDepth levels")
        skipWhitespace()
        if (index >= text.length) fail("Unexpected end of input")
        return when (text[index]) {
            '{' -> parseObject(depth + 1)
            '[' -> parseArray(depth + 1)
            '"' -> JsonValue.StringValue(parseString())
            't' -> { expectLiteral("true"); JsonValue.BooleanValue(true) }
            'f' -> { expectLiteral("false"); JsonValue.BooleanValue(false) }
            'n' -> { expectLiteral("null"); JsonValue.Null }
            '-', in '0'..'9' -> JsonValue.NumberValue(parseNumber())
            else -> fail("Unexpected character '${text[index]}'")
        }
    }

    private fun parseObject(depth: Int): JsonValue.ObjectValue {
        expect('{')
        skipWhitespace()
        val values = LinkedHashMap<String, JsonValue>()
        if (consume('}')) return JsonValue.ObjectValue(values)
        while (true) {
            if (values.size >= maxContainerItems) fail("JSON object exceeds $maxContainerItems members")
            skipWhitespace()
            if (peek() != '"') fail("Object keys must be strings")
            val key = parseString()
            if (values.containsKey(key)) fail("Duplicate object key '$key'")
            skipWhitespace()
            expect(':')
            values[key] = parseValue(depth)
            skipWhitespace()
            if (consume('}')) break
            expect(',')
        }
        return JsonValue.ObjectValue(values)
    }

    private fun parseArray(depth: Int): JsonValue.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = ArrayList<JsonValue>()
        if (consume(']')) return JsonValue.ArrayValue(values)
        while (true) {
            if (values.size >= maxContainerItems) fail("JSON array exceeds $maxContainerItems values")
            values += parseValue(depth)
            skipWhitespace()
            if (consume(']')) break
            expect(',')
        }
        return JsonValue.ArrayValue(values)
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < text.length) {
            val character = text[index++]
            when {
                character == '"' -> {
                    if (result.length > maxStringChars) fail("JSON string exceeds $maxStringChars characters")
                    return result.toString()
                }
                character == '\\' -> {
                    if (index >= text.length) fail("Unterminated escape sequence")
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> result.append(escaped)
                        'b' -> result.append('\b')
                        'f' -> result.append('\u000C')
                        'n' -> result.append('\n')
                        'r' -> result.append('\r')
                        't' -> result.append('\t')
                        'u' -> result.append(parseUnicodeEscape())
                        else -> fail("Unsupported escape sequence \\$escaped")
                    }
                }
                character.code < 0x20 -> fail("Unescaped control character in JSON string")
                else -> result.append(character)
            }
            if (result.length > maxStringChars) fail("JSON string exceeds $maxStringChars characters")
        }
        fail("Unterminated JSON string")
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > text.length) fail("Incomplete Unicode escape")
        val token = text.substring(index, index + 4)
        val value = token.toIntOrNull(16) ?: fail("Invalid Unicode escape")
        index += 4
        return value.toChar()
    }

    private fun parseNumber(): String {
        val start = index
        consume('-')
        when {
            consume('0') -> Unit
            peek() in '1'..'9' -> while (peek() in '0'..'9') index++
            else -> fail("Invalid JSON number")
        }
        if (consume('.')) {
            if (peek() !in '0'..'9') fail("Invalid JSON fraction")
            while (peek() in '0'..'9') index++
        }
        if (peek() == 'e' || peek() == 'E') {
            index++
            if (peek() == '+' || peek() == '-') index++
            if (peek() !in '0'..'9') fail("Invalid JSON exponent")
            while (peek() in '0'..'9') index++
        }
        return text.substring(start, index)
    }

    private fun expectLiteral(literal: String) {
        if (!text.regionMatches(index, literal, 0, literal.length)) fail("Expected '$literal'")
        index += literal.length
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (!consume(expected)) fail("Expected '$expected'")
    }

    private fun consume(expected: Char): Boolean {
        if (index < text.length && text[index] == expected) {
            index++
            return true
        }
        return false
    }

    private fun peek(): Char = if (index < text.length) text[index] else '\u0000'

    private fun skipWhitespace() {
        while (index < text.length && text[index] in listOf(' ', '\t', '\r', '\n')) index++
    }

    private fun fail(message: String): Nothing =
        throw InvalidCatalogManifestException("$message at character $index.")
}

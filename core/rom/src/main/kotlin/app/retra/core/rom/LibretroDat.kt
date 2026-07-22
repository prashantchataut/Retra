package app.retra.core.rom

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

class InvalidLibretroDatException(message: String) : IllegalArgumentException(message)

data class LibretroRomMetadata(
    val canonicalTitle: String,
    val romName: String,
    val sizeBytes: Long,
    val crc32: Long?,
    val md5: String?,
    val sha1: String?,
    val status: String?
)

data class LibretroDatIndex(
    val records: List<LibretroRomMetadata>
) {
    private val bySha1 = records.mapNotNull { record ->
        record.sha1?.lowercase()?.let { it to record }
    }.groupBy({ it.first }, { it.second })

    private val byCrc = records.mapNotNull { record ->
        record.crc32?.let { it to record }
    }.groupBy({ it.first }, { it.second })

    fun match(sha1: String?, crc32: Long?, sizeBytes: Long): LibretroRomMetadata? {
        val normalizedSha1 = sha1?.trim()?.lowercase()?.takeIf { HEX_40.matches(it) }
        val shaMatches = normalizedSha1?.let(bySha1::get).orEmpty()
            .filter { it.sizeBytes == sizeBytes }
        if (shaMatches.size == 1) return shaMatches.single()

        val crcMatches = crc32?.let(byCrc::get).orEmpty()
            .filter { it.sizeBytes == sizeBytes }
        return crcMatches.singleOrNull()
    }

    companion object {
        private val HEX_40 = Regex("[0-9a-f]{40}")
    }
}

object LibretroDatParser {
    const val MAX_DAT_BYTES: Int = 32 * 1024 * 1024
    const val MAX_RECORDS: Int = 250_000
    private const val MAX_GAME_BLOCK_CHARS = 256 * 1024

    fun parse(bytes: ByteArray): LibretroDatIndex {
        if (bytes.isEmpty()) throw InvalidLibretroDatException("Metadata DAT is empty.")
        if (bytes.size > MAX_DAT_BYTES) {
            throw InvalidLibretroDatException("Metadata DAT exceeds Retra's 32 MiB safety limit.")
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = runCatching { decoder.decode(ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { throw InvalidLibretroDatException("Metadata DAT is not valid UTF-8.") }
        return parse(text)
    }

    fun parse(text: String): LibretroDatIndex {
        val records = ArrayList<LibretroRomMetadata>()
        var cursor = 0
        while (cursor < text.length) {
            val gameToken = findToken(text, "game", cursor) ?: break
            val open = skipWhitespace(text, gameToken + 4)
            if (open >= text.length || text[open] != '(') {
                cursor = gameToken + 4
                continue
            }
            val close = findMatchingParen(text, open)
            if (close < 0) throw InvalidLibretroDatException("Unterminated game block near character $gameToken.")
            if (close - open > MAX_GAME_BLOCK_CHARS) {
                throw InvalidLibretroDatException("A metadata game block exceeds Retra's safety limit.")
            }
            val block = text.substring(open + 1, close)
            val title = readTopLevelStringField(block, "name")
                ?: readTopLevelStringField(block, "description")
                ?: "Unknown title"
            parseRomBlocks(block, title, records)
            if (records.size > MAX_RECORDS) {
                throw InvalidLibretroDatException("Metadata DAT contains more than $MAX_RECORDS ROM records.")
            }
            cursor = close + 1
        }
        if (records.isEmpty()) throw InvalidLibretroDatException("Metadata DAT contains no ROM records.")
        return LibretroDatIndex(records)
    }

    private fun parseRomBlocks(
        block: String,
        title: String,
        records: MutableList<LibretroRomMetadata>
    ) {
        var cursor = 0
        while (cursor < block.length) {
            val token = findToken(block, "rom", cursor) ?: break
            if (nestingDepthAt(block, token) != 0) {
                cursor = token + 3
                continue
            }
            val open = skipWhitespace(block, token + 3)
            if (open >= block.length || block[open] != '(') {
                cursor = token + 3
                continue
            }
            val close = findMatchingParen(block, open)
            if (close < 0) throw InvalidLibretroDatException("Unterminated rom block for $title.")
            val fields = parseFlatFields(block.substring(open + 1, close))
            val name = fields["name"] ?: title
            val size = fields["size"]?.toLongOrNull()
                ?: throw InvalidLibretroDatException("ROM record for $title has no valid size.")
            if (size < 0 || size > GbaRomParser.MAX_ROM_SIZE_BYTES.toLong()) {
                throw InvalidLibretroDatException("ROM record for $title has an unsupported size.")
            }
            val crc = fields["crc"]?.let { parseHexLong(it, 8, "CRC-32", title) }
            val md5 = fields["md5"]?.normalizeHash(32, "MD5", title)
            val sha1 = fields["sha1"]?.normalizeHash(40, "SHA-1", title)
            records += LibretroRomMetadata(
                canonicalTitle = title.take(300),
                romName = name.take(300),
                sizeBytes = size,
                crc32 = crc,
                md5 = md5,
                sha1 = sha1,
                status = fields["status"]?.take(40)
            )
            cursor = close + 1
        }
    }

    private fun parseFlatFields(content: String): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        var index = 0
        while (index < content.length) {
            index = skipWhitespace(content, index)
            if (index >= content.length) break
            val keyStart = index
            while (index < content.length && (content[index].isLetterOrDigit() || content[index] == '_')) index++
            if (keyStart == index) {
                index++
                continue
            }
            val key = content.substring(keyStart, index).lowercase()
            index = skipWhitespace(content, index)
            if (index >= content.length) break
            val value: String
            if (content[index] == '"') {
                val parsed = parseQuoted(content, index)
                value = parsed.first
                index = parsed.second
            } else {
                val valueStart = index
                while (index < content.length && !content[index].isWhitespace() && content[index] != ')') index++
                value = content.substring(valueStart, index)
            }
            if (key in SUPPORTED_FIELDS && key !in fields) fields[key] = value
        }
        return fields
    }

    private fun readTopLevelStringField(content: String, field: String): String? {
        var cursor = 0
        while (cursor < content.length) {
            val token = findToken(content, field, cursor) ?: return null
            if (nestingDepthAt(content, token) != 0) {
                cursor = token + field.length
                continue
            }
            var index = skipWhitespace(content, token + field.length)
            if (index < content.length && content[index] == '"') {
                return parseQuoted(content, index).first.take(300)
            }
            cursor = token + field.length
        }
        return null
    }

    private fun parseQuoted(text: String, quoteIndex: Int): Pair<String, Int> {
        var index = quoteIndex + 1
        val result = StringBuilder()
        while (index < text.length) {
            when (val character = text[index++]) {
                '"' -> return result.toString() to index
                '\\' -> {
                    if (index >= text.length) throw InvalidLibretroDatException("Unterminated quoted escape.")
                    result.append(text[index++])
                }
                else -> result.append(character)
            }
            if (result.length > 4_096) throw InvalidLibretroDatException("Metadata field exceeds 4096 characters.")
        }
        throw InvalidLibretroDatException("Unterminated quoted metadata value.")
    }

    private fun findToken(text: String, token: String, start: Int): Int? {
        var index = start.coerceAtLeast(0)
        var inQuote = false
        var escaped = false
        while (index <= text.length - token.length) {
            val character = text[index]
            if (inQuote) {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == '"') inQuote = false
                index++
                continue
            }
            if (character == '"') {
                inQuote = true
                index++
                continue
            }
            if (text.regionMatches(index, token, 0, token.length, ignoreCase = true)) {
                val before = text.getOrNull(index - 1)
                val after = text.getOrNull(index + token.length)
                if ((before == null || !before.isLetterOrDigit() && before != '_') &&
                    (after == null || !after.isLetterOrDigit() && after != '_')) {
                    return index
                }
            }
            index++
        }
        return null
    }

    private fun findMatchingParen(text: String, openIndex: Int): Int {
        var depth = 0
        var inQuote = false
        var escaped = false
        for (index in openIndex until text.length) {
            val character = text[index]
            if (inQuote) {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == '"') inQuote = false
                continue
            }
            when (character) {
                '"' -> inQuote = true
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                    if (depth < 0) return -1
                }
            }
        }
        return -1
    }

    private fun nestingDepthAt(text: String, endExclusive: Int): Int {
        var depth = 0
        var inQuote = false
        var escaped = false
        for (index in 0 until endExclusive.coerceAtMost(text.length)) {
            val character = text[index]
            if (inQuote) {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == '"') inQuote = false
            } else {
                when (character) {
                    '"' -> inQuote = true
                    '(' -> depth++
                    ')' -> depth--
                }
            }
        }
        return depth
    }

    private fun skipWhitespace(text: String, start: Int): Int {
        var index = start
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }

    private fun parseHexLong(token: String, length: Int, label: String, title: String): Long {
        val normalized = token.trim().lowercase()
        if (normalized.length != length || normalized.any { it !in "0123456789abcdef" }) {
            throw InvalidLibretroDatException("$label for $title is malformed.")
        }
        return normalized.toLong(16)
    }

    private fun String.normalizeHash(length: Int, label: String, title: String): String {
        val normalized = trim().lowercase()
        if (normalized.length != length || normalized.any { it !in "0123456789abcdef" }) {
            throw InvalidLibretroDatException("$label for $title is malformed.")
        }
        return normalized
    }

    private val SUPPORTED_FIELDS = setOf("name", "size", "crc", "md5", "sha1", "status")
}

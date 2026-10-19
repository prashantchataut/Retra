package app.retra.core

/**
 * Validates and normalises cheat codes before they reach the engine.
 * Accepted per line: raw "AAAAAAAA:VV|VVVV|VVVVVVVV", CodeBreaker "XXXXXXXX YYYY",
 * GameShark / Action Replay "XXXXXXXX YYYYYYYY". An entry may start with "GS:" or "AR:".
 */
object CheatParser {
    enum class Format { RAW, CODEBREAKER, GAMESHARK_AR, MIXED }

    sealed interface Result {
        data class Ok(val normalized: String, val lines: Int, val format: Format) : Result
        data class Error(val message: String) : Result
    }

    private val RAW = Regex("^[0-9A-F]{8}:([0-9A-F]{2}|[0-9A-F]{4}|[0-9A-F]{8})$")
    private val CB = Regex("^[0-9A-F]{8} [0-9A-F]{4}$")
    private val GS = Regex("^[0-9A-F]{8} [0-9A-F]{8}$")

    fun parse(input: String): Result {
        var text = input.trim()
        var prefix = ""
        val upper = text.uppercase()
        if (upper.startsWith("GS:") || upper.startsWith("AR:")) {
            prefix = upper.substring(0, 3)
            text = text.substring(3)
        }
        val rawLines = text.split('\n', '\r', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.isEmpty()) return Result.Error("Enter a cheat code.")
        val out = ArrayList<String>()
        val formats = HashSet<Format>()
        rawLines.forEachIndexed { index, line ->
            val compact = line.uppercase().replace(Regex("[\\s-]+"), " ").trim()
            val normalized = when {
                RAW.matches(compact.replace(" ", "")) -> compact.replace(" ", "").also { formats += Format.RAW }
                CB.matches(compact) -> compact.also { formats += Format.CODEBREAKER }
                GS.matches(compact) -> compact.also { formats += Format.GAMESHARK_AR }
                compact.length == 16 && compact.all { it.isHexDigit() } ->
                    "${compact.substring(0, 8)} ${compact.substring(8)}".also { formats += Format.GAMESHARK_AR }
                compact.length == 12 && compact.all { it.isHexDigit() } ->
                    "${compact.substring(0, 8)} ${compact.substring(8)}".also { formats += Format.CODEBREAKER }
                else -> return Result.Error(
                    if (rawLines.size == 1) "That doesn't look like a cheat code. Use AAAAAAAA:VV, XXXXXXXX YYYY or XXXXXXXX YYYYYYYY."
                    else "Line ${index + 1} isn't a valid code: \"$line\"",
                )
            }
            out += normalized
        }
        if (prefix.isNotEmpty() && Format.RAW in formats) return Result.Error("GS:/AR: prefixes only apply to GameShark or Action Replay codes.")
        val format = if (formats.size == 1) formats.first() else Format.MIXED
        return Result.Ok(prefix + out.joinToString("\n"), out.size, format)
    }

    private fun Char.isHexDigit() = this in '0'..'9' || this in 'A'..'F'
}

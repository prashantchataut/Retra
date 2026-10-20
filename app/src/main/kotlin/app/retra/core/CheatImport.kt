package app.retra.core

/**
 * Turns pasted text or a cheat file into named cheats: RetroArch `.cht` files (`cheatN_desc` /
 * `cheatN_code`, codes joined with `+`) or plain blocks where a name line is followed by its codes and
 * blocks are separated by blank lines. Every result is validated with [CheatParser].
 */
object CheatImport {
    data class Parsed(val name: String, val code: String)
    data class Result(val cheats: List<Parsed>, val skipped: Int)

    private val CHT_LINE = Regex("""^\s*cheat(\d+)_(desc|code)\s*=\s*"?(.*?)"?\s*$""")
    private val CODE_LINE = Regex("""^(?:(?:GS|AR):)?[0-9A-Fa-f:+\s-]+$""")

    fun parse(text: String, fallbackName: (Int) -> String = { "Cheat ${it + 1}" }): Result =
        if (text.lineSequence().any { CHT_LINE.matches(it) && it.contains("_code") }) parseCht(text, fallbackName) else parseBlocks(text, fallbackName)

    /** Splits `+`/space separated pieces into one code per line ("82025838+0000" -> "82025838 0000"). */
    fun regroup(raw: String): String {
        var text = raw.trim()
        var prefix = ""
        if (text.length > 3 && (text.startsWith("GS:", true) || text.startsWith("AR:", true))) {
            prefix = text.substring(0, 3).uppercase()
            text = text.substring(3)
        }
        val tokens = text.uppercase().split(Regex("[+\\s,;]+")).filter { it.isNotEmpty() }
        val out = ArrayList<String>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val next = tokens.getOrNull(i + 1)
            when {
                ':' in t -> out += t
                t.length == 16 || t.length == 12 -> out += t.substring(0, 8) + " " + t.substring(8)
                t.length == 8 && next != null && ':' !in next && (next.length == 8 || next.length == 4) -> {
                    out += "$t $next"
                    i++
                }
                else -> out += t
            }
            i++
        }
        return prefix + out.joinToString("\n")
    }

    private fun validated(name: String, raw: String): Parsed? =
        (CheatParser.parse(regroup(raw)) as? CheatParser.Result.Ok)?.let { Parsed(name, it.normalized) }

    private fun parseCht(text: String, fallbackName: (Int) -> String): Result {
        val desc = HashMap<Int, String>()
        val code = HashMap<Int, String>()
        text.lineSequence().forEach { line ->
            val m = CHT_LINE.matchEntire(line) ?: return@forEach
            val i = m.groupValues[1].toIntOrNull() ?: return@forEach
            if (m.groupValues[2] == "desc") desc[i] = m.groupValues[3] else code[i] = m.groupValues[3]
        }
        var skipped = 0
        val out = code.keys.sorted().mapNotNull { i ->
            val name = desc[i]?.trim()?.replace(Regex("\\s+"), " ")?.take(60)?.ifBlank { null } ?: fallbackName(i)
            validated(name, code[i].orEmpty()).also { if (it == null) skipped++ }
        }
        return Result(out, skipped)
    }

    private fun parseBlocks(text: String, fallbackName: (Int) -> String): Result {
        val out = ArrayList<Parsed>()
        var skipped = 0
        var name: String? = null
        val codes = ArrayList<String>()
        fun flush() {
            if (codes.isNotEmpty()) {
                val parsed = validated(name ?: fallbackName(out.size), codes.joinToString("\n"))
                if (parsed != null) out += parsed else skipped++
            }
            name = null
            codes.clear()
        }
        text.lineSequence().map { it.trim() }.forEach { line ->
            when {
                line.isEmpty() -> flush()
                CODE_LINE.matches(line) && line.count { it.isLetterOrDigit() } >= 8 -> codes += line
                else -> {
                    if (codes.isNotEmpty()) flush()
                    name = line.trimStart('#', '-', '*', ' ').trimEnd(':', ' ').take(60).ifBlank { null }
                }
            }
        }
        flush()
        return Result(out, skipped)
    }
}

package app.retra.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The model-facing half of "search cheats online": builds the prompt and turns the reply into codes Retra's
 * engine accepts. Anything the model returns is validated with [CheatParser]; nothing is trusted.
 */
object OpenRouterCheats {
    @Serializable
    data class Suggestion(val name: String = "", val code: String = "", val note: String = "")

    @Serializable
    data class Reply(val cheats: List<Suggestion> = emptyList())

    data class Parsed(val cheats: List<Suggested>, val dropped: Int)

    data class Suggested(val name: String, val code: String, val note: String)

    data class Message(val role: String, val content: String)

    private val lenient = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    const val DEFAULT_MODEL = "openai/gpt-4o-mini:online"

    /** Models offered in Settings. The ":online" suffix lets OpenRouter attach live web results to the answer. */
    val models = listOf(
        "openai/gpt-4o-mini:online" to "GPT-4o mini + web",
        "google/gemini-2.5-flash:online" to "Gemini 2.5 Flash + web",
        "perplexity/sonar" to "Perplexity Sonar (built-in search)",
        "openai/gpt-4o-mini" to "GPT-4o mini (no web)",
    )

    fun messages(title: String, serial: String, request: String?): List<Message> {
        val system = """
            You are an expert on Game Boy Advance cheat codes (GameShark Advance, Action Replay, CodeBreaker).
            Find real, working codes for the game the user names. Prefer codes that are published for the exact game and region.
            Rules:
            - Reply with JSON only, no prose and no code fences: {"cheats":[{"name":"...","code":"...","note":"..."}]}
            - "code" is one or more lines of hex written as "AAAAAAAA BBBBBBBB" (Action Replay v1/v2 / GameShark Advance) or "AAAAAAAA:VV" (raw address:value). Separate lines with "\n".
            - Include any master / enable code the other codes need as the first entry, named "Master code".
            - Give at most 12 codes. If you are not sure a code exists, leave it out. Never invent codes.
            - "note" is one short sentence: the region/version the code is for and anything to watch out for.
        """.trimIndent()
        val user = buildString {
            append("Game: ").append(title)
            if (serial.isNotBlank()) append(" (game code ").append(serial.uppercase()).append(")")
            append("\nConsole: Game Boy Advance")
            if (!request.isNullOrBlank()) append("\nI'm looking for: ").append(request.trim())
        }
        return listOf(Message("system", system), Message("user", user))
    }

    /** Pulls the JSON out of a reply (models sometimes wrap it) and keeps only codes that validate. */
    fun parse(content: String): Parsed {
        val json = extractJson(content) ?: return Parsed(emptyList(), 0)
        val reply = runCatching { lenient.decodeFromString(Reply.serializer(), json) }.getOrNull() ?: return Parsed(emptyList(), 0)
        var dropped = 0
        val seen = HashSet<String>()
        val out = ArrayList<Suggested>()
        for (s in reply.cheats) {
            val name = s.name.trim().ifBlank { "Cheat" }.take(60)
            val code = s.code.replace("\\n", "\n").trim()
            val parsed = CheatImport.parse("$name\n$code").cheats.firstOrNull()
            if (parsed == null || !seen.add(parsed.code)) { dropped++; continue }
            out += Suggested(name, parsed.code, s.note.trim().take(160))
        }
        return Parsed(out, dropped)
    }

    private fun extractJson(text: String): String? {
        val t = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        return if (start in 0 until end) t.substring(start, end + 1) else null
    }

    /** Which models answer: OpenRouter's own error text, shortened for a toast. */
    fun friendlyError(code: Int, body: String): String = when (code) {
        401, 403 -> "OpenRouter rejected the API key. Check it in Settings."
        402 -> "Your OpenRouter account is out of credits."
        429 -> "OpenRouter says slow down. Try again in a minute."
        in 500..599 -> "OpenRouter is having a moment. Try again shortly."
        else -> runCatching {
            lenient.parseToJsonElement(body).let { (it as? kotlinx.serialization.json.JsonObject)?.get("error") }
                ?.let { e -> (e as? kotlinx.serialization.json.JsonObject)?.get("message")?.toString()?.trim('"') }
        }.getOrNull()?.take(140) ?: "OpenRouter returned an error ($code)."
    }
}

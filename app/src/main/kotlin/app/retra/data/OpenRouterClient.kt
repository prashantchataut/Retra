package app.retra.data

import app.retra.core.OpenRouterCheats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/**
 * Searches for cheat codes through the player's own OpenRouter account. The API key is stored encrypted on the
 * device ([SecretStore]) and only ever sent to openrouter.ai. Results come back as suggestions that are validated
 * and added switched off.
 */
class OpenRouterClient(private val secrets: SecretStore, private val settings: SettingsRepository) {
    sealed interface Outcome {
        data class Found(val parsed: OpenRouterCheats.Parsed, val model: String) : Outcome
        data class Failed(val message: String) : Outcome
    }

    private val json = Json { ignoreUnknownKeys = true }

    val hasKey: Boolean get() = secrets.has(SecretStore.OPENROUTER)

    fun saveKey(key: String): Boolean = key.trim().let { it.isNotEmpty() && secrets.put(SecretStore.OPENROUTER, it) }

    fun removeKey() = secrets.remove(SecretStore.OPENROUTER)

    /** Checks the key against OpenRouter and returns a short description of it, or null when it's rejected. */
    suspend fun verifyKey(key: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL("$BASE/key").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000; readTimeout = 15_000
                setRequestProperty("Authorization", "Bearer ${key.trim()}")
            }
            if (c.responseCode !in 200..299) return@runCatching null
            val data = json.parseToJsonElement(c.inputStream.bufferedReader().readText()).jsonObject["data"]?.jsonObject
            val label = data?.get("label")?.jsonPrimitive?.contentOrNull
            val limit = data?.get("limit_remaining")?.jsonPrimitive?.contentOrNull
            buildString {
                append("Key works")
                if (!label.isNullOrBlank()) append(" (").append(label.takeLast(12)).append(")")
                if (limit != null && limit != "null") append(" · $").append(limit.toDoubleOrNull()?.let { "%.2f".format(it) } ?: limit).append(" left")
            }
        }.getOrNull()
    }

    suspend fun search(game: Game, request: String?): Outcome = withContext(Dispatchers.IO) {
        val key = secrets.get(SecretStore.OPENROUTER) ?: return@withContext Outcome.Failed("Add your OpenRouter API key in Settings first.")
        val model = settings.current.openRouterModel.ifBlank { OpenRouterCheats.DEFAULT_MODEL }
        val msgs = OpenRouterCheats.messages(game.title, game.gameCode, request)
        val body = buildJsonObject {
            put("model", model)
            put("temperature", 0.2)
            put("max_tokens", 1400)
            put("messages", buildJsonArray { msgs.forEach { m -> add(buildJsonObject { put("role", m.role); put("content", m.content) }) } })
        }
        try {
            val c = (URL("$BASE/chat/completions").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 75_000 // web-search models are slow
                doOutput = true
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("HTTP-Referer", "https://github.com/prashantchataut/retra")
                setRequestProperty("X-Title", "Retra")
            }
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            if (code !in 200..299) {
                val err = runCatching { c.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
                return@withContext Outcome.Failed(OpenRouterCheats.friendlyError(code, err))
            }
            val reply: JsonObject = json.parseToJsonElement(c.inputStream.bufferedReader().readText()).jsonObject
            val content = ((reply["choices"] as? JsonArray)?.firstOrNull() as? JsonObject)
                ?.get("message")?.jsonObject?.get("content")?.let { (it as? JsonPrimitive)?.contentOrNull }
                ?: return@withContext Outcome.Failed("OpenRouter sent an empty answer.")
            val parsed = OpenRouterCheats.parse(content)
            if (parsed.cheats.isEmpty()) Outcome.Failed(if (parsed.dropped > 0) "The model only returned codes Retra can't use." else "No codes found for this game. Try describing what you want.")
            else Outcome.Found(parsed, model)
        } catch (e: java.net.SocketTimeoutException) {
            Outcome.Failed("The search took too long. Try again, or pick a faster model in Settings.")
        } catch (e: java.io.IOException) {
            Outcome.Failed("Couldn't reach OpenRouter. Check the connection.")
        }
    }

    companion object { private const val BASE = "https://openrouter.ai/api/v1" }
}

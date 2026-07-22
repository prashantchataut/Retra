package app.retra.emulator.data

import android.content.Context
import app.retra.core.achievements.AchievementDefinition
import app.retra.core.achievements.AchievementEngine
import app.retra.core.achievements.AchievementEvent
import app.retra.core.achievements.AchievementIntegrity
import app.retra.core.achievements.AchievementProgress
import app.retra.core.achievements.RetraAchievements
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class AchievementStatus(
    val definition: AchievementDefinition,
    val progress: AchievementProgress,
    val completionRatio: Float
)

@Singleton
class AchievementRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val file = File(context.filesDir, "achievements/progress.tsv")
    private val lock = Any()
    private var progressById: Map<String, AchievementProgress> = load()
    private val mutableStatuses = MutableStateFlow(buildStatuses())
    val statuses: StateFlow<List<AchievementStatus>> = mutableStatuses

    suspend fun record(event: AchievementEvent, integrity: AchievementIntegrity): List<AchievementStatus> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val updated = progressById.toMutableMap()
            RetraAchievements.builtIns.forEach { definition ->
                val evaluation = AchievementEngine.evaluate(definition, updated[definition.id], event, integrity)
                updated[definition.id] = evaluation.progress
            }
            progressById = updated
            persist(updated)
            buildStatuses().also { mutableStatuses.value = it }
        }
    }

    fun unlockedCount(): Int = statuses.value.count { it.progress.unlockedAtEpochMillis != null }

    fun exportProgress(): ByteArray = synchronized(lock) {
        encode(progressById).toByteArray(Charsets.UTF_8)
    }

    /** Merges validated local achievement progress from a Retra backup. */
    suspend fun importProgress(bytes: ByteArray): Int = withContext(Dispatchers.IO) {
        require(bytes.size <= MAX_IMPORT_BYTES) { "Achievement progress exceeds the backup safety limit." }
        val imported = parse(bytes.toString(Charsets.UTF_8).lineSequence().toList())
        synchronized(lock) {
            val merged = progressById.toMutableMap()
            imported.forEach { (id, incoming) ->
                if (RetraAchievements.builtIns.none { it.id == id }) return@forEach
                val current = merged[id] ?: AchievementProgress(id)
                merged[id] = AchievementProgress(
                    achievementId = id,
                    count = maxOf(current.count, incoming.count),
                    sum = maxOf(current.sum, incoming.sum),
                    unlockedAtEpochMillis = listOfNotNull(current.unlockedAtEpochMillis, incoming.unlockedAtEpochMillis).minOrNull(),
                    uniqueKeys = current.uniqueKeys + incoming.uniqueKeys
                )
            }
            progressById = merged
            persist(merged)
            mutableStatuses.value = buildStatuses()
            imported.size
        }
    }

    private fun buildStatuses(): List<AchievementStatus> = RetraAchievements.builtIns.map { definition ->
        val progress = progressById[definition.id] ?: AchievementProgress(definition.id)
        AchievementStatus(definition, progress, AchievementEngine.completionRatio(definition, progress))
    }

    private fun load(): Map<String, AchievementProgress> =
        if (!file.isFile) emptyMap() else parse(file.readLines())

    private fun parse(lines: List<String>): Map<String, AchievementProgress> = lines
        .take(MAX_IMPORT_LINES)
        .mapNotNull { line ->
            runCatching {
                require(line.length <= MAX_LINE_CHARS)
                val parts = line.split('\t')
                require(parts.size == 5)
                val id = parts[0].take(120)
                require(id.matches(Regex("[a-z0-9._-]{1,120}")))
                val keys = if (parts[4].isBlank()) emptySet() else {
                    String(Base64.getUrlDecoder().decode(parts[4]), Charsets.UTF_8)
                        .split('\u0000')
                        .filter(String::isNotBlank)
                        .map { it.take(240) }
                        .take(2_000)
                        .toSet()
                }
                id to AchievementProgress(
                    achievementId = id,
                    count = parts[1].toLong().coerceAtLeast(0),
                    sum = parts[2].toLong().coerceAtLeast(0),
                    unlockedAtEpochMillis = parts[3].takeIf(String::isNotBlank)?.toLong()?.takeIf { it > 0 },
                    uniqueKeys = keys
                )
            }.getOrNull()
        }.toMap()

    private fun encode(progress: Map<String, AchievementProgress>): String =
        progress.values.sortedBy(AchievementProgress::achievementId).joinToString("\n") { item ->
            val keys = Base64.getUrlEncoder().withoutPadding().encodeToString(
                item.uniqueKeys.sorted().take(2_000).joinToString("\u0000").toByteArray()
            )
            listOf(item.achievementId, item.count, item.sum, item.unlockedAtEpochMillis ?: "", keys).joinToString("\t")
        }

    private fun persist(progress: Map<String, AchievementProgress>) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        val text = encode(progress)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(text.toByteArray())
                output.fd.sync()
            }
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 2 * 1024 * 1024
        const val MAX_IMPORT_LINES = 10_000
        const val MAX_LINE_CHARS = 32_768
    }

}

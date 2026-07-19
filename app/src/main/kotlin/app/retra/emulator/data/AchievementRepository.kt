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

    private fun buildStatuses(): List<AchievementStatus> = RetraAchievements.builtIns.map { definition ->
        val progress = progressById[definition.id] ?: AchievementProgress(definition.id)
        AchievementStatus(definition, progress, AchievementEngine.completionRatio(definition, progress))
    }

    private fun load(): Map<String, AchievementProgress> {
        if (!file.isFile) return emptyMap()
        return file.readLines().mapNotNull { line ->
            runCatching {
                val parts = line.split('\t')
                require(parts.size == 5)
                val id = parts[0]
                val keys = if (parts[4].isBlank()) emptySet() else {
                    String(Base64.getUrlDecoder().decode(parts[4]), Charsets.UTF_8)
                        .split('\u0000').filter(String::isNotBlank).toSet()
                }
                id to AchievementProgress(
                    achievementId = id,
                    count = parts[1].toLong(),
                    sum = parts[2].toLong(),
                    unlockedAtEpochMillis = parts[3].takeIf(String::isNotBlank)?.toLong(),
                    uniqueKeys = keys
                )
            }.getOrNull()
        }.toMap()
    }

    private fun persist(progress: Map<String, AchievementProgress>) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.tmp")
        val text = progress.values.sortedBy(AchievementProgress::achievementId).joinToString("\n") { item ->
            val keys = Base64.getUrlEncoder().withoutPadding().encodeToString(item.uniqueKeys.sorted().joinToString("\u0000").toByteArray())
            listOf(item.achievementId, item.count, item.sum, item.unlockedAtEpochMillis ?: "", keys).joinToString("\t")
        }
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
}

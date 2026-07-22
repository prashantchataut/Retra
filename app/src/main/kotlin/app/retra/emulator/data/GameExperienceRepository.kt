package app.retra.emulator.data

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.ScreenScalingMode
import app.retra.emulation.api.RuntimeMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private fun PerformanceProfile.stableProfile(): PerformanceProfile = when (this) {
    PerformanceProfile.BOOSTED, PerformanceProfile.EXTREME -> PerformanceProfile.BALANCED
    else -> this
}

data class GameLaunchProfile(
    val gameSha256: String,
    val performanceProfile: PerformanceProfile? = null,
    val scalingMode: ScreenScalingMode? = null,
    val displaySmoothing: Boolean? = null,
    val controlLayout: ControlLayoutPreset? = null,
    val showTouchControls: Boolean? = null,
    val fastForwardSpeed: Float? = null
)

@Singleton
class GameLaunchProfileRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val root = File(context.filesDir, "launch-profiles")
    private val mutableProfiles = MutableStateFlow<Map<String, GameLaunchProfile>>(emptyMap())
    val profiles: StateFlow<Map<String, GameLaunchProfile>> = mutableProfiles

    init {
        root.mkdirs()
        refresh()
    }

    fun profileFor(gameSha256: String): GameLaunchProfile? = mutableProfiles.value[gameSha256.lowercase()]

    fun save(profile: GameLaunchProfile) {
        val hash = profile.gameSha256.lowercase()
        require(HASH.matches(hash))
        val normalized = profile.copy(
            gameSha256 = hash,
            performanceProfile = profile.performanceProfile?.stableProfile(),
            fastForwardSpeed = profile.fastForwardSpeed?.coerceIn(1f, 16f)
        )
        val text = buildString {
            appendLine(MAGIC)
            appendLine("game=$hash")
            normalized.performanceProfile?.let { appendLine("performance=${it.name}") }
            normalized.scalingMode?.let { appendLine("scaling=${it.name}") }
            normalized.displaySmoothing?.let { appendLine("smoothing=${if (it) 1 else 0}") }
            normalized.controlLayout?.let { appendLine("controls=${it.name}") }
            normalized.showTouchControls?.let { appendLine("touch=${if (it) 1 else 0}") }
            normalized.fastForwardSpeed?.let { appendLine("fastForward=$it") }
        }
        writeAtomically(File(root, "$hash.rlp"), text.toByteArray())
        refresh()
    }

    fun clear(gameSha256: String): Boolean {
        val deleted = File(root, "${gameSha256.lowercase()}.rlp").delete()
        refresh()
        return deleted
    }

    private fun refresh() {
        mutableProfiles.value = root.listFiles()?.asSequence()
            ?.filter { it.isFile && it.extension == "rlp" && it.length() in 1..MAX_FILE_BYTES }
            ?.mapNotNull { file -> runCatching { parse(file.readLines()) }.getOrNull() }
            ?.associateBy(GameLaunchProfile::gameSha256)
            .orEmpty()
    }

    private fun parse(lines: List<String>): GameLaunchProfile {
        require(lines.firstOrNull() == MAGIC)
        val values = lines.drop(1).filter(String::isNotBlank).associate { line ->
            val index = line.indexOf('=')
            require(index > 0)
            line.substring(0, index) to line.substring(index + 1)
        }
        require(values.keys.all { it in setOf("game", "performance", "scaling", "smoothing", "controls", "touch", "fastForward") })
        val hash = values.getValue("game").lowercase()
        require(HASH.matches(hash))
        return GameLaunchProfile(
            gameSha256 = hash,
            performanceProfile = values["performance"]?.let { runCatching { PerformanceProfile.valueOf(it).stableProfile() }.getOrNull() },
            scalingMode = values["scaling"]?.let { runCatching { ScreenScalingMode.valueOf(it) }.getOrNull() },
            displaySmoothing = values["smoothing"]?.let { it == "1" },
            controlLayout = values["controls"]?.let { runCatching { ControlLayoutPreset.valueOf(it) }.getOrNull() },
            showTouchControls = values["touch"]?.let { it == "1" },
            fastForwardSpeed = values["fastForward"]?.toFloatOrNull()?.coerceIn(1f, 16f)
        )
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        require(bytes.size <= MAX_FILE_BYTES)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val MAGIC = "RETRA-LAUNCH-PROFILE-1"
        const val MAX_FILE_BYTES = 16 * 1024
        val HASH = Regex("[0-9a-f]{64}")
    }
}

data class PerformanceEvidence(
    val sessionCount: Int,
    val sampledSeconds: Long,
    val frameTimeP50Millis: Float,
    val frameTimeP95Millis: Float,
    val frameTimeP99Millis: Float,
    val averagePresentedFps: Float,
    val averageSpeedPercent: Float,
    val droppedFramesPerMinute: Float,
    val audioUnderrunsPerMinute: Float,
    val maximumThermalStatus: Int,
    val minimumBatteryPercent: Int?
)

data class PerformanceAdvice(
    val gameSha256: String,
    val evidence: PerformanceEvidence,
    val recommendedProfile: PerformanceProfile?,
    val confidence: Float,
    val reasons: List<String>,
    val ready: Boolean
)

private data class PerformanceSessionSummary(
    val gameSha256: String,
    val profile: PerformanceProfile,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val sampleCount: Int,
    val frameTimeP50Millis: Float,
    val frameTimeP95Millis: Float,
    val frameTimeP99Millis: Float,
    val averagePresentedFps: Float,
    val averageSpeedPercent: Float,
    val droppedFrames: Long,
    val audioUnderruns: Long,
    val maximumThermalStatus: Int,
    val minimumBatteryPercent: Int?
)

private class PerformanceAccumulator(
    val gameSha256: String,
    val profile: PerformanceProfile,
    val startedAtEpochMillis: Long
) {
    val frameTimes = ArrayList<Float>(900)
    var fpsTotal = 0f
    var speedTotal = 0f
    var samples = 0
    var firstDropped: Long? = null
    var lastDropped: Long = 0
    var firstUnderruns: Long? = null
    var lastUnderruns: Long = 0
    var maximumThermalStatus: Int = 0
    var minimumBatteryPercent: Int? = null

    fun record(metrics: RuntimeMetrics, thermalStatus: Int, batteryPercent: Int?) {
        if (metrics.frameTimeMillis.isFinite() && metrics.frameTimeMillis in 0.1f..250f) frameTimes += metrics.frameTimeMillis
        if (metrics.presentedFps.isFinite()) fpsTotal += metrics.presentedFps.coerceIn(0f, 240f)
        if (metrics.speedPercent.isFinite()) speedTotal += metrics.speedPercent.coerceIn(0f, 1600f)
        if (firstDropped == null) firstDropped = metrics.droppedFrames
        if (firstUnderruns == null) firstUnderruns = metrics.audioUnderruns
        lastDropped = metrics.droppedFrames
        lastUnderruns = metrics.audioUnderruns
        maximumThermalStatus = maxOf(maximumThermalStatus, thermalStatus)
        if (batteryPercent != null) minimumBatteryPercent = minOf(minimumBatteryPercent ?: batteryPercent, batteryPercent)
        samples++
    }

    fun finish(endedAt: Long): PerformanceSessionSummary? {
        if (samples < MIN_SESSION_SAMPLES || frameTimes.size < MIN_SESSION_SAMPLES / 2) return null
        val sorted = frameTimes.sorted()
        return PerformanceSessionSummary(
            gameSha256 = gameSha256,
            profile = profile,
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = endedAt,
            sampleCount = samples,
            frameTimeP50Millis = percentile(sorted, 0.50f),
            frameTimeP95Millis = percentile(sorted, 0.95f),
            frameTimeP99Millis = percentile(sorted, 0.99f),
            averagePresentedFps = fpsTotal / samples,
            averageSpeedPercent = speedTotal / samples,
            droppedFrames = (lastDropped - (firstDropped ?: lastDropped)).coerceAtLeast(0),
            audioUnderruns = (lastUnderruns - (firstUnderruns ?: lastUnderruns)).coerceAtLeast(0),
            maximumThermalStatus = maximumThermalStatus,
            minimumBatteryPercent = minimumBatteryPercent
        )
    }

    private fun percentile(sorted: List<Float>, quantile: Float): Float {
        if (sorted.isEmpty()) return 0f
        val index = (ceil(sorted.size * quantile).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    companion object {
        const val MIN_SESSION_SAMPLES = 120
    }
}

@Singleton
class PerformanceAdvisorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val root = File(context.filesDir, "performance-evidence")
    private val mutableAdvice = MutableStateFlow<Map<String, PerformanceAdvice>>(emptyMap())
    val advice: StateFlow<Map<String, PerformanceAdvice>> = mutableAdvice
    private var active: PerformanceAccumulator? = null

    init {
        root.mkdirs()
        refreshAdvice()
    }

    fun beginSession(gameSha256: String, profile: PerformanceProfile) {
        endSession()
        val hash = gameSha256.lowercase()
        if (!HASH.matches(hash)) return
        active = PerformanceAccumulator(hash, profile, System.currentTimeMillis())
    }

    fun record(metrics: RuntimeMetrics) {
        val accumulator = active ?: return
        accumulator.record(metrics, thermalStatus(), batteryPercent())
    }

    fun endSession() {
        val summary = active?.finish(System.currentTimeMillis())
        active = null
        if (summary != null) {
            appendSummary(summary)
            refreshAdvice()
        }
    }

    fun clear(gameSha256: String): Boolean {
        val deleted = evidenceFile(gameSha256).delete()
        refreshAdvice()
        return deleted
    }

    private fun refreshAdvice() {
        val result = root.listFiles()?.asSequence()
            ?.filter { it.isFile && it.extension == "rpe" && it.length() in 1..MAX_EVIDENCE_BYTES }
            ?.mapNotNull { file ->
                val summaries = readSummaries(file)
                val hash = file.nameWithoutExtension.lowercase()
                if (summaries.isEmpty() || !HASH.matches(hash)) null else hash to buildAdvice(hash, summaries)
            }
            ?.toMap()
            .orEmpty()
        mutableAdvice.value = result
    }

    private fun buildAdvice(hash: String, summaries: List<PerformanceSessionSummary>): PerformanceAdvice {
        val recent = summaries.takeLast(MAX_SESSIONS_PER_GAME)
        val totalSamples = recent.sumOf(PerformanceSessionSummary::sampleCount)
        val totalSeconds = totalSamples.toLong()
        val weighted = totalSamples.coerceAtLeast(1).toFloat()
        fun avg(selector: (PerformanceSessionSummary) -> Float): Float = recent.sumOf { (selector(it) * it.sampleCount).toDouble() }.toFloat() / weighted
        val dropped = recent.sumOf(PerformanceSessionSummary::droppedFrames)
        val underruns = recent.sumOf(PerformanceSessionSummary::audioUnderruns)
        val evidence = PerformanceEvidence(
            sessionCount = recent.size,
            sampledSeconds = totalSeconds,
            frameTimeP50Millis = avg(PerformanceSessionSummary::frameTimeP50Millis),
            frameTimeP95Millis = avg(PerformanceSessionSummary::frameTimeP95Millis),
            frameTimeP99Millis = avg(PerformanceSessionSummary::frameTimeP99Millis),
            averagePresentedFps = avg(PerformanceSessionSummary::averagePresentedFps),
            averageSpeedPercent = avg(PerformanceSessionSummary::averageSpeedPercent),
            droppedFramesPerMinute = dropped * 60f / totalSeconds.coerceAtLeast(1),
            audioUnderrunsPerMinute = underruns * 60f / totalSeconds.coerceAtLeast(1),
            maximumThermalStatus = recent.maxOfOrNull(PerformanceSessionSummary::maximumThermalStatus) ?: 0,
            minimumBatteryPercent = recent.mapNotNull(PerformanceSessionSummary::minimumBatteryPercent).minOrNull()
        )
        val ready = totalSamples >= MIN_ADVICE_SAMPLES && recent.size >= 1
        val reasons = mutableListOf<String>()
        val recommendation = when {
            !ready -> null
            evidence.maximumThermalStatus >= THERMAL_SEVERE -> {
                reasons += "The device reached a severe thermal state during measured play."
                PerformanceProfile.BATTERY_SAVER
            }
            evidence.averageSpeedPercent < 96f || evidence.frameTimeP95Millis > 24f -> {
                reasons += "Sustained frame pacing or emulation speed fell below the stable range."
                PerformanceProfile.BALANCED
            }
            evidence.audioUnderrunsPerMinute > 2f || evidence.droppedFramesPerMinute > 4f -> {
                reasons += "Audio underruns or dropped frames were repeatedly observed."
                PerformanceProfile.BALANCED
            }
            evidence.frameTimeP95Millis <= 18.5f && evidence.averageSpeedPercent >= 99f && evidence.audioUnderrunsPerMinute < 0.5f -> {
                reasons += "Frame pacing remained stable across the measured sample."
                PerformanceProfile.AUTHENTIC
            }
            else -> {
                reasons += "The balanced profile best matches the measured headroom."
                PerformanceProfile.BALANCED
            }
        }
        if (!ready) reasons += "Retra needs at least two minutes of active gameplay before making a recommendation."
        val confidence = if (!ready) 0f else (totalSamples / 900f).coerceIn(0.25f, 1f)
        return PerformanceAdvice(hash, evidence, recommendation, confidence, reasons, ready)
    }

    private fun appendSummary(summary: PerformanceSessionSummary) {
        val file = evidenceFile(summary.gameSha256)
        file.parentFile?.mkdirs()
        val line = encodeSummary(summary) + "\n"
        FileOutputStream(file, true).use { output ->
            output.write(line.toByteArray())
            output.fd.sync()
        }
        val summaries = readSummaries(file).takeLast(MAX_STORED_SESSIONS)
        val bytes = summaries.joinToString("\n", postfix = if (summaries.isEmpty()) "" else "\n", transform = ::encodeSummary).toByteArray()
        writeAtomically(file, bytes)
    }

    private fun readSummaries(file: File): List<PerformanceSessionSummary> = file.readLines()
        .take(MAX_STORED_SESSIONS * 2)
        .mapNotNull { line -> runCatching { decodeSummary(line) }.getOrNull() }
        .filter { it.gameSha256 == file.nameWithoutExtension.lowercase() }

    private fun encodeSummary(value: PerformanceSessionSummary): String = listOf(
        MAGIC,
        value.gameSha256,
        value.profile.name,
        value.startedAtEpochMillis,
        value.endedAtEpochMillis,
        value.sampleCount,
        value.frameTimeP50Millis,
        value.frameTimeP95Millis,
        value.frameTimeP99Millis,
        value.averagePresentedFps,
        value.averageSpeedPercent,
        value.droppedFrames,
        value.audioUnderruns,
        value.maximumThermalStatus,
        value.minimumBatteryPercent ?: -1
    ).joinToString("|")

    private fun decodeSummary(line: String): PerformanceSessionSummary {
        val parts = line.split('|')
        require(parts.size == 15 && parts[0] == MAGIC)
        val hash = parts[1].lowercase()
        require(HASH.matches(hash))
        return PerformanceSessionSummary(
            gameSha256 = hash,
            profile = PerformanceProfile.valueOf(parts[2]),
            startedAtEpochMillis = parts[3].toLong(),
            endedAtEpochMillis = parts[4].toLong(),
            sampleCount = parts[5].toInt().coerceIn(1, 100_000),
            frameTimeP50Millis = parts[6].toFloat(),
            frameTimeP95Millis = parts[7].toFloat(),
            frameTimeP99Millis = parts[8].toFloat(),
            averagePresentedFps = parts[9].toFloat(),
            averageSpeedPercent = parts[10].toFloat(),
            droppedFrames = parts[11].toLong().coerceAtLeast(0),
            audioUnderruns = parts[12].toLong().coerceAtLeast(0),
            maximumThermalStatus = parts[13].toInt().coerceIn(0, 10),
            minimumBatteryPercent = parts[14].toInt().takeIf { it in 0..100 }
        )
    }

    private fun evidenceFile(gameSha256: String): File = File(root, "${gameSha256.lowercase()}.rpe")

    private fun thermalStatus(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        return context.getSystemService(PowerManager::class.java)?.currentThermalStatus ?: 0
    }

    private fun batteryPercent(): Int? = context.getSystemService(BatteryManager::class.java)
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?.takeIf { it in 0..100 }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        require(bytes.size <= MAX_EVIDENCE_BYTES)
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val MAGIC = "RPE1"
        const val MAX_EVIDENCE_BYTES = 256 * 1024
        const val MAX_STORED_SESSIONS = 40
        const val MAX_SESSIONS_PER_GAME = 12
        const val MIN_ADVICE_SAMPLES = 120
        const val THERMAL_SEVERE = 3
        val HASH = Regex("[0-9a-f]{64}")
    }
}

package app.retra.data

import app.retra.core.AtomicFiles
import app.retra.core.RetraJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.Executors

/** App settings in `settings.json`, exposed as a StateFlow. Writes happen on a single background thread. */
class SettingsRepository(root: File) {
    private val file = File(root, "settings.json")
    private val writer = Executors.newSingleThreadExecutor { r -> Thread(r, "retra-settings").apply { isDaemon = true } }
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    val current: Settings get() = _settings.value

    private fun load(): Settings = runCatching {
        if (file.exists()) RetraJson.decodeFromString(Settings.serializer(), file.readText()) else Settings()
    }.getOrDefault(Settings())

    fun update(transform: (Settings) -> Settings) {
        _settings.update(transform)
        val snapshot = _settings.value
        writer.execute { runCatching { AtomicFiles.writeText(file, RetraJson.encodeToString(Settings.serializer(), snapshot)) } }
    }
}

package app.retra.emulator.data

import android.content.Context
import app.retra.core.emulation.EmulatorButton
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A physical controller observed by Retra. The descriptor is stable enough for per-device profiles. */
data class ControllerDeviceSummary(
    val descriptor: String,
    val displayName: String,
    val lastSeenAtEpochMillis: Long
)

data class ControllerProfile(
    val id: String,
    val deviceDescriptor: String,
    val deviceName: String,
    val gameSha256: String? = null,
    val deadZone: Float = 0.18f,
    val triggerThreshold: Float = 0.55f,
    val bindings: Map<Int, EmulatorButton> = defaultControllerBindings()
) {
    val alternateBindings: Map<EmulatorButton, List<Int>>
        get() = bindings.entries.groupBy({ it.value }, { it.key }).filterValues { it.size > 1 }
}

data class ControllerCaptureEvent(
    val deviceDescriptor: String,
    val deviceName: String,
    val keyCode: Int,
    val capturedAtEpochMillis: Long
)

@Singleton
class ControllerProfileRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val root = File(context.filesDir, "controller-profiles")
    private val mutableProfiles = MutableStateFlow<List<ControllerProfile>>(emptyList())
    val profiles: StateFlow<List<ControllerProfile>> = mutableProfiles
    private val mutableDevices = MutableStateFlow<List<ControllerDeviceSummary>>(emptyList())
    val devices: StateFlow<List<ControllerDeviceSummary>> = mutableDevices

    init {
        root.mkdirs()
        refresh()
    }

    fun registerDevice(descriptor: String, displayName: String) {
        if (descriptor.isBlank()) return
        val now = System.currentTimeMillis()
        val updated = mutableDevices.value
            .filterNot { it.descriptor == descriptor }
            .plus(ControllerDeviceSummary(descriptor, displayName.ifBlank { "Game controller" }, now))
            .sortedByDescending(ControllerDeviceSummary::lastSeenAtEpochMillis)
            .take(12)
        mutableDevices.value = updated
    }

    fun resolve(deviceDescriptor: String, deviceName: String, gameSha256: String?): ControllerProfile {
        val normalizedHash = gameSha256?.lowercase()?.takeIf { HASH.matches(it) }
        return mutableProfiles.value.firstOrNull {
            it.deviceDescriptor == deviceDescriptor && it.gameSha256 == normalizedHash
        } ?: mutableProfiles.value.firstOrNull {
            it.deviceDescriptor == deviceDescriptor && it.gameSha256 == null
        } ?: ControllerProfile(
            id = profileId(deviceDescriptor, normalizedHash),
            deviceDescriptor = deviceDescriptor,
            deviceName = deviceName.ifBlank { "Game controller" },
            gameSha256 = normalizedHash
        )
    }

    fun bind(
        deviceDescriptor: String,
        deviceName: String,
        gameSha256: String?,
        keyCode: Int,
        button: EmulatorButton
    ): ControllerProfile {
        require(keyCode in 1..4096) { "Controller key code is outside the supported range." }
        val current = resolve(deviceDescriptor, deviceName, gameSha256)
        // One physical input must never trigger multiple emulator actions. Rebinding replaces it.
        val updated = current.copy(bindings = current.bindings + (keyCode to button))
        persist(updated)
        return updated
    }

    fun unbind(deviceDescriptor: String, deviceName: String, gameSha256: String?, keyCode: Int): ControllerProfile {
        val current = resolve(deviceDescriptor, deviceName, gameSha256)
        val updated = current.copy(bindings = current.bindings - keyCode)
        persist(updated)
        return updated
    }

    fun setCalibration(
        deviceDescriptor: String,
        deviceName: String,
        gameSha256: String?,
        deadZone: Float,
        triggerThreshold: Float
    ): ControllerProfile {
        val current = resolve(deviceDescriptor, deviceName, gameSha256)
        val updated = current.copy(
            deadZone = deadZone.coerceIn(0.05f, 0.65f),
            triggerThreshold = triggerThreshold.coerceIn(0.15f, 0.95f)
        )
        persist(updated)
        return updated
    }

    fun reset(deviceDescriptor: String, deviceName: String, gameSha256: String?): ControllerProfile {
        val normalizedHash = gameSha256?.lowercase()?.takeIf(HASH::matches)
        val profile = ControllerProfile(
            id = profileId(deviceDescriptor, normalizedHash),
            deviceDescriptor = deviceDescriptor,
            deviceName = deviceName.ifBlank { "Game controller" },
            gameSha256 = normalizedHash
        )
        persist(profile)
        return profile
    }

    fun delete(profile: ControllerProfile): Boolean {
        val deleted = profileFile(profile.id).delete()
        refresh()
        return deleted
    }

    private fun persist(profile: ControllerProfile) {
        require(profile.deviceDescriptor.isNotBlank() && profile.deviceDescriptor.length <= 512)
        require(profile.deviceName.length <= 160)
        require(profile.gameSha256 == null || HASH.matches(profile.gameSha256))
        require(profile.bindings.size <= 64)
        val lines = buildList {
            add(MAGIC)
            add("id=${encode(profile.id)}")
            add("descriptor=${encode(profile.deviceDescriptor)}")
            add("name=${encode(profile.deviceName)}")
            add("game=${profile.gameSha256.orEmpty()}")
            add("deadZone=${profile.deadZone.coerceIn(0.05f, 0.65f)}")
            add("trigger=${profile.triggerThreshold.coerceIn(0.15f, 0.95f)}")
            profile.bindings.toSortedMap().forEach { (keyCode, button) ->
                add("bind=$keyCode:${button.name}")
            }
        }
        writeAtomically(profileFile(profile.id), lines.joinToString("\n", postfix = "\n").toByteArray())
        refresh()
    }

    private fun refresh() {
        mutableProfiles.value = root.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == "rcp" && it.length() in 1..MAX_PROFILE_BYTES }
            ?.mapNotNull { runCatching { parse(it.readLines()) }.getOrNull() }
            ?.sortedWith(compareBy<ControllerProfile> { it.deviceName.lowercase() }.thenBy { it.gameSha256 != null })
            ?.toList()
            .orEmpty()
    }

    private fun parse(lines: List<String>): ControllerProfile {
        require(lines.firstOrNull() == MAGIC) { "Unsupported controller profile." }
        require(lines.size <= 80) { "Controller profile is too large." }
        val scalar = linkedMapOf<String, String>()
        val bindings = linkedMapOf<Int, EmulatorButton>()
        lines.drop(1).filter(String::isNotBlank).forEach { line ->
            val separator = line.indexOf('=')
            require(separator > 0) { "Malformed controller profile line." }
            val key = line.substring(0, separator)
            val value = line.substring(separator + 1)
            if (key == "bind") {
                val parts = value.split(':', limit = 2)
                require(parts.size == 2)
                val keyCode = parts[0].toInt()
                val button = EmulatorButton.valueOf(parts[1])
                require(bindings.put(keyCode, button) == null) { "Duplicate controller binding." }
            } else {
                require(key in setOf("id", "descriptor", "name", "game", "deadZone", "trigger"))
                require(scalar.put(key, value) == null) { "Duplicate controller field." }
            }
        }
        val descriptor = decode(scalar.getValue("descriptor"))
        val game = scalar["game"].orEmpty().ifBlank { null }
        require(game == null || HASH.matches(game))
        return ControllerProfile(
            id = decode(scalar.getValue("id")),
            deviceDescriptor = descriptor,
            deviceName = decode(scalar.getValue("name")),
            gameSha256 = game,
            deadZone = scalar.getValue("deadZone").toFloat().coerceIn(0.05f, 0.65f),
            triggerThreshold = scalar.getValue("trigger").toFloat().coerceIn(0.15f, 0.95f),
            bindings = bindings.ifEmpty { defaultControllerBindings().toMutableMap() }
        )
    }

    private fun profileFile(id: String): File = File(root, "${safeId(id)}.rcp")

    private fun writeAtomically(target: File, bytes: ByteArray) {
        require(bytes.size <= MAX_PROFILE_BYTES)
        root.mkdirs()
        val temporary = File(root, ".${target.name}.tmp")
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

    private fun profileId(descriptor: String, gameSha256: String?): String = sha256(
        "$descriptor\u0000${gameSha256.orEmpty()}".toByteArray()
    ).take(24)

    private fun safeId(value: String): String = value.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '-' }.take(48)
        .ifBlank { sha256(value.toByteArray()).take(24) }

    private fun encode(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    private fun decode(value: String): String = Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAGIC = "RETRA-CONTROLLER-1"
        const val MAX_PROFILE_BYTES = 32 * 1024L
        val HASH = Regex("[0-9a-f]{64}")
    }
}

fun defaultControllerBindings(): Map<Int, EmulatorButton> = linkedMapOf(
    android.view.KeyEvent.KEYCODE_DPAD_UP to EmulatorButton.UP,
    android.view.KeyEvent.KEYCODE_DPAD_DOWN to EmulatorButton.DOWN,
    android.view.KeyEvent.KEYCODE_DPAD_LEFT to EmulatorButton.LEFT,
    android.view.KeyEvent.KEYCODE_DPAD_RIGHT to EmulatorButton.RIGHT,
    android.view.KeyEvent.KEYCODE_BUTTON_A to EmulatorButton.A,
    android.view.KeyEvent.KEYCODE_BUTTON_B to EmulatorButton.B,
    android.view.KeyEvent.KEYCODE_BUTTON_X to EmulatorButton.B,
    android.view.KeyEvent.KEYCODE_BUTTON_Y to EmulatorButton.A,
    android.view.KeyEvent.KEYCODE_BUTTON_L1 to EmulatorButton.L,
    android.view.KeyEvent.KEYCODE_BUTTON_R1 to EmulatorButton.R,
    android.view.KeyEvent.KEYCODE_BUTTON_START to EmulatorButton.START,
    android.view.KeyEvent.KEYCODE_BUTTON_SELECT to EmulatorButton.SELECT,
    android.view.KeyEvent.KEYCODE_BUTTON_MODE to EmulatorButton.MENU
)

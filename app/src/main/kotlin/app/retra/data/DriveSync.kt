package app.retra.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import app.retra.core.DriveSyncPlanner
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface DriveStatus {
    data object Disconnected : DriveStatus
    data object Idle : DriveStatus
    data object Syncing : DriveStatus
    data class Failed(val message: String) : DriveStatus
}

/** What asking Google for Drive access led to. */
sealed interface DriveAuth {
    data class Token(val accessToken: String) : DriveAuth
    /** Google needs to show its consent screen: launch [sender] and pass the result to [DriveSync.finishConsent]. */
    data class NeedsConsent(val sender: IntentSender) : DriveAuth
    data class Failed(val message: String) : DriveAuth
}

/**
 * Backs battery saves up to the player's own Google Drive, in the app's hidden "appDataFolder" (the scope is
 * `drive.appdata`: Retra can't see or touch any other file in the Drive, and the user can clear it from Drive
 * settings). No server of ours is involved; the phone talks to Google directly.
 *
 * Setup for a self-built APK: create an OAuth client of type "Android" in Google Cloud Console for package
 * `app.retra.emulator` and the SHA-1 of the signing key, and enable the Drive API. See `docs/GOOGLE_DRIVE.md`.
 */
class DriveSync(
    private val context: Context,
    private val settings: SettingsRepository,
    private val saves: SaveRepository,
    private val library: LibraryRepository,
    private val messages: Messages,
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<DriveStatus>(if (settings.current.driveAccount != null) DriveStatus.Idle else DriveStatus.Disconnected)
    val status: StateFlow<DriveStatus> = _status.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true }

    private val request: AuthorizationRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SCOPE))).build()

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { c ->
        addOnSuccessListener { c.resume(it) }
        addOnFailureListener { c.resumeWithException(it) }
    }

    /** Asks for Drive access. Silent when the user already agreed once. */
    suspend fun authorize(activity: Activity? = null): DriveAuth = try {
        val client = if (activity != null) Identity.getAuthorizationClient(activity) else Identity.getAuthorizationClient(context)
        val r = client.authorize(request).await()
        resultToAuth(r)
    } catch (e: Exception) {
        DriveAuth.Failed(friendly(e))
    }

    private fun resultToAuth(r: AuthorizationResult): DriveAuth {
        if (r.hasResolution()) return r.pendingIntent?.intentSender?.let { DriveAuth.NeedsConsent(it) } ?: DriveAuth.Failed("Google needs more permission but couldn't ask.")
        return r.accessToken?.let { DriveAuth.Token(it) } ?: DriveAuth.Failed("Google didn't give Retra access.")
    }

    /** Reads the consent screen's result and finishes connecting. */
    suspend fun finishConsent(activity: Activity, data: Intent?): DriveAuth = try {
        resultToAuth(Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data))
    } catch (e: Exception) {
        DriveAuth.Failed(friendly(e))
    }

    private fun friendly(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            "10:" in m || "DEVELOPER_ERROR" in m -> "Google sign-in isn't set up for this build of Retra. See docs/GOOGLE_DRIVE.md."
            "16:" in m || "cancel" in m.lowercase() -> "Sign-in was cancelled."
            "7:" in m || "network" in m.lowercase() -> "No connection to Google right now."
            else -> "Couldn't connect to Google Drive."
        }
    }

    /** Connects with a fresh token: records the account email and runs a first sync. */
    suspend fun connect(accessToken: String): Boolean {
        _status.value = DriveStatus.Syncing
        val email = runCatching { withContext(Dispatchers.IO) { fetchEmail(accessToken) } }.getOrNull()
        if (email == null) {
            _status.value = DriveStatus.Failed("Google Drive didn't answer. Check the connection and that the Drive API is enabled for this app.")
            return false
        }
        settings.update { it.copy(driveAccount = email) }
        val report = sync(accessToken)
        messages.show("Connected to $email. ${report?.summary() ?: ""}".trim(), MessageTone.SUCCESS)
        return true
    }

    fun disconnect() {
        settings.update { it.copy(driveAccount = null, driveLastSync = 0) }
        _status.value = DriveStatus.Disconnected
        messages.show("Disconnected from Google Drive. Your saves in Drive stay put.")
    }

    /** Background sync after a play session or at start. Never throws and never shows UI. */
    suspend fun autoSync() {
        if (settings.current.driveAccount == null || !settings.current.driveAutoSync) return
        val auth = authorize()
        if (auth is DriveAuth.Token) sync(auth.accessToken, quiet = true)
    }

    suspend fun syncNow(): DriveSyncPlanner.Report? {
        return when (val a = authorize()) {
            is DriveAuth.Token -> sync(a.accessToken).also { it?.let { r -> messages.show(r.summary(), MessageTone.SUCCESS) } }
            is DriveAuth.NeedsConsent -> { _status.value = DriveStatus.Failed("Google needs you to grant access again."); null }
            is DriveAuth.Failed -> { _status.value = DriveStatus.Failed(a.message); messages.show(a.message, MessageTone.ERROR); null }
        }
    }

    suspend fun sync(token: String, quiet: Boolean = false): DriveSyncPlanner.Report? = mutex.withLock {
        _status.value = DriveStatus.Syncing
        try {
            val report = withContext(Dispatchers.IO) { runSync(token) }
            settings.update { it.copy(driveLastSync = System.currentTimeMillis()) }
            _status.value = DriveStatus.Idle
            report
        } catch (e: DriveHttpException) {
            val msg = when (e.code) {
                401 -> "Google Drive access expired. Reconnect in Settings."
                403 -> "Google Drive refused. Is the Drive API enabled for this app?"
                else -> "Google Drive error (${e.code})."
            }
            _status.value = DriveStatus.Failed(msg)
            if (!quiet) messages.show(msg, MessageTone.ERROR)
            null
        } catch (e: Exception) {
            _status.value = DriveStatus.Failed("Couldn't reach Google Drive.")
            if (!quiet) messages.show("Couldn't reach Google Drive.", MessageTone.ERROR)
            null
        }
    }

    // ---- the actual work (blocking, IO thread) ----

    private fun runSync(token: String): DriveSyncPlanner.Report {
        val localSaves = HashMap<String, DriveSyncPlanner.Local>()
        for (g in library.games.value) {
            val f = saves.savFile(g.id)
            if (f.exists() && f.length() > 0) localSaves[g.id] = DriveSyncPlanner.Local(md5(f.readBytes()), f.lastModified())
        }
        val remote = list(token)
        val plan = DriveSyncPlanner.plan(localSaves, remote, library.games.value.map { it.id }.toSet())
        var up = 0
        var down = 0
        var failed = 0
        for (a in plan) {
            try {
                when (a) {
                    is DriveSyncPlanner.Action.Upload -> {
                        upload(token, a.gameId, a.updateFileId, saves.readBatterySave(a.gameId) ?: continue, saves.savFile(a.gameId).lastModified())
                        up++
                    }
                    is DriveSyncPlanner.Action.Download -> {
                        val bytes = download(token, a.fileId)
                        saves.writeBatterySave(a.gameId, bytes, backup = a.replacesLocal)
                        down++
                    }
                }
            } catch (e: DriveHttpException) {
                if (e.code == 401 || e.code == 403) throw e
                failed++
            } catch (e: java.io.IOException) {
                failed++
            }
        }
        return DriveSyncPlanner.Report(up, down, failed, localSaves.size - up)
    }

    private fun md5(b: ByteArray) = MessageDigest.getInstance("MD5").digest(b).joinToString("") { "%02x".format(it) }

    class DriveHttpException(val code: Int, message: String) : java.io.IOException(message)

    private fun open(url: String, token: String, method: String = "GET"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $token")
        }

    private fun read(c: HttpURLConnection): ByteArray {
        val code = c.responseCode
        if (code !in 200..299) {
            val body = runCatching { c.errorStream?.readBytes()?.decodeToString() }.getOrNull().orEmpty()
            throw DriveHttpException(code, body.take(200))
        }
        return c.inputStream.use { it.readBytes() }
    }

    private fun fetchEmail(token: String): String? {
        val body = read(open("$API/about?fields=user(emailAddress)", token)).decodeToString()
        return json.parseToJsonElement(body).jsonObject["user"]?.jsonObject?.get("emailAddress")?.jsonPrimitive?.contentOrNull
    }

    private fun list(token: String): Map<String, DriveSyncPlanner.Remote> {
        val out = HashMap<String, DriveSyncPlanner.Remote>()
        var page: String? = null
        do {
            val q = "$API/files?spaces=appDataFolder&pageSize=1000&fields=nextPageToken,files(id,name,md5Checksum,modifiedTime)" +
                (page?.let { "&pageToken=$it" } ?: "")
            val obj: JsonObject = json.parseToJsonElement(read(open(q, token)).decodeToString()).jsonObject
            for (f in obj["files"]?.jsonArray.orEmpty()) {
                val o = f.jsonObject
                val id = DriveSyncPlanner.gameId(o["name"]?.jsonPrimitive?.contentOrNull ?: continue) ?: continue
                val modified = o["modifiedTime"]?.jsonPrimitive?.contentOrNull?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
                out[id] = DriveSyncPlanner.Remote(o["id"]!!.jsonPrimitive.content, o["md5Checksum"]?.jsonPrimitive?.contentOrNull.orEmpty(), modified)
            }
            page = obj["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (page != null)
        return out
    }

    private fun download(token: String, fileId: String): ByteArray = read(open("$API/files/$fileId?alt=media", token))

    private fun upload(token: String, gameId: String, existing: String?, data: ByteArray, modifiedMs: Long) {
        val modified = Instant.ofEpochMilli(modifiedMs).toString()
        val boundary = "retra" + System.nanoTime()
        val meta = if (existing == null) """{"name":"${DriveSyncPlanner.fileName(gameId)}","parents":["appDataFolder"],"modifiedTime":"$modified"}"""
        else """{"modifiedTime":"$modified"}"""
        val url = if (existing == null) "$UPLOAD/files?uploadType=multipart" else "$UPLOAD/files/$existing?uploadType=multipart"
        val c = open(url, token, if (existing == null) "POST" else "PATCH")
        c.doOutput = true
        c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        c.outputStream.use { o ->
            o.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
            o.write(data)
            o.write("\r\n--$boundary--".toByteArray())
        }
        read(c)
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}

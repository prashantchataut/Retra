package app.retra.link

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Room codes carry the host's address, so a friend only needs the code: `K3F9A-0Q7ZM` is the IPv4 address plus the
 * port (48 bits) written in a 32-letter alphabet without look-alikes. No server of ours is involved: rooms work on
 * the same Wi-Fi, over a VPN such as Tailscale or ZeroTier, or through a forwarded port.
 */
object RoomCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    fun encode(ipv4: ByteArray, port: Int): String {
        require(ipv4.size == 4 && port in 1..65535)
        var v = 0L
        for (b in ipv4) v = (v shl 8) or (b.toLong() and 0xFF)
        v = (v shl 16) or port.toLong()
        val chars = CharArray(10) { i -> ALPHABET[((v shr (5 * (9 - i))) and 31).toInt()] }
        return String(chars, 0, 5) + "-" + String(chars, 5, 5)
    }

    /** Returns the address and port, or null when [code] isn't a valid room code. */
    fun decode(code: String): Pair<String, Int>? {
        val clean = code.uppercase().filter { it != '-' && !it.isWhitespace() }
            .replace('O', '0').replace('I', '1').replace('L', '1').replace('U', 'V')
        if (clean.length != 10) return null
        var v = 0L
        for (ch in clean) {
            val i = ALPHABET.indexOf(ch)
            if (i < 0) return null
            v = (v shl 5) or i.toLong()
        }
        val port = (v and 0xFFFF).toInt()
        val ip = (v shr 16)
        if (ip shr 32 != 0L || port == 0) return null
        val host = "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"
        return host to port
    }
}

@Serializable
data class Peer(val id: Int, val name: String, val game: String = "", val gameCode: String = "", val ready: Boolean = false, val host: Boolean = false)

@Serializable
data class ChatLine(val from: String, val text: String, val at: Long)

/** One line of the wire protocol. [t] is the type: hello, roster, chat, ready, start, bye. */
@Serializable
data class Wire(
    val t: String,
    val name: String? = null,
    val game: String? = null,
    val gameCode: String? = null,
    val text: String? = null,
    val ready: Boolean? = null,
    val players: List<Peer>? = null,
    val you: Int? = null,
)

enum class RoomPhase { IDLE, OPENING, LOBBY, STARTING, CLOSED, ERROR }

data class RoomState(
    val phase: RoomPhase = RoomPhase.IDLE,
    val code: String = "",
    val isHost: Boolean = false,
    val you: Int = 0,
    val players: List<Peer> = emptyList(),
    val chat: List<ChatLine> = emptyList(),
    val message: String? = null,
) {
    val everyoneReady get() = players.size >= 2 && players.all { it.ready }
    val sameGame get() = players.map { it.gameCode }.filter { it.isNotBlank() }.distinct().size <= 1
}

/**
 * A link room: one host, up to three friends, a lobby with ready checks and chat. The same newline-delimited channel
 * is what carries cable data between games once the core exposes its serial port; today it carries the lobby.
 */
class LinkRoom(private val clock: () -> Long = System::currentTimeMillis) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val _state = MutableStateFlow(RoomState())
    val state: StateFlow<RoomState> = _state.asStateFlow()

    private class Conn(val id: Int, val socket: Socket, val out: BufferedWriter)
    private val conns = CopyOnWriteArrayList<Conn>()
    private var server: ServerSocket? = null
    private var clientConn: Conn? = null
    private var job: Job? = null
    private var nextId = 1

    // ---- host ----

    suspend fun host(name: String, game: String, gameCode: String) = withContext(Dispatchers.IO) {
        close()
        _state.value = RoomState(phase = RoomPhase.OPENING, isHost = true)
        try {
            val ss = ServerSocket(0)
            server = ss
            val ip = localIpv4()
            val me = Peer(0, name.ifBlank { "Host" }, game, gameCode, ready = false, host = true)
            _state.value = RoomState(
                phase = RoomPhase.LOBBY, code = if (ip != null) RoomCode.encode(ip, ss.localPort) else "", isHost = true, you = 0,
                players = listOf(me), message = if (ip == null) "No network address found. Connect to Wi-Fi to invite friends." else null,
            )
            job = scope.launch { acceptLoop(ss) }
        } catch (e: Exception) {
            _state.value = RoomState(phase = RoomPhase.ERROR, message = "Couldn't open a room: ${e.message ?: "unknown error"}")
        }
    }

    private suspend fun acceptLoop(ss: ServerSocket) {
        while (scope.isActive && !ss.isClosed) {
            val s = try { ss.accept() } catch (_: Exception) { break }
            if (conns.size >= 3) { runCatching { s.close() }; continue }
            val id = nextId++
            val conn = Conn(id, s, BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)))
            conns += conn
            scope.launch { hostReader(conn) }
        }
    }

    private fun hostReader(c: Conn) {
        try {
            val r = BufferedReader(InputStreamReader(c.socket.getInputStream(), Charsets.UTF_8))
            while (true) {
                val line = r.readLine() ?: break
                val m = runCatching { json.decodeFromString(Wire.serializer(), line) }.getOrNull() ?: continue
                when (m.t) {
                    "hello" -> {
                        _state.update { it.copy(players = it.players.filter { p -> p.id != c.id } + Peer(c.id, m.name?.take(20).orEmpty().ifBlank { "Player ${c.id}" }, m.game.orEmpty(), m.gameCode.orEmpty())) }
                        send(c, Wire("welcome", you = c.id))
                        broadcastRoster()
                    }
                    "ready" -> { setReady(c.id, m.ready == true); broadcastRoster() }
                    "chat" -> addChat(Peer(c.id, "", "", "").let { _state.value.players.firstOrNull { p -> p.id == c.id }?.name ?: "?" }, m.text.orEmpty().take(200), broadcast = true)
                    "bye" -> break
                }
            }
        } catch (_: Exception) {
        } finally {
            conns -= c
            runCatching { c.socket.close() }
            _state.update { it.copy(players = it.players.filter { p -> p.id != c.id }) }
            broadcastRoster()
        }
    }

    private fun setReady(id: Int, ready: Boolean) = _state.update { s -> s.copy(players = s.players.map { if (it.id == id) it.copy(ready = ready) else it }) }

    private fun broadcastRoster() {
        val players = _state.value.players
        for (c in conns) send(c, Wire("roster", players = players))
    }

    private fun addChat(from: String, text: String, broadcast: Boolean) {
        if (text.isBlank()) return
        _state.update { it.copy(chat = (it.chat + ChatLine(from, text, clock())).takeLast(80)) }
        if (broadcast) for (c in conns) send(c, Wire("chat", name = from, text = text))
    }

    // ---- friend ----

    suspend fun join(code: String, name: String, game: String, gameCode: String) = withContext(Dispatchers.IO) {
        close()
        val addr = RoomCode.decode(code)
        if (addr == null) {
            _state.value = RoomState(phase = RoomPhase.ERROR, message = "That doesn't look like a room code.")
            return@withContext
        }
        _state.value = RoomState(phase = RoomPhase.OPENING, code = code.uppercase())
        try {
            val s = Socket()
            s.connect(InetSocketAddress(InetAddress.getByName(addr.first), addr.second), 6000)
            val conn = Conn(0, s, BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)))
            clientConn = conn
            send(conn, Wire("hello", name = name.ifBlank { "Player" }, game = game, gameCode = gameCode))
            job = scope.launch { clientReader(conn, code.uppercase()) }
        } catch (e: Exception) {
            _state.value = RoomState(phase = RoomPhase.ERROR, message = "Couldn't reach that room. Are you on the same network (or VPN) as the host?")
        }
    }

    private fun clientReader(c: Conn, code: String) {
        try {
            val r = BufferedReader(InputStreamReader(c.socket.getInputStream(), Charsets.UTF_8))
            while (true) {
                val line = r.readLine() ?: break
                val m = runCatching { json.decodeFromString(Wire.serializer(), line) }.getOrNull() ?: continue
                when (m.t) {
                    "welcome" -> _state.update { it.copy(phase = RoomPhase.LOBBY, code = code, you = m.you ?: 0) }
                    "roster" -> _state.update { it.copy(players = m.players.orEmpty()) }
                    "chat" -> addChat(m.name.orEmpty(), m.text.orEmpty(), broadcast = false)
                    "start" -> _state.update { it.copy(phase = RoomPhase.STARTING) }
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { c.socket.close() }
            if (_state.value.phase != RoomPhase.IDLE) _state.update { it.copy(phase = RoomPhase.CLOSED, message = "The host closed the room.") }
        }
    }

    // ---- shared actions ----

    fun setReady(ready: Boolean) {
        val s = _state.value
        if (s.isHost) { setReady(s.you, ready); broadcastRoster() }
        else clientConn?.let { send(it, Wire("ready", ready = ready)) }
    }

    fun say(text: String) {
        val s = _state.value
        val me = s.players.firstOrNull { it.id == s.you }?.name ?: return
        if (s.isHost) addChat(me, text, broadcast = true) else clientConn?.let { send(it, Wire("chat", text = text)) }
    }

    /** Host only: tells everyone to start. */
    fun start() {
        if (!_state.value.isHost || !_state.value.everyoneReady) return
        _state.update { it.copy(phase = RoomPhase.STARTING) }
        for (c in conns) send(c, Wire("start"))
    }

    private fun send(c: Conn, m: Wire) {
        scope.launch {
            runCatching {
                synchronized(c.out) {
                    c.out.write(json.encodeToString(Wire.serializer(), m))
                    c.out.write("\n")
                    c.out.flush()
                }
            }
        }
    }

    fun close() {
        clientConn?.let { runCatching { send(it, Wire("bye")); it.socket.close() } }
        clientConn = null
        conns.forEach { runCatching { it.socket.close() } }
        conns.clear()
        runCatching { server?.close() }
        server = null
        job?.cancel()
        job = null
        nextId = 1
        _state.value = RoomState()
    }

    fun shutdown() {
        close()
        scope.cancel()
    }

    private fun localIpv4(): ByteArray? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<java.net.Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress || !it.isLinkLocalAddress }
            ?.address
    }.getOrNull()
}

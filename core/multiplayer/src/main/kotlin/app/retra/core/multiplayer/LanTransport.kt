package app.retra.core.multiplayer

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * A bounded length-prefixed transport intended for trusted local networks.
 * Internet play must use a TLS relay implementation; Retra never silently
 * exposes this cleartext LAN transport to the public internet.
 */
class MultiplayerSocketConnection private constructor(
    private val socket: Socket
) : AutoCloseable {
    private val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
    private val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
    private val writeLock = Any()

    val remoteAddress: String get() = socket.remoteSocketAddress.toString()

    fun send(packet: MultiplayerPacket) {
        val encoded = MultiplayerPacketCodec.encode(packet)
        require(encoded.size <= MAX_FRAME_BYTES) { "Multiplayer frame exceeds the transport limit." }
        synchronized(writeLock) {
            output.writeInt(encoded.size)
            output.write(encoded)
            output.flush()
        }
    }

    fun receive(): MultiplayerPacket {
        val length = input.readInt()
        require(length in MIN_FRAME_BYTES..MAX_FRAME_BYTES) { "Invalid multiplayer frame length." }
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return MultiplayerPacketCodec.decode(bytes)
    }

    override fun close() {
        runCatching { socket.shutdownInput() }
        runCatching { socket.shutdownOutput() }
        runCatching { socket.close() }
    }

    companion object {
        private const val MIN_FRAME_BYTES = 27
        private const val MAX_FRAME_BYTES = 8 * 1024

        fun connect(
            host: String,
            port: Int,
            connectTimeoutMillis: Int = 8_000,
            readTimeoutMillis: Int = 10_000
        ): MultiplayerSocketConnection {
            require(host.isNotBlank())
            require(port in 1..65_535)
            require(connectTimeoutMillis in 250..60_000)
            require(readTimeoutMillis in 250..60_000)
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = readTimeoutMillis
            socket.connect(InetSocketAddress(host, port), connectTimeoutMillis)
            return MultiplayerSocketConnection(socket)
        }

        internal fun accepted(socket: Socket, readTimeoutMillis: Int): MultiplayerSocketConnection {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = readTimeoutMillis
            return MultiplayerSocketConnection(socket)
        }
    }
}

class MultiplayerLanHost(
    port: Int = 0,
    bindAddress: InetAddress = InetAddress.getByName("127.0.0.1"),
    private val acceptTimeoutMillis: Int = 10_000,
    private val readTimeoutMillis: Int = 10_000
) : AutoCloseable {
    private val server = ServerSocket()

    init {
        require(port in 0..65_535)
        require(acceptTimeoutMillis in 250..60_000)
        require(readTimeoutMillis in 250..60_000)
        require(bindAddress.isLoopbackAddress || bindAddress.isSiteLocalAddress || bindAddress.isLinkLocalAddress) {
            "LAN host must bind to a loopback, site-local, or link-local address."
        }
        server.reuseAddress = true
        server.soTimeout = acceptTimeoutMillis
        server.bind(InetSocketAddress(bindAddress, port), 4)
    }

    val localPort: Int get() = server.localPort
    val localAddress: InetAddress get() = server.inetAddress

    fun accept(): MultiplayerSocketConnection = MultiplayerSocketConnection.accepted(server.accept(), readTimeoutMillis)

    override fun close() {
        runCatching { server.close() }
    }
}

interface InternetRelayTransport {
    /** Opens a TLS-authenticated relay room. Implementations require real service credentials. */
    fun connect(roomCode: String, authenticationToken: String): MultiplayerSocketConnection
}

package app.retra.data

import app.retra.core.AtomicFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.coroutineContext

class DownloadException(message: String) : IOException(message)

/** HTTPS-only downloads with progress, cancellation, and size + SHA-256 verification. */
class Downloader(private val tempDir: File, private val io: CoroutineDispatcher = Dispatchers.IO) {

    suspend fun fetchText(url: String, maxBytes: Int = 2 * 1024 * 1024): String = withContext(io) {
        val conn = open(url)
        try {
            if (conn.responseCode !in 200..299) throw DownloadException("Server answered ${conn.responseCode}")
            conn.inputStream.use { input ->
                val bytes = input.readNBytesCompat(maxBytes + 1)
                if (bytes.size > maxBytes) throw DownloadException("Response too large")
                String(bytes, Charsets.UTF_8)
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Downloads [download] and returns the verified payload bytes (the extracted `archiveEntry`
     * for zips). [onProgress] receives 0..1.
     */
    suspend fun download(download: CatalogDownload, key: String, onProgress: (Float) -> Unit): ByteArray = withContext(io) {
        tempDir.mkdirs()
        val part = File(tempDir, "$key.part")
        val digest = MessageDigest.getInstance("SHA-256")
        val conn = open(download.url)
        try {
            if (conn.responseCode !in 200..299) throw DownloadException("The download server answered ${conn.responseCode}. Try again later.")
            val total = download.size.takeIf { it > 0 } ?: conn.contentLengthLong
            var done = 0L
            part.outputStream().buffered().use { out ->
                conn.inputStream.use { input ->
                    val buf = ByteArray(64 * 1024)
                    var lastReport = -1
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        done += n
                        if (done > download.size + 1024 * 1024) throw DownloadException("The file is bigger than expected, so Retra stopped the download.")
                        digest.update(buf, 0, n)
                        out.write(buf, 0, n)
                        val pct = if (total > 0) ((done * 1000) / total).toInt() else -1
                        if (pct != lastReport) {
                            lastReport = pct
                            onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (done != download.size) throw DownloadException("The download was incomplete. Try again.")
            if (hex(digest.digest()) != download.sha256.lowercase()) throw DownloadException("The file didn't pass the safety check (SHA-256 mismatch), so it was discarded.")
            val bytes = part.readBytes()
            if (download.format == "zip") extractEntry(bytes, download) else bytes
        } finally {
            conn.disconnect()
            part.delete()
        }
    }

    private fun extractEntry(zip: ByteArray, d: CatalogDownload): ByteArray {
        val want = d.archiveEntry ?: throw DownloadException("The archive entry is missing from the catalog.")
        ZipInputStream(ByteArrayInputStream(zip)).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                if (e.isDirectory) continue
                if (e.name == want || e.name.substringAfterLast('/') == want) {
                    val bytes = zin.readNBytesCompat(((d.archiveEntrySize ?: (256L * 1024 * 1024)) + 1).toInt())
                    if (d.archiveEntrySize != null && bytes.size.toLong() != d.archiveEntrySize) throw DownloadException("The file inside the zip has an unexpected size.")
                    if (d.archiveEntrySha256 != null && hex(MessageDigest.getInstance("SHA-256").digest(bytes)) != d.archiveEntrySha256.lowercase()) {
                        throw DownloadException("The file inside the zip didn't pass the safety check.")
                    }
                    return bytes
                }
            }
        }
        throw DownloadException("Couldn't find $want inside the zip.")
    }

    private fun open(url: String): HttpURLConnection {
        if (!url.startsWith("https://")) throw DownloadException("Retra only downloads over HTTPS.")
        var current = URL(url)
        repeat(6) {
            val c = current.openConnection() as HttpsURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", "Retra-Android")
            val code = c.responseCode
            if (code in 300..399) {
                val loc = c.getHeaderField("Location") ?: throw DownloadException("Bad redirect")
                c.disconnect()
                current = URL(current, loc)
                if (current.protocol != "https") throw DownloadException("Retra only downloads over HTTPS.")
            } else {
                return c
            }
        }
        throw DownloadException("Too many redirects")
    }

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
}

/** InputStream.readNBytes is API 33+; this works everywhere. */
internal fun java.io.InputStream.readNBytesCompat(max: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    var total = 0
    while (total < max) {
        val n = read(buf, 0, minOf(buf.size, max - total))
        if (n < 0) break
        out.write(buf, 0, n)
        total += n
    }
    return out.toByteArray()
}

@Suppress("unused")
private fun keepAtomic() = AtomicFiles

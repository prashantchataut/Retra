package app.retra.core.download

import app.retra.core.model.CatalogEntry
import java.net.InetAddress
import java.net.URI

class UnsafeDownloadException(message: String) : IllegalArgumentException(message)

data class DownloadResponseMetadata(
    val contentType: String?,
    val contentLength: Long,
    val contentEncoding: String?
)

object CatalogDownloadPolicy {
    const val MAX_DOWNLOAD_BYTES: Long = 64L * 1024L * 1024L
    const val MAX_REDIRECTS: Int = 5
    private val hashPattern = Regex("[0-9a-fA-F]{64}")
    private val idPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")
    private val allowedContentTypes = setOf(
        "application/octet-stream",
        "binary/octet-stream",
        "application/x-gba-rom",
        "application/vnd.gba-rom"
    )

    fun validateEntry(entry: CatalogEntry): URI {
        if (!entry.id.matches(idPattern)) {
            throw UnsafeDownloadException("Catalog entry ID must use 1-80 safe ASCII characters.")
        }
        val uri = parseHttps(entry.downloadUrl, "Catalog download URL")
        if (!entry.sha256.matches(hashPattern)) throw UnsafeDownloadException("Catalog entry SHA-256 is invalid.")
        if (entry.fileSize !in 1..MAX_DOWNLOAD_BYTES) throw UnsafeDownloadException("Catalog entry size is outside Retra's 64 MiB limit.")
        if (entry.license.isBlank()) throw UnsafeDownloadException("Catalog entry license is missing.")
        if (entry.distributionPermission.isBlank()) throw UnsafeDownloadException("Catalog entry distribution permission is missing.")
        if (!uri.path.lowercase().endsWith(".gba")) throw UnsafeDownloadException("Catalog game URL must identify a .gba file.")
        return uri
    }

    fun validateRedirect(origin: URI, current: URI, location: String, visited: Set<URI>): URI {
        val next = runCatching { current.resolve(location) }
            .getOrElse { throw UnsafeDownloadException("Download redirect is invalid.") }
        val secure = parseHttps(next.toString(), "Redirect URL")
        if (!origin.host.equals(secure.host, ignoreCase = true)) {
            throw UnsafeDownloadException("Cross-host download redirect was blocked.")
        }
        if (secure in visited) throw UnsafeDownloadException("Download redirect loop detected.")
        return secure
    }

    fun validateResponse(entry: CatalogEntry, metadata: DownloadResponseMetadata) {
        if (metadata.contentLength > MAX_DOWNLOAD_BYTES) throw UnsafeDownloadException("Server response exceeds Retra's 64 MiB limit.")
        if (metadata.contentLength >= 0 && metadata.contentLength != entry.fileSize) {
            throw UnsafeDownloadException("Server Content-Length does not match the signed catalog metadata.")
        }
        val encoding = metadata.contentEncoding?.trim()?.lowercase()
        if (!encoding.isNullOrEmpty() && encoding != "identity") {
            throw UnsafeDownloadException("Compressed transfer encoding is not accepted for ROM downloads.")
        }
        val type = metadata.contentType?.substringBefore(';')?.trim()?.lowercase()
        if (!type.isNullOrEmpty() && type !in allowedContentTypes) {
            throw UnsafeDownloadException("Unexpected download content type: $type.")
        }
    }

    fun validateCompletedSize(entry: CatalogEntry, actualBytes: Long) {
        if (actualBytes != entry.fileSize) {
            throw UnsafeDownloadException("Downloaded size does not match the catalog metadata.")
        }
        if (actualBytes !in 1..MAX_DOWNLOAD_BYTES) {
            throw UnsafeDownloadException("Downloaded file is outside Retra's 64 MiB limit.")
        }
    }

    private fun parseHttps(value: String, label: String): URI {
        val uri = runCatching { URI(value) }.getOrElse { throw UnsafeDownloadException("$label is malformed.") }
        if (!uri.scheme.equals("https", ignoreCase = true)) throw UnsafeDownloadException("$label must use HTTPS.")
        if (uri.host.isNullOrBlank()) throw UnsafeDownloadException("$label has no host.")
        if (isBlockedHost(uri.host)) throw UnsafeDownloadException("$label targets a local or private host.")
        if (uri.userInfo != null) throw UnsafeDownloadException("$label must not contain user information.")
        if (uri.fragment != null) throw UnsafeDownloadException("$label must not contain a fragment.")
        return uri.normalize()
    }

    private fun isBlockedHost(hostValue: String): Boolean {
        val host = hostValue.trim().trim('[', ']').lowercase()
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".internal") || host.endsWith(".lan") || host.endsWith(".home.arpa") ||
            !host.contains('.') && !host.contains(':')
        ) {
            return true
        }
        val looksLikeIpLiteral = host.contains(':') || host.matches(Regex("[0-9.]+"))
        if (!looksLikeIpLiteral) return false
        val address = runCatching { InetAddress.getByName(host) }.getOrElse { return true }
        return address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
    }
}

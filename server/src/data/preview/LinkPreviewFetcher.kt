package com.usuallycoding.opensecretsanta.server.data.preview

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Result of a bounded, SSRF-checked HTTP GET. */
data class FetchedResource(
    val finalUri: URI,
    val contentType: String?,
    val bytes: ByteArray,
)

/** Raw HTTP response from a single non-redirecting hop. */
data class RawHttpResponse(
    val statusCode: Int,
    val location: String?,
    val contentType: String?,
    val contentLength: Long?,
    val body: InputStream,
)

/** Minimal single-hop HTTP transport so tests can exercise redirect, SSRF, and size-cap logic without sockets. */
fun interface HttpTransport {
    fun get(uri: URI, accept: String, userAgent: String, timeout: Duration): RawHttpResponse?
}

/** Rejects loopback, private, link-local, CGNAT, multicast, and unspecified IP addresses. */
object SafeAddressValidator {
    fun isSafePublicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }
        return when (address) {
            is Inet4Address -> isSafeIpv4(address.address)
            is Inet6Address -> isSafeIpv6(address.address)
            else -> false
        }
    }

    private fun isSafeIpv4(b: ByteArray): Boolean {
        if (b.size != 4) return false
        val b0 = b[0].toInt() and 0xFF
        val b1 = b[1].toInt() and 0xFF
        val b2 = b[2].toInt() and 0xFF
        return when {
            b0 == 0 -> false // 0.0.0.0/8
            b0 == 10 -> false // 10.0.0.0/8
            b0 == 100 && b1 in 64..127 -> false // 100.64.0.0/10 (CGNAT)
            b0 == 127 -> false // 127.0.0.0/8
            b0 == 169 && b1 == 254 -> false // 169.254.0.0/16
            b0 == 172 && b1 in 16..31 -> false // 172.16.0.0/12
            b0 == 192 && b1 == 0 && b2 == 0 -> false // 192.0.0.0/24
            b0 == 192 && b1 == 0 && b2 == 2 -> false // 192.0.2.0/24
            b0 == 192 && b1 == 168 -> false // 192.168.0.0/16
            b0 == 198 && b1 in 18..19 -> false // 198.18.0.0/15
            b0 == 198 && b1 == 51 && b2 == 100 -> false // 198.51.100.0/24
            b0 == 203 && b1 == 0 && b2 == 113 -> false // 203.0.113.0/24
            b0 >= 224 -> false // multicast & reserved
            else -> true
        }
    }

    private fun isSafeIpv6(b: ByteArray): Boolean {
        if (b.size != 16) return false
        val b0 = b[0].toInt() and 0xFF
        val b1 = b[1].toInt() and 0xFF
        if ((b0 and 0xFE) == 0xFC) return false // fc00::/7
        if (b0 == 0xFE && (b1 and 0xC0) == 0x80) return false // fe80::/10
        if (b0 == 0x20 && b1 == 0x01 && (b[2].toInt() and 0xFF) == 0x0D && (b[3].toInt() and 0xFF) == 0xB8) return false
        val isIpv4Mapped = (0..9).all { b[it] == 0.toByte() } &&
            (b[10].toInt() and 0xFF) == 0xFF &&
            (b[11].toInt() and 0xFF) == 0xFF
        if (isIpv4Mapped) {
            return isSafeIpv4(b.copyOfRange(12, 16))
        }
        return true
    }
}

/**
 * Fetches external web pages and preview images with SSRF protection, manual redirect validation,
 * strict timeouts, and byte limits.
 */
class SafeHttpFetcher(
    private val transport: HttpTransport = jdkTransport(),
    private val resolveHost: (String) -> List<InetAddress> = { InetAddress.getAllByName(it).toList() },
    private val addressAllowed: (InetAddress) -> Boolean = SafeAddressValidator::isSafePublicAddress,
    private val requestTimeout: Duration = Duration.ofSeconds(10),
    private val maxRedirects: Int = 5,
) {
    fun fetchHtml(url: String, maxBytes: Int = MAX_HTML_BYTES): FetchedResource? =
        fetch(url, accept = "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1", maxBytes = maxBytes, truncateAtLimit = true)

    fun fetchImage(url: String, maxBytes: Int = MAX_IMAGE_BYTES): FetchedResource? =
        fetch(url, accept = "image/jpeg,image/png,image/webp,image/gif,*/*;q=0.1", maxBytes = maxBytes, truncateAtLimit = false)

    private fun fetch(url: String, accept: String, maxBytes: Int, truncateAtLimit: Boolean): FetchedResource? {
        var current = parseHttpUri(url) ?: return null
        repeat(maxRedirects + 1) { hop ->
            if (!isAllowedHost(current)) return null
            val response = transport.get(current, accept, USER_AGENT, requestTimeout) ?: return null
            val status = response.statusCode
            if (status in REDIRECT_CODES) {
                response.body.close()
                if (hop >= maxRedirects) return null
                val location = response.location ?: return null
                current = resolveRedirect(current, location) ?: return null
                return@repeat
            }
            response.body.use { stream ->
                if (status !in 200..299) return null
                val contentLength = response.contentLength ?: -1L
                if (!truncateAtLimit && contentLength > maxBytes) return null
                val bytes = readBounded(stream, maxBytes, truncateAtLimit) ?: return null
                return FetchedResource(finalUri = current, contentType = response.contentType, bytes = bytes)
            }
        }
        return null
    }

    private fun isAllowedHost(uri: URI): Boolean {
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return false
        val addresses = try {
            resolveHost(host)
        } catch (_: Exception) {
            return false
        }
        return addresses.isNotEmpty() && addresses.all(addressAllowed)
    }

    private fun parseHttpUri(raw: String): URI? {
        val uri = try {
            URI(raw.trim())
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return uri
    }

    private fun resolveRedirect(base: URI, location: String): URI? {
        val resolved = try {
            base.resolve(location.trim())
        } catch (_: Exception) {
            return null
        }
        val scheme = resolved.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (resolved.host.isNullOrBlank()) return null
        return resolved
    }

    private fun readBounded(stream: InputStream, maxBytes: Int, truncateAtLimit: Boolean): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            val remaining = maxBytes - total
            if (read > remaining) {
                if (truncateAtLimit) {
                    if (remaining > 0) out.write(buffer, 0, remaining)
                    break
                }
                return null
            }
            out.write(buffer, 0, read)
            total += read
        }
        return out.toByteArray()
    }

    companion object {
        const val MAX_HTML_BYTES = 512 * 1024
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        private const val USER_AGENT = "OpenSecretSanta-LinkPreview/1.0"
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

        fun jdkTransport(
            http: HttpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build(),
        ): HttpTransport = HttpTransport { uri, accept, userAgent, timeout ->
            val request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(timeout)
                .header("User-Agent", userAgent)
                .header("Accept", accept)
                .build()
            val response = try {
                http.send(request, HttpResponse.BodyHandlers.ofInputStream())
            } catch (_: Exception) {
                return@HttpTransport null
            }
            RawHttpResponse(
                statusCode = response.statusCode(),
                location = response.headers().firstValue("Location").orElse(null),
                contentType = response.headers().firstValue("Content-Type").orElse(null),
                contentLength = response.headers().firstValueAsLong("Content-Length").let { if (it.isPresent) it.asLong else null },
                body = response.body(),
            )
        }
    }
}

package com.usuallycoding.opensecretsanta.server.data.preview

import java.net.URI
import java.nio.charset.Charset

/** Metadata extracted from an HTML document for link preview display. */
data class ParsedMetadata(
    val title: String,
    val siteName: String?,
    val imageUrl: String?,
)

/**
 * Lightweight HTML head/meta parser extracting Open Graph, Twitter Card, and `<title>` metadata
 * without requiring a full DOM parser.
 */
object HtmlMetadataParser {
    private const val MAX_TITLE_LENGTH = 160
    private const val MAX_SITE_NAME_LENGTH = 80

    private val META_TAG = Regex("""<meta\b[^>]*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ATTR = Regex("""([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+))""")
    private val TITLE_TAG = Regex("""<title\b[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val CHARSET_PARAM = Regex("""charset\s*=\s*["']?([a-zA-Z0-9._-]+)["']?""", RegexOption.IGNORE_CASE)
    private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);""")
    private val WHITESPACE = Regex("""\s+""")

    fun parse(bytes: ByteArray, contentType: String?, finalUri: URI): ParsedMetadata? {
        if (contentType != null && !isHtmlContentType(contentType)) return null
        val charset = extractCharset(contentType) ?: Charsets.UTF_8
        val html = bytes.toString(charset)

        var ogTitle: String? = null
        var twitterTitle: String? = null
        var ogSiteName: String? = null
        var ogImage: String? = null
        var twitterImage: String? = null

        for (match in META_TAG.findAll(html)) {
            val attrs = parseAttributes(match.value)
            val content = attrs["content"]?.takeIf { it.isNotBlank() } ?: continue
            val key = (attrs["property"] ?: attrs["name"])?.lowercase() ?: continue
            when (key) {
                "og:title" -> if (ogTitle == null) ogTitle = content
                "twitter:title" -> if (twitterTitle == null) twitterTitle = content
                "og:site_name" -> if (ogSiteName == null) ogSiteName = content
                "og:image", "og:image:secure_url" -> if (ogImage == null) ogImage = content
                "twitter:image", "twitter:image:src" -> if (twitterImage == null) twitterImage = content
            }
        }

        val htmlTitle = TITLE_TAG.find(html)?.groupValues?.getOrNull(1)
        val rawTitle = ogTitle ?: twitterTitle ?: htmlTitle ?: return null
        val cleanTitle = cleanText(rawTitle, MAX_TITLE_LENGTH) ?: return null

        val hostFallback = finalUri.host?.lowercase()?.removePrefix("www.")?.takeIf { it.isNotBlank() }
        val cleanSiteName = cleanText(ogSiteName, MAX_SITE_NAME_LENGTH) ?: hostFallback

        val rawImage = ogImage ?: twitterImage
        val resolvedImage = rawImage?.let { resolveImageUrl(finalUri, decodeEntities(it).trim()) }

        return ParsedMetadata(
            title = cleanTitle,
            siteName = cleanSiteName,
            imageUrl = resolvedImage,
        )
    }

    private fun isHtmlContentType(contentType: String): Boolean {
        val mime = contentType.substringBefore(';').trim().lowercase()
        return mime == "text/html" || mime == "application/xhtml+xml"
    }

    private fun extractCharset(contentType: String?): Charset? {
        val name = contentType?.let { CHARSET_PARAM.find(it)?.groupValues?.getOrNull(1) } ?: return null
        return try {
            Charset.forName(name)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAttributes(tag: String): Map<String, String> = buildMap {
        for (m in ATTR.findAll(tag)) {
            val name = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } }
            put(name, value)
        }
    }

    private fun cleanText(raw: String?, maxLength: Int): String? {
        if (raw == null) return null
        val decoded = decodeEntities(raw).replace(WHITESPACE, " ").trim()
        if (decoded.isEmpty()) return null
        return if (decoded.length <= maxLength) decoded else decoded.take(maxLength).trimEnd()
    }

    private fun resolveImageUrl(base: URI, raw: String): String? {
        if (raw.isEmpty()) return null
        val resolved = try {
            base.resolve(raw)
        } catch (_: Exception) {
            return null
        }
        val scheme = resolved.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (resolved.host.isNullOrBlank()) return null
        return resolved.toString()
    }

    internal fun decodeEntities(input: String): String =
        ENTITY.replace(input) { m ->
            val token = m.groupValues[1]
            when {
                token.startsWith("#x", ignoreCase = true) -> {
                    val code = token.substring(2).toIntOrNull(16)
                    if (code != null && Character.isValidCodePoint(code)) String(Character.toChars(code)) else m.value
                }
                token.startsWith("#") -> {
                    val code = token.substring(1).toIntOrNull(10)
                    if (code != null && Character.isValidCodePoint(code)) String(Character.toChars(code)) else m.value
                }
                else -> when (token.lowercase()) {
                    "amp" -> "&"
                    "lt" -> "<"
                    "gt" -> ">"
                    "quot" -> "\""
                    "apos" -> "'"
                    "nbsp" -> " "
                    else -> m.value
                }
            }
        }
}

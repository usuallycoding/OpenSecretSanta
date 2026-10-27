package com.usuallycoding.opensecretsanta.data

/**
 * Whether a server address points at a private network rather than the public internet: this
 * machine, a home or office network, or a name that only a local network can resolve. Android 17
 * wants the local-network permission before the app talks to such an address.
 */
fun isLocalNetworkAddress(baseUrl: String): Boolean {
    val host = hostOf(baseUrl) ?: return false
    if (host == "localhost") return true
    ipv4Octets(host)?.let { (a, b) ->
        return a == 10 || a == 127 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
    }
    if (':' in host) return host == "::1" || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")
    return host.endsWith(".local") || '.' !in host
}

/** The lower-case host of an absolute URL: no scheme, user-info, port, path, query or IPv6 brackets. */
private fun hostOf(url: String): String? {
    val separator = url.indexOf("://")
    if (separator <= 0) return null
    val authority = url.substring(separator + 3).takeWhile { it != '/' && it != '?' && it != '#' }.substringAfterLast('@')
    val host = if (authority.startsWith("[")) {
        authority.substringAfter('[').substringBefore(']')
    } else {
        authority.substringBefore(':')
    }
    return host.lowercase().takeIf { it.isNotEmpty() }
}

private fun ipv4Octets(host: String): Pair<Int, Int>? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    val numbers = parts.map { it.toIntOrNull()?.takeIf { n -> n in 0..255 } ?: return null }
    return numbers[0] to numbers[1]
}

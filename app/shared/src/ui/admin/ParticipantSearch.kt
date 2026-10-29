package com.usuallycoding.opensecretsanta.ui.admin

private val WHITESPACE = Regex("\\s+")

/**
 * Whether a person matches what the owner typed: every word of [query] (split at spaces, ignoring case) is found in the
 * username or the full name, in any order. An empty or blank query matches everyone.
 */
fun matchesSearch(query: String, username: String, fullName: String?): Boolean {
    val words = query.lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    // a line break between the two, so that a word never matches across the end of one and the start of the other
    val text = (username + "\n" + fullName.orEmpty()).lowercase()
    return words.all { it in text }
}

/** The participants that match [query], in the order they came in. */
fun filterParticipants(participants: List<AdminParticipant>, query: String): List<AdminParticipant> =
    if (query.isBlank()) participants else participants.filter { matchesSearch(query, it.username, it.fullName) }

/** The pairs whose Santa or whose receiver matches [query], in the order they came in. */
fun filterPairs(pairs: List<AdminPair>, query: String): List<AdminPair> =
    if (query.isBlank()) pairs else pairs.filter { matchesSearch(query, it.santa, it.santaFullName) || matchesSearch(query, it.receiver, it.receiverFullName) }

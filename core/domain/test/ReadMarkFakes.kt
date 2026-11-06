package com.usuallycoding.opensecretsanta.domain

import com.usuallycoding.opensecretsanta.domain.gift.ReadKind
import com.usuallycoding.opensecretsanta.domain.gift.ReadMarkRepository
import com.usuallycoding.opensecretsanta.domain.user.Username

/** In-memory read marks that keep the later time, as the real repository does. */
class InMemoryReadMarks : ReadMarkRepository {
    private val saved = mutableMapOf<Triple<Long, Username, ReadKind>, Long>()

    override fun seenUpTo(groupId: Long, santa: Username): Map<ReadKind, Long> =
        saved.filterKeys { it.first == groupId && it.second == santa }.mapKeys { it.key.third }

    override fun markSeen(groupId: Long, santa: Username, kind: ReadKind, atMillis: Long): Long {
        val key = Triple(groupId, santa, kind)
        val later = maxOf(saved[key] ?: atMillis, atMillis)
        saved[key] = later
        return later
    }
}

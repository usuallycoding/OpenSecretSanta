package com.usuallycoding.opensecretsanta.domain.group

/** A group only ever moves one step forward through these states, in this order. */
enum class GroupState {
    DRAFT,
    PENDING,
    ACTIVE,
    ARCHIVED,
    ;

    /** The only state this one may move to, or null when it is the last. */
    fun next(): GroupState? = entries.getOrNull(ordinal + 1)

    fun canMoveTo(target: GroupState): Boolean = next() == target

    /** Participants may still change while the group is being prepared. */
    val allowsParticipantChanges: Boolean get() = this == DRAFT || this == PENDING

    val allowsDetailChanges: Boolean get() = this != ARCHIVED

    /** The lowercase name used when the state is stored. */
    val storedName: String get() = name.lowercase()

    companion object {
        fun fromStored(value: String): GroupState =
            entries.firstOrNull { it.storedName == value } ?: error("Unknown group state '$value'")
    }
}

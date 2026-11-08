package com.usuallycoding.opensecretsanta.ui.readmarks

import com.usuallycoding.opensecretsanta.contract.ReadMarkDto
import com.usuallycoding.opensecretsanta.contract.ReadMarksDto
import com.usuallycoding.opensecretsanta.data.api.ApiClient
import com.usuallycoding.opensecretsanta.data.api.ApiResult
import com.usuallycoding.opensecretsanta.data.api.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** How a wishlist item differs from what the Santa has seen. */
enum class Change { New, Edited }

/**
 * What the Santa had seen of one kind when a page was opened. A baseline that is not [known] (the server could not be asked) marks
 * nothing, so that a failure never makes everything look new. Known with no time, the Santa has never looked: everything is new.
 */
data class SeenBaseline(val known: Boolean, val seenUpTo: Instant?)

internal fun parseTime(raw: String?): Instant? = raw?.let { try { Instant.parse(it.trim()) } catch (_: Exception) { null } }

/** An item created after the baseline is new, one created before it and changed after it is edited. Times are compared as instants. */
internal fun itemChange(createdAt: String, updatedAt: String, baseline: SeenBaseline?): Change? {
    if (baseline == null || !baseline.known) return null
    val seen = baseline.seenUpTo ?: return Change.New
    val created = parseTime(createdAt) ?: return null
    val updated = parseTime(updatedAt) ?: created
    return when {
        created > seen -> Change.New
        updated > seen -> Change.Edited
        else -> null
    }
}

/** An answer given or changed after the baseline is new; an unanswered question is not. */
internal fun answerIsNew(answerUpdatedAt: String?, baseline: SeenBaseline?): Boolean {
    if (baseline == null || !baseline.known || answerUpdatedAt == null) return false
    val seen = baseline.seenUpTo ?: return true
    val updated = parseTime(answerUpdatedAt) ?: return false
    return updated > seen
}

/** A letter sent after the baseline is new. */
internal fun letterIsNew(sentAt: String, baseline: SeenBaseline?): Boolean {
    if (baseline == null || !baseline.known) return false
    val seen = baseline.seenUpTo ?: return true
    val sent = parseTime(sentAt) ?: return false
    return sent > seen
}

/** The newest of the [times], as the text it came as, or null when there is none that can be read. */
internal fun newestOf(times: List<String?>): String? =
    times.filterNotNull().mapNotNull { raw -> parseTime(raw)?.let { it to raw } }.maxByOrNull { it.first }?.second

/** The marks of one kind in what the server answered. */
internal fun ReadMarksDto.of(kind: String): ReadMarkDto = when (kind) {
    KIND_WISHLIST -> wishlist
    KIND_ANSWERS -> answers
    else -> letters
}

const val KIND_WISHLIST = "wishlist"
const val KIND_ANSWERS = "answers"
const val KIND_LETTERS = "letters"

/**
 * Keeps one page's baseline for the whole visit and marks what the page showed as seen. The baseline is asked for once, when the page
 * first loads, and not again, so a refresh keeps the marks and a new arrival is marked too; the next visit makes a new tracker and so
 * a new baseline. Marking sends the newest time shown, never twice for the same time, and does nothing while the baseline is not
 * known. A failed mark changes nothing: the marks come back next time.
 */
internal class ReadTracker(
    private val scope: CoroutineScope,
    private val readBaseline: suspend () -> ApiResult<ReadMarkDto>,
    private val mark: suspend (String) -> Unit,
) {
    /** The tracker of one kind of a Santa's receiver's things. */
    constructor(api: ApiClient, groupId: Long, scope: CoroutineScope, kind: String) : this(
        scope,
        { api.readMarks(groupId).map { it.of(kind) } },
        { time -> api.markRead(groupId, kind, time) },
    )

    var baseline: SeenBaseline? = null
        private set
    private var latest: String? = null
    private var marked: String? = null

    /** Asks the server for the baseline unless it is already known. */
    suspend fun ensureBaseline() {
        if (baseline?.known == true) return
        baseline = when (val result = readBaseline()) {
            is ApiResult.Success -> SeenBaseline(known = true, seenUpTo = parseTime(result.value.seenUpTo))
            is ApiResult.Failure -> SeenBaseline(known = false, seenUpTo = null)
        }
    }

    /** What the page now shows: its newest time, or null for an empty list. */
    fun shown(newest: String?) {
        latest = newest
    }

    /** Tells the server what has been shown; returns the running work, or null when there is nothing to send. */
    fun markSeen(): Job? {
        val time = latest ?: return null
        if (baseline?.known != true || time == marked) return null
        marked = time
        return scope.launch { mark(time) }
    }
}

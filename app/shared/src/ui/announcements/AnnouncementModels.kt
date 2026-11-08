package com.usuallycoding.opensecretsanta.ui.announcements

import com.usuallycoding.opensecretsanta.contract.AnnouncementDto
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant
import com.usuallycoding.opensecretsanta.ui.readmarks.SeenBaseline
import com.usuallycoding.opensecretsanta.ui.readmarks.letterIsNew

/** One announcement as the screens show it. [sent] is the time it was sent, ready to read. */
data class AnnouncementItem(
    val number: Int,
    val title: String,
    val text: String,
    val sent: String,
    /** Sent since the person last looked, when there are marks to compare with. */
    val isNew: Boolean = false,
)

internal fun AnnouncementDto.toItem(baseline: SeenBaseline? = null) = AnnouncementItem(number, title, body, formatInstant(sentAt), letterIsNew(sentAt, baseline))

/** The server lists announcements in the order they were sent; the screens show the last one sent first. */
internal fun List<AnnouncementDto>.newestFirst(baseline: SeenBaseline? = null): List<AnnouncementItem> =
    map { it.toItem(baseline) }.sortedByDescending { it.number }

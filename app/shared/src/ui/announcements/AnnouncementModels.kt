package com.usuallycoding.opensecretsanta.ui.announcements

import com.usuallycoding.opensecretsanta.contract.AnnouncementDto
import com.usuallycoding.opensecretsanta.ui.groups.formatInstant

/** One announcement as the screens show it. [sent] is the time it was sent, ready to read. */
data class AnnouncementItem(
    val number: Int,
    val title: String,
    val text: String,
    val sent: String,
)

internal fun AnnouncementDto.toItem() = AnnouncementItem(number, title, body, formatInstant(sentAt))

/** The server lists announcements in the order they were sent; the screens show the last one sent first. */
internal fun List<AnnouncementDto>.newestFirst(): List<AnnouncementItem> = map { it.toItem() }.sortedByDescending { it.number }

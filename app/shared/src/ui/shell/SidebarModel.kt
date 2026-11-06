package com.usuallycoding.opensecretsanta.ui.shell

import com.usuallycoding.opensecretsanta.ui.Strings
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase
import com.usuallycoding.opensecretsanta.ui.navigation.GifterRoute
import com.usuallycoding.opensecretsanta.ui.navigation.GroupRoute
import com.usuallycoding.opensecretsanta.ui.navigation.InboxRoute
import com.usuallycoding.opensecretsanta.ui.navigation.ReceiverRoute
import com.usuallycoding.opensecretsanta.ui.navigation.SendLetterRoute
import com.usuallycoding.opensecretsanta.ui.readmarks.UnreadCounts

/** One entry of the sidebar's tree: the [route] it opens, and what is waiting behind it ([count], said aloud as [spoken]). */
internal data class SidebarItem(val label: String, val route: Any, val count: Int = 0, val spoken: String? = null)

/**
 * What an expanded group holds, by what its [phase] has to offer: its Overview, Giving (the receiver's wishlist, the questions for them
 * and the Inbox, side by side on one page) and Receiving (the person's own wishlist, Respond to Secret Santa and the Outbox, the same).
 * Giving carries what is new to the Santa and Receiving the questions that wait for an answer, as the group page's cards badge them.
 */
internal fun groupItems(groupId: Long, phase: GroupPhase, unread: UnreadCounts?, unanswered: Int): List<SidebarItem> {
    val items = mutableListOf(SidebarItem(Strings.SIDEBAR_OVERVIEW, GroupRoute(groupId)))
    val drawn = phase == GroupPhase.Active || phase == GroupPhase.Archived
    if (drawn) {
        val new = unread?.let { it.wishlist + it.answers + it.letters } ?: 0
        items += SidebarItem(Strings.SIDEBAR_GIVING, GifterRoute(groupId), new, Strings.withNew(Strings.SIDEBAR_GIVING, new))
    }
    if (drawn || phase == GroupPhase.Pending) {
        items += SidebarItem(Strings.SIDEBAR_RECEIVING, ReceiverRoute(groupId), unanswered, Strings.withUnanswered(Strings.SIDEBAR_RECEIVING, unanswered))
    }
    return items
}

/** Whether [current], the screen that is open, belongs to this item: the pages of Giving and of Receiving each hold more than one route. */
internal fun SidebarItem.holds(current: Any?): Boolean = when (route) {
    is GroupRoute -> current is GroupRoute
    is GifterRoute -> current is GifterRoute || current is InboxRoute
    is ReceiverRoute -> current is ReceiverRoute || current is SendLetterRoute
    else -> current == route
}

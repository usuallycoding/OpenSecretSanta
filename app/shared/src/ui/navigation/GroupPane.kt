package com.usuallycoding.opensecretsanta.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.usuallycoding.opensecretsanta.ui.groups.GroupPhase

/**
 * What can open beside a group's page on a wide window, and be a page of its own on a small screen: the announcements, and the receiver's
 * wishlist, the person's questions to them, the Inbox, the person's own wishlist, the questions from their Santa and the Outbox. Each has a
 * route, and so a web address, of its own.
 */
enum class GroupPane {
    Announcements,
    GifterWishlist,
    GifterQuestions,
    Inbox,
    ReceiverWishlist,
    ReceiverQuestions,
    Outbox,
    ;

    /** What follows `#/groups/{id}/` in this pane's web address. */
    val segment: String
        get() = when (this) {
            Announcements -> "announcements"
            GifterWishlist -> "giver/wishlist"
            GifterQuestions -> "giver/questions"
            Inbox -> "inbox"
            ReceiverWishlist -> "receiver/wishlist"
            ReceiverQuestions -> "receiver/questions"
            Outbox -> "outbox"
        }

    /** The route of this pane for [groupId]. */
    fun route(groupId: Long): Any = when (this) {
        Announcements -> AnnouncementsRoute(groupId)
        GifterWishlist -> GifterWishlistRoute(groupId)
        GifterQuestions -> GifterQuestionsRoute(groupId)
        Inbox -> InboxRoute(groupId)
        ReceiverWishlist -> ReceiverWishlistRoute(groupId)
        ReceiverQuestions -> ReceiverQuestionsRoute(groupId)
        Outbox -> SendLetterRoute(groupId)
    }

    /**
     * What this pane is in a group in [phase]: itself, another pane that stands in for it, or null for none, which is the group's own
     * page. Announcements can be read from a Pending group on. Before the names are drawn there is no receiver, no question and no letter:
     * the person's own wishlist is all there is, and the questions from the Santa show it.
     */
    fun within(phase: GroupPhase): GroupPane? = when (phase) {
        GroupPhase.Pending -> when (this) {
            Announcements, ReceiverWishlist -> this
            ReceiverQuestions -> ReceiverWishlist
            GifterWishlist, GifterQuestions, Inbox, Outbox -> null
        }
        GroupPhase.Active, GroupPhase.Archived -> this
        GroupPhase.Other -> null
    }
}

/** The pane a route opens, or null for a route that is not one of a group page's panes. */
fun paneOf(route: Any): GroupPane? = when (route) {
    is AnnouncementsRoute -> GroupPane.Announcements
    is GifterWishlistRoute -> GroupPane.GifterWishlist
    is GifterQuestionsRoute -> GroupPane.GifterQuestions
    is InboxRoute -> GroupPane.Inbox
    is ReceiverWishlistRoute -> GroupPane.ReceiverWishlist
    is ReceiverQuestionsRoute -> GroupPane.ReceiverQuestions
    is SendLetterRoute -> GroupPane.Outbox
    else -> null
}

/** The group a pane route is about, or null for a route that is not a pane's. */
fun groupIdOfPane(route: Any): Long? = when (route) {
    is AnnouncementsRoute -> route.groupId
    is GifterWishlistRoute -> route.groupId
    is GifterQuestionsRoute -> route.groupId
    is InboxRoute -> route.groupId
    is ReceiverWishlistRoute -> route.groupId
    is ReceiverQuestionsRoute -> route.groupId
    is SendLetterRoute -> route.groupId
    else -> null
}

/** Whether the Giving page, which has the receiver's wishlist, the questions and the Inbox side by side, has anything in a group in [phase]. */
fun givingWithin(phase: GroupPhase): Boolean = phase == GroupPhase.Active || phase == GroupPhase.Archived

/** The pane whose web address ends in [segment] (`giver/wishlist`, `inbox` and so on), or null for a segment that is none's. */
fun paneOfSegment(segment: String): GroupPane? = GroupPane.entries.firstOrNull { it.segment == segment }

/*
 * The two functions below are the only places that list the routes of the panes for a back stack entry or a destination: the back
 * arrow, the title, the sidebar's selection, the open group and the address bar all ask them, so that a pane's route cannot be missed
 * by one list and not by another. A new pane is added to the enum, to [paneOf] and [groupIdOfPane], and to these two.
 */

/** Whether a screen is the route of one of a group page's panes. */
fun NavDestination.isGroupPane(): Boolean =
    hasRoute<AnnouncementsRoute>() || hasRoute<GifterWishlistRoute>() || hasRoute<GifterQuestionsRoute>() || hasRoute<InboxRoute>() ||
        hasRoute<ReceiverWishlistRoute>() || hasRoute<ReceiverQuestionsRoute>() || hasRoute<SendLetterRoute>()

/** The route, with its arguments, of a back stack entry that is one of the panes', or null for any other entry. */
fun NavBackStackEntry.paneRoute(): Any? = when {
    destination.hasRoute<AnnouncementsRoute>() -> toRoute<AnnouncementsRoute>()
    destination.hasRoute<GifterWishlistRoute>() -> toRoute<GifterWishlistRoute>()
    destination.hasRoute<GifterQuestionsRoute>() -> toRoute<GifterQuestionsRoute>()
    destination.hasRoute<InboxRoute>() -> toRoute<InboxRoute>()
    destination.hasRoute<ReceiverWishlistRoute>() -> toRoute<ReceiverWishlistRoute>()
    destination.hasRoute<ReceiverQuestionsRoute>() -> toRoute<ReceiverQuestionsRoute>()
    destination.hasRoute<SendLetterRoute>() -> toRoute<SendLetterRoute>()
    else -> null
}

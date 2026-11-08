# Design

## Context

`GifterRoute` and `ReceiverRoute` each carry a `questions` flag that picks a tab. `GifterPage` and `ReceiverPage` (in `GroupPages.kt`) draw the two tabs with `TabbedPages` on a small screen and the sections side by side with `WideColumns` on a wide window; the Inbox and the Outbox are `InboxRoute` and `SendLetterRoute` pages on a small screen and a third column on a wide one. The group page's callbacks (`onOpenGifter(questions)`, `onOpenReceiver(questions)`, `onOpenInbox`, `onSendLetter`) navigate to those, and on a wide window to the three-column pages. See proposal.md for why.

What already exists and can be reused:
- The sections the columns draw are standalone: `ReceiverWishlistSection`, `ReceiverQuestionsSection` (with `AskQuestionFab`), `InboxContent`, `WishlistSection` (with `WishlistFab`), `SantaQuestionsSection` and `SendLetterContent`. Several own their lazy page, and their view models are keyed per group (`receiver-wishlist-`, `receiver-`, `inbox-`, `wishlist-`, `santa-questions-`, `send-letter-`), so a page, a pane and a column share what is loaded.
- `WideColumns` already draws a heading row with the section's own button: it gives each column its own `PageActionHost`, and `AppFab` registers its action there on a wide window instead of drawing a floating button.
- The announcements already open beside the group page by their route: the icon navigates, `GroupEntry` draws the pane when the route says so, closing navigates to `GroupRoute` with `popUpTo`, and an address or a notification gives the same screen.
- `PageTitle` sets the top bar title from a screen, which is how the receiver's username reaches the title.

The announcements work showed what goes wrong when a route is added: each of `belowGroups`, `WebAddresses.addressOf(entry)`, `openGroupId`, `sidebarRoute`, `titleOf` and the spread set keeps its own list, and a missed one gave no back arrow and a blanked address.

## Goals / Non-Goals

**Goals:**
- Six pages and six panes from one set of routes, with the addresses, notification targets and link targets of both platforms.
- Remove the tabs, and make sure a new route cannot be missed by one of the route lists.

**Non-Goals:**
- Any change to what a page shows or does, to badges, marks, the three-column Giving and Receiving pages, the admin screens or the server.
- The intent filter of the Android manifest, which matches only the scheme and the host of the web app, never the part after the `#`: which addresses open is decided by `LinkTarget` alone.

## Decisions

**Six routes, and the old two lose their flag.** `GifterWishlistRoute(groupId)`, `GifterQuestionsRoute(groupId)`, `ReceiverWishlistRoute(groupId)` and `ReceiverQuestionsRoute(groupId)` join the existing `InboxRoute` and `SendLetterRoute`, with the addresses `#/groups/{id}/giver/wishlist`, `giver/questions`, `receiver/wishlist`, `receiver/questions`, `inbox` and `outbox`. `GifterRoute(groupId)` and `ReceiverRoute(groupId)` stay at `giver` and `receiver`, lose `questions`, and `PageTab`, `TabbedPages` and the `questions` suffix go. Together with `AnnouncementsRoute` they are the seven routes of a group page's pane, which an enum `GroupPane` (Announcements, GifterWishlist, GifterQuestions, Inbox, ReceiverWishlist, ReceiverQuestions, Outbox) names, with `route(groupId)` and `paneOf(destination)`.

**One mapping instead of six lists.** `belowGroups`, `openGroupId`, `sidebarRoute`, `addressOf(entry)` and the title all ask the same function for a destination's pane and group, so that a pane route is covered once; the spread set and `titleOf` use it too. The sidebar maps every pane route to its group's Overview, as it does for announcements, which also lets `SidebarItem.holds` forget the Inbox and the Outbox.

**A pane's content is one composable, in a page or in the pane.** `GroupPaneContent(pane, groupId)` builds a pane from the keyed view models and the existing sections: the receiver's wishlist and the Inbox read the receiver's name from `ReceiverViewModel` or `InboxViewModel`; the questions page is the questions section with the ask button; the person's own wishlist is `WishlistSection` with its add button, in the `PullPage` it needs for the drag to scroll the page; the Outbox is `SendLetterContent`. On a small screen a route's composable shows it as the page with the title from `paneTitle(pane, receiver)` and the back arrow; on a wide window `GroupEntry` shows the group page and, beside it, a pane frame around the same content. `GroupPage` therefore takes `pane: GroupPane?`, `onOpenPane` and `onClosePane` in place of the announcements-only parameters, and the bell and the six buttons call `onOpenPane`.

**Page and pane are equally wide.** With a pane open the group page loses its readable-width cap and the page and the pane each take `weight(1f)` of the row, on the participant group page and on the owner's screen; `PANE_WIDTH` goes. With no pane the page stays centered at `STANDARD_CONTENT_WIDTH`.

**The pane frame is the column heading, with a close icon.** `WideColumns`' heading row (title, the section's button, from its own `PageActionHost`) is pulled out as one composable and used by the columns and by a `PaneFrame` that adds the close icon and has the same heading for the announcements pane. A pane gives its content a fresh `PageActionHost`, so the add, ask and send buttons become heading buttons and `fabClearance()` is 0, as in a column.

**Opening replaces, closing goes to the group.** Every button navigates to its pane's route with `popUpTo<GroupRoute>` and `launchSingleTop`, so the back stack is the group list, the group and the one pane, whatever was open; the same button again changes nothing. Closing navigates to `GroupRoute` and pops the pane's route, as the announcements pane does, which also covers a pane an address opened with no group behind it. Alternative rejected: stacking panes. It makes Back walk through every page that was looked at and makes the close icon ambiguous.

**What a state allows is decided once.** A function `GroupPane.within(phase)` says what a pane or page becomes in a group's phase: in a Pending group `ReceiverQuestions` becomes `ReceiverWishlist` and `GifterWishlist`, `GifterQuestions`, `Inbox` and `Outbox` become none (the group page), and the Giving page (`GifterRoute`) becomes none; in Active and Archived every pane stays. A route's composable reads the group's phase from the group's own view model, shows progress until it is known, and, when the result differs from the route, navigates to the result's route (or to `GroupRoute`) replacing itself, on a small screen and on a wide window alike. The group page does not offer the buttons that lead to the pages a phase does not have, as now.

**`giver` and `receiver` are the three-column pages on wide, the wishlist on small.** `GifterRoute` and `ReceiverRoute` keep their wide composables (`GifterPage` and `ReceiverPage` without the tab code). On a small screen they navigate, replacing themselves, to `GifterWishlistRoute` and `ReceiverWishlistRoute`, so an old bookmark, a link and an old notification still land on the old default tab's content.

**Notifications and links use the routes.** `NotificationTarget.routesOf` returns `[GroupRoute, X]` with X the pane route of the category: `questions` the receiver's questions, `answers` the giver's questions, `wishlist` the giver's wishlist and `letters` the Inbox, and on a wide window X is the pane through the same route composable. The web push service worker's `addressOf` changes only `wishlist`, from `/giver` to `/giver/wishlist`. `LinkTarget` opens `[GroupRoute, X]` for the six addresses, the Announcements address and `giver` and `receiver`, and still nothing for the delivery info, settings or admin addresses.

## Risks / Trade-offs

- [The group page is composed afresh when a pane opens, so it starts at the top] → As for the announcements; the buttons are in the two cards near the top.
- [On windows only a little wider than the 840dp breakpoint the group page is squeezed to half the width beside the pane] → The two panes share the width equally by request; accepted, and checked on a device.
- [A phase redirect could loop or flash] → The rule is a pure function with a test, a redirect only leads to a route that the same rule leaves alone, and the page shows progress, not content, until the phase is known.
- [Replacing a page's route changes what Back does for a link to `receiver/questions` in a Pending group] → It lands on the wishlist with the group page behind it, which is the right place.
- [The first Giver page needs the receiver's name for its title, an extra request for the wishlist and the Inbox pages] → The view model is keyed and shared, and the title shows the fallback until it arrives.
- [A missed route in a list gives a missing back arrow or a blank address] → One mapping function, and a test that every pane's route has an address, a title and a place in the sidebar mapping.

## Migration Plan

Nothing is stored or sent differently, so the change ships with the app. Addresses `giver/questions`, `receiver/questions`, `giver`, `receiver`, `inbox` and `outbox` keep working; `giver` and `receiver` open the wishlist on a small screen as the old default tab did, and `receiver/wishlist` and `giver/wishlist`, which named nothing before, now open pages. A browser that still has the old service worker keeps the old `wishlist` address `/giver` until it updates, which opens the receiver's wishlist on a small screen and the Giving page on a wide window.

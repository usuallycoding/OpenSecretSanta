# Tasks

## 1. Routes, addresses and the one mapping

- [x] 1.1 Add the four routes (`GifterWishlistRoute`, `GifterQuestionsRoute`, `ReceiverWishlistRoute`, `ReceiverQuestionsRoute`), drop the `questions` flag and `PageTab` from `GifterRoute` and `ReceiverRoute`, add the enum `GroupPane` (the six pages and the announcements) with its route, and a pure `GroupPane.within(phase)` for what a Pending group has (`receiver/questions` becomes the wishlist, the giver pages, the Inbox and the Outbox become the group page, and the Giving page too); verify a test covers `within` for every pane and phase and the build of `shared` succeeds
- [x] 1.2 Give each pane route its address in `WebAddresses` (`giver/wishlist`, `giver/questions`, `receiver/wishlist`, `receiver/questions`, with `inbox`, `outbox` and `announcements` as they are), parse them, keep `giver` and `receiver`, and make `addressOf(entry)`, `belowGroups`, `openGroupId`, `sidebarRoute` and the title ask one destination-to-pane function, with every pane route mapped to its group's Overview in the sidebar and `SidebarItem.holds` simplified; extend `WebAddressesTest` (every address round trips, a trailing slash, the old `questions` addresses, `receiver/wishlist` naming a page now, an admin address for a non-administrator) and add a test that every pane has an address and a title; verify the shared tests pass

## 2. Notification and link paths

- [x] 2.1 Make `NotificationTarget.routesOf` return `[GroupRoute, pane route]` (`questions` to the receiver's questions, `answers` to the giver's questions, `wishlist` to the giver's wishlist, `letters` to the Inbox, announcements as they are) and `LinkTarget` open `[GroupRoute, route]` for the six addresses, the Announcements address and `giver` and `receiver`, and nothing for delivery info, settings or admin; extend `NotificationTargetTest` and `LinkTargetTest`, including that the web addresses the service worker builds open the same routes; verify the tests pass
- [x] 2.2 Change the wishlist case of `addressOf` in `app/webApp/resources/firebase-messaging-sw.js` from `/giver` to `/giver/wishlist` (and its comment); verify by test or by reading that the four addresses it builds are the ones `NotificationTargetTest` names

## 3. The pages on a small screen

- [x] 3.1 Add `GroupPaneContent(pane, groupId)` from the existing sections and keyed view models and `paneTitle(pane, receiver)` from the existing strings, and show it as a page for each pane route on a small screen, titled and with the back arrow, reading the receiver's name from the keyed `ReceiverViewModel` or `InboxViewModel` for the Giver pages; verify the build succeeds and the titles, including the fallbacks before the receiver is known, are covered by a test of `paneTitle`
- [x] 3.2 In each pane route's composable read the group's phase from its view model, show progress until it is known and replace the route with `within(phase)`'s route or the group page when it differs; on a small screen make `GifterRoute` and `ReceiverRoute` replace themselves with the receiver's wishlist and the person's wishlist; verify with tests of the redirect decision, and a manual check later, that a Pending group's addresses land where `client-giver-receiver-pages` says
- [x] 3.3 Remove `TabbedPages`, `PageTab`, the tab labels that nothing uses and the `questions` arguments from the group page's callbacks, leave `GifterPage` and `ReceiverPage` as the wide three-column pages, and make the group page's six buttons navigate to the pane routes (on a small screen the page); verify `grep -rn "PageTab\|TabbedPages" app` finds nothing and the shared tests pass

## 4. The right pane on a wide window

- [x] 4.1 Pull the heading row of `WideColumns` out as one composable, add `PaneFrame` (that heading plus a close icon, a fresh `PageActionHost` for its content) and use it for the announcements pane too; verify the build succeeds and the three-column pages are unchanged
- [x] 4.2 Make `GroupPage` and `GroupEntry` take `pane: GroupPane?`, `onOpenPane` and `onClosePane` in place of the announcements parameters, draw `PaneFrame { GroupPaneContent }` beside the group page for the pane the route names, and make each pane route's composable on a wide window show `GroupEntry` with its pane; opening navigates with `popUpTo<GroupRoute>` and `launchSingleTop`, closing navigates to `GroupRoute` popping the pane's route; verify the build succeeds and the pure navigation decisions (what a button opens, what closing goes to) have tests
- [x] 4.3 Add the pane routes to the spread set so that the group page and its pane use the width, keep the top bar title of the group on a wide window and the page's title on a small one, and keep Overview selected in the sidebar for every pane; verify the sidebar and title mappings with tests and the build with `./kotlin build`

## 5. Verify

- [x] 5.1 Run the full build and all tests (`./kotlin build` and `./kotlin test`) and verify they pass
- [x] 5.2 On a phone and on a wide window, with a Pending, an Active and an Archived group: open each of the six pages from the group page and see the page (phone) or the pane with its heading button (wide) and the address follow; replace one pane with another and with the bell, go back and close; open each of the six addresses by typing, refreshing, an Android link and a tapped notification (and a web push) and see the page or the pane with the group page behind it; check `giver` and `receiver` give the wishlist on the phone and the three-column pages on the wide window, the sidebar's Giving and Receiving still open them, Pending groups land on the wishlist or the group page, and the badges and marks still clear; note anything that looks off in the change

## Workflow follow-up

- Archive the change after review.

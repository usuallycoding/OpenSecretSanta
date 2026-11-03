# Design

## Context

The push data message already holds `category` (`announcements`, `wishlist`, `questions`, `answers`) and `groupId`. The Android tap puts only the group id in the intent, and `MainActivity` does one `navigate(GroupRoute(id))`. The web service worker builds `#/groups/{id}` from the group id alone. `GifterRoute` and `ReceiverRoute` carry only the group, and the Wishlist and Questions tabs are internal pager state. See proposal.md for the behaviour and the delta specs for the requirements.

## Goals / Non-Goals

**Goals:**
- One mapping from a notification to the screens it opens, tested without a device.
- Pages that open on a requested tab, reachable by a web address.

**Non-Goals:**
- No change to the server, the push payload or what notifications say.
- No notifications on iOS, and no change to the Notifications sheet.
- No address that tracks the tab as the person switches it.

## Decisions

- **The tab is a route argument, and it is a Boolean.** `GifterRoute` and `ReceiverRoute` get `questions`, false by default, so every existing `GifterRoute(id)` and its address stay as they are; a `tab` property turns it into the `PageTab` the pager uses. It is not an enum field: Navigation on the web and iOS cannot carry an enum in a route and fails when the screens are built, right after sign-in (Android tolerates it, so only a test of the route's argument types shows it, and `RoutesTest` does). The pager's initial page comes from it. Alternative: a one-shot event the page consumes. Rejected: a route argument survives process death and web reloads, and is what the web address needs.
- **The address names the tab only as the opening tab.** `#/groups/2/receiver/questions` and `#/groups/2/gifter/questions` are parsed and produced; the Wishlist default has no suffix, and `#/.../wishlist` stays an address that names no screen. Swiping a tab does not update the address: the address follows screens, not tabs, so the shell's tracking of screens does not change. A reload after switching tabs reopens on the tab in the address, which is accepted.
- **The category-to-screens mapping is a pure function in `app/shared`.** It takes the category and group id and returns the routes to open in order: announcements give the group; questions give the group then `ReceiverRoute(id, Questions)`; answers give the group then `GifterRoute(id, Questions)`; wishlist gives the group then `GifterRoute(id)`. An unknown or missing category gives the group. Android uses it directly. Alternative: map in `MainActivity`. Rejected: it could not be tested and would drift from the web.
- **Android builds the stack in order.** The tap's intent carries the category beside the group id. The activity opens the Groups root the way a view switch does (nothing of an older stack behind it), then pushes each route of the mapping, so Back goes page, group, list from any starting state, including when the app is already on another group.
- **On the web the worker tells the page where to go; it does not navigate it.** `WindowClient.navigate()` only works on a page the worker controls, and a page opened before the worker was installed is not one, so the click did nothing. The worker posts the address to the open page, which sets its own address (the path the app already follows for a typed address); with no page open it opens a window on the address. A new worker version takes over at once (`skipWaiting` and `claim`), and a notification the page shows itself (the app was open when it arrived) carries the category as well.
- **The service worker keeps its own copy of the mapping** because it is a plain script that cannot call the app's code: category to `#/groups/{id}`, `.../receiver/questions`, `.../gifter/questions` or `.../gifter`. The app's tests cover that each of those addresses parses to the intended route, so a mismatch shows up as a failing address, not a silent wrong page.
- **A page opened on a tab it does not have shows what it has.** The Receiver page of a Pending group has no tabs and shows the wishlist, ignoring the hint. In an Archived group the questions tab is read-only but present.
- **Unavailable groups use the screens' own message.** The group and pages already show "no longer available" with a way back for a 404; the tap does not add a check of its own.

## Risks / Trade-offs

- [The category copies in the service worker and in the app can drift] → Both map to the same four addresses, and `WebAddressesTest` pins them; the mapping function's test names the same four cases.
- [Opening on the second tab skips the first tab's load] → The pager's `onShown` reloads a tab when it is shown, and the initial tab is the one composed first; checked by opening each target cold.
- [A reload after switching tabs reopens on the tab in the address] → Accepted: the address is an opening hint only.

## Migration Plan

None: no data or API change, and old addresses and notifications keep working because the tab defaults to Wishlist and a missing category opens the group. Rollback is reverting the app, and the service worker with it.

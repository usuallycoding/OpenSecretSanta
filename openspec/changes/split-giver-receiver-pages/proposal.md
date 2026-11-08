# Proposal

## Why

On a small screen the Giver and Receiver pages each pack a wishlist and a list of questions into two tabs, so a person cannot send someone a link to just the questions, and the web address names a page, not what is on it (`/receiver/wishlist` names nothing). On a wide window every button on the group page jumps to a three-column page and away from the group, while announcements already open beside it with their own address. Splitting the tabs into pages of their own, and opening each of them beside the group page on a wide window, makes every screen of a group addressable and consistent.

## What Changes

- **Six pages, no tabs, on a small screen.** The wishlist and the questions of the Giver and Receiver pages, and the Inbox and the Outbox, are each a page with its own title and back arrow: `giver/wishlist`, `giver/questions`, `inbox`, `receiver/wishlist`, `receiver/questions` and `outbox`. The tabs, the swiping and the tab row are gone.
- **The same six open in the right pane on a wide window.** Pressing See Wishlist, Ask Questions, Inbox, Edit Wishlist, Respond to Santa or Send letter on the group page opens that page in a pane beside the group page, as announcements do, and the web address follows (`#/groups/2/giver/wishlist` while it is open, `#/groups/2` once it is closed). Opening another pane, or the announcements, replaces the open one, so Back and the close button always return to the group page. The pane's heading carries the page's own button (Add item, Ask a question, Send letter).
- **An address opens the same on a wide window.** Typing, refreshing or tapping any of the six addresses on a wide window shows the group page with that page in the pane.
- **`/giver` and `/receiver` stay.** On a wide window they are the Giving and Receiving pages with all three sections side by side, which the sidebar's Giving and Receiving items open and which nothing else opens any more. On a small screen they open the wishlist page, the old default tab, so old links keep working.
- **Pending groups.** Only the wishlist exists: `receiver/questions` opens the wishlist, and the giver pages, the inbox and the outbox open the group page. There is no receiver, no questions and no letters before the names are drawn.
- **Notifications and links.** Android's taps open the right page (a question goes to `receiver/questions`, an answer to `giver/questions`, a wishlist change to `giver/wishlist`, a letter to `inbox`), and so does the web push service worker, whose only changed address is the wishlist's, from `/giver` to `/giver/wishlist`. On a wide window a tap opens the group page with the pane. All six addresses, and the Announcements address, also open in the Android app from a link.
- The sidebar's items, the badges, the pills and bars, and what each page shows and does are not changed.

## Capabilities

### New Capabilities

- `client-giver-receiver-pages`: the six pages on a small screen, their titles, addresses and what a Pending group has, and the old Giver and Receiver page statements carrying over to the page, pane and column that show the same things.
- `client-group-panes`: the right pane of the group page on a wide window for the six pages and the announcements, what opens, replaces and closes it, its heading and the address that follows it.

### Modified Capabilities

- `client-group-pages`: the tab requirements of the Giver and Receiver pages are removed and the pages and their loading are described without tabs.
- `client-app`: the readable web addresses name the six pages, and the questions-tab address requirement is removed.
- `client-layout`: the Giving and Receiving pages keep their three columns but are opened only from the sidebar, and the group page's buttons open panes.
- `client-notifications`: tapping a notification opens the page the notification is about, and the pane on a wide window.
- `client-app-links`: the six addresses open in the Android app from a link.
- `client-read-marks`: the marks and what clears them are described for the pages instead of tabs.

## Impact

- `app/shared/src/ui`: routes and web addresses (`Routes.kt`, `WebAddresses.kt`), the pages and the tab code (`GroupPages.kt`, `PageTab`, `TabbedPages`), the group page and its entries and pane (`GroupPage.kt`, `ShellScreen.kt`, `Sidebar.kt`), notification and link targets (`NotificationTarget.kt`, `LinkTarget.kt`), strings and tests.
- `app/webApp/resources/firebase-messaging-sw.js`: the address the web push service worker builds for a wishlist change.
- `app/androidApp`: only through `NotificationTarget`, which it already calls.
- No server, API or data change.

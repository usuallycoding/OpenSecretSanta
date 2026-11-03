# Tasks

## 1. Routes and addresses

- [x] 1.1 Add a `questions` flag (false by default; a Boolean, not an enum, which web and iOS navigation cannot carry) to `GifterRoute` and `ReceiverRoute` in `app/shared/src/ui/navigation/Routes.kt`, and make `WebAddresses.kt` produce and read `#/groups/2/gifter/questions` and `#/groups/2/receiver/questions`, keeping the Wishlist addresses as they are and `.../wishlist` naming no screen; extend `app/shared/test/ui/WebAddressesTest.kt` with both addresses round-tripping and the old ones unchanged, and verify with `./kotlin test -m shared`

## 2. Pages

- [x] 2.1 Start `TabbedPages` (`app/shared/src/ui/groups/GroupPages.kt`) on the requested tab and pass the route's tab from `ShellScreen.kt` to `GifterPage` and `ReceiverPage`; a Pending group's Receiver page ignores it; verify by opening each page cold on the questions tab, and on the wishlist tab, and that the other tab still loads when swiped to

## 3. What a notification opens

- [x] 3.1 Add a pure mapping in `app/shared` from a notification's category and group id to the routes to open in order (announcements: the group; questions: the group, then the Receiver page on questions; answers: the group, then the Gifter page on questions; wishlist: the group, then the Gifter page; unknown or missing category: the group), with tests for the four categories, an unknown category and a missing one; verify with `./kotlin test -m shared`

## 4. Android

- [x] 4.1 Put the notification's category in the tap intent beside the group id (`NotificationsAndroid.kt`) and make `MainActivity.kt` open the Groups root and then each route of the mapping, for a cold start and for a tap while the app is open; verify on a device or emulator that each of the four notifications opens its page and tab, that Back goes page, group, list, and that a tap while on another group leaves no stale screens behind

## 5. Web

- [x] 5.1 Make the click handler in `app/webApp/resources/firebase-messaging-sw.js` choose the address from the category (`#/groups/{id}`, `#/groups/{id}/receiver/questions`, `#/groups/{id}/gifter/questions`, `#/groups/{id}/gifter`), falling back to the group for an unknown category; verify in a browser with each of the four notifications, with the app open and with it closed

## 6. Specs and integration

- [x] 6.1 Run `openspec validate open-notifications-on-their-page --strict` and verify it passes, then run `./kotlin test -m shared`, `./kotlin test -p jvm` and `./kotlin build` and verify they are green

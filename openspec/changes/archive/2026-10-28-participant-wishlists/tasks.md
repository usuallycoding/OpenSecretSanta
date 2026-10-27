# Tasks

## 1. Calls and building blocks

- [x] 1.1 Add the six wishlist calls to `ApiClient` (`wishlist`, `addWishlistItem`, `updateWishlistItem`, `deleteWishlistItem`, `reorderWishlist`, `receiverWishlist`) and extend `ScriptedServer` with the wishlist routes and scripted failures; verify common tests for each call on every target: the list and receiver list, a 201 add, an edit, a 204 delete (it must parse as an empty answer), the reorder body (all ids in the new order), a 400 naming a field, a 400 naming `itemIds`, a 404, a 409, and the password never appearing in any body
- [x] 1.2 Give `Loader` a `replace(value)` and `GroupDetail` its `phase` (`Pending`, `Active`, `Archived`, `Other`); verify common tests that a replaced value shows with loading off and no error, that a refresh afterwards still works, and that each state string maps to its phase (an unknown one to `Other`) in the group detail view model

## 2. View models

- [x] 2.1 Implement `WishlistViewModel` loading the list and the group together (editable by phase, an unknown group reported as gone) and register it in Koin; verify common tests for the order, an empty list, editable in Pending and Active, read-only in Archived, unreachable then retry, and a vanished group
- [x] 2.2 Add the add and edit flow (the sheet state, `WishlistEntry.of` validation naming the field, POST and PUT through the server's answer, the 50-item limit, errors kept in the sheet); verify common tests for an add appearing last, an edit keeping its place, a cleared note and link, an empty title, a title over 200 characters, a note over 1000, a link that is not `http` or `https` (`javascript:`, `ftp:`), a server field error keeping the sheet open, a network failure keeping the edits, dismissing discarding them, and "Add" unavailable at 50
- [x] 2.3 Add delete and reorder with the concurrency outcomes (confirmation then DELETE, up and down, the ends, buttons locked while saving, 400 `itemIds`, 409 as archived and as full decided from the reloaded phase, 404 on an item, network failure notices); verify common tests for each of them, that confirming removes only that item, that cancelling changes nothing and that every notice can be dismissed
- [x] 2.4 Implement `ReceiverWishlistViewModel` and register both new view models in Koin; verify common tests for the receiver's items in order, an empty list, unreachable then retry, and that it exposes nothing that changes the list

## 3. Screens

- [x] 3.1 Add `WishlistRoute` and `ReceiverWishlistRoute`, the Wishlists section on the group detail (My wishlist always, the receiver's entry for Active and Archived, the Pending note), the back arrow and titles for the new routes; verify on the Android emulator with a Pending, an Active and an Archived group that the right entries show, that each opens a screen and that back returns to the group
- [x] 3.2 Implement the My wishlist screen (rows with title, note, link, up and down buttons, the empty state, the closed note when Archived, refresh and retry, notices); verify on the Android emulator: items in order, move up and down and the disabled ends, an Archived group with no controls, the server stopped then "Try again", and a link opening in the browser
- [x] 3.3 Implement the bottom sheet for adding and editing, the delete confirmation dialog and the field errors; verify on the Android emulator: add, edit, clear note and link, an empty title, a non-web link, a very long note, delete with confirm and cancel, closing the sheet with the back button, the sheet staying open when the server is stopped, and the "list is full" case at 50 items (seed 50 through the API)
- [x] 3.4 Implement the Your receiver's wishlist screen (read-only rows, the empty state, refresh, a link opening); verify on the Android emulator with an Active and an Archived group: the receiver's items in the receiver's order after the receiver reorders them on the server, no editing controls, an empty list, and that nothing about the person's reading changes any response the receiver gets

## 4. Platforms and integration

- [x] 4.1 Document the wishlist screens in the README and extend the Koin graph test for the new view models; verify the web in headless Chrome: opening My wishlist and going back with the browser's back button, the sheet opening with its keyboard and saving, a link opening in a new tab, and that the address bar follows `#WishlistRoute/<id>`
- [x] 4.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator and the iOS simulator or device: add, edit, reorder and delete items as one participant, read them as that participant's Santa, archive the group and see the list turn read-only for both (Done on Android, web and the full build and test run. The iOS app builds with the rest of the repo, but it was not exercised by hand: no way to create a group and a second account on a device.)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

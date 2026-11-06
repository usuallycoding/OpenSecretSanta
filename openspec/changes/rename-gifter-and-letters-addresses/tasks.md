# Tasks

## 1. The addresses

- [x] 1.1 In `WebAddresses.kt`, write `giver` for the Giving page and `outbox` for the Send letter page in `addressOf` and `routeOf`, and update its comments; verify with `WebAddressesTest` that `GifterRoute(2)` is `#/groups/2/giver`, `GifterRoute(2, questions = true)` is `#/groups/2/giver/questions`, `SendLetterRoute(2)` is `#/groups/2/outbox`, and that each round-trips.
- [x] 1.2 Add `#/groups/2/gifter`, `#/groups/2/gifter/questions` and `#/groups/2/letters` to the addresses that name nothing in `WebAddressesTest`; verify the test passes.
- [x] 1.3 Update `LinkTargetTest` and `NotificationTargetTest` to the new addresses, with a `LinkTarget` case that `/groups/7/gifter` gives nothing; verify that all the tests of `shared` pass.

## 2. Links and documents

- [x] 2.1 In `firebase-messaging-sw.js` point the `answers` and `wishlist` notifications at `/giver/questions` and `/giver`; verify that `NotificationTargetTest`'s comparison of the app's routes with these addresses passes and that no `gifter` is left in the file.
- [x] 2.2 Update the address table in `README.md` (`/giver`, `/outbox`); verify that no `gifter` or `/letters` address is left in the README or the web app's files, apart from the server's `/santa/letters` and `/receiver/letters` routes.
- [x] 2.3 Check on the web build that `#/groups/{id}/giver`, `#/groups/{id}/outbox` and their refresh open the right pages and the old addresses open the first screen, then sync the delta specs and archive the change.

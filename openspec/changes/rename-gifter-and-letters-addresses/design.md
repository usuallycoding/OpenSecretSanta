# Design

## Context

See proposal.md for the motivation. `WebAddresses` maps the part of a web address after the `#` to a route and back, in `addressOf` and `routeOf`. `LinkTarget` (Android links from emails) and the web app's address bar both read it, and `NotificationTarget` gives the routes for a tapped notification. The only other place that writes an address is `firebase-messaging-sw.js`, a plain script that cannot call the Kotlin code, so it repeats the mapping for the pages a notification opens. The Android manifest's links match on the host, not on these paths.

## Goals / Non-Goals

**Goals:**
- `/giver` and `/outbox` are the only addresses for the Giving and Send letter pages, in the app, in links and in the web's notifications.

**Non-Goals:**
- No aliases for the old addresses (chosen: a clean rename).
- No rename of `GifterRoute`, `SendLetterRoute`, "Gifter page" in the specs' wording, or of any server route: the addresses are what the person sees.

## Decisions

**Change the two words in `WebAddresses` and nowhere else in the mapping.** Both directions live there, so `addressOf` and `routeOf` stay in step, and `LinkTarget` and the Android links follow without a change of their own. Dropping the old words from `routeOf` is what makes the old addresses name nothing: they fall to the existing "unknown address" case, so no new code path is needed.

**The service worker is edited by hand.** `firebase-messaging-sw.js` is not built from Kotlin, so its two lines (`answers`, `wishlist`) change to `/giver`. `NotificationTargetTest` already checks that the app's notification routes and the web's addresses agree, so its literals are updated to the new words and the test keeps the two mappings from drifting. A worker already installed in a browser is replaced at once by the new one, as its own comment says, so a notification tapped after the update opens the new address.

**Old forms are tested as unknown.** `WebAddressesTest`'s list of addresses that name nothing gains `gifter`, `gifter/questions` and `letters`, which states the clean rename as a test.

## Risks / Trade-offs

- [A notification already in the tray, a bookmark or a link already sent still carries the old address] → it opens the first screen instead of the page; accepted with the clean rename, and the group's own address, which is what emails use, is unchanged.
- [The mapping now has the new words in two places (Kotlin and the worker)] → as before; `NotificationTargetTest` compares them.

# Proposal

## Why

The web app's addresses name the Giving page `/gifter` and the page of letters the person sent `/letters`. The app and its pages now say "giver" for the person who gives and "Outbox" for the letters that go out, with "Inbox" for the ones that come in, so the addresses should say the same.

## What Changes

- The address of a group's Giving page becomes `#/groups/{id}/giver`, and `#/groups/{id}/giver/questions` opens it on its questions tab. `/gifter` goes away.
- The address of a group's page of sent letters becomes `#/groups/{id}/outbox`. `/letters` goes away. The Inbox stays `#/groups/{id}/inbox`.
- It is a clean rename: the old addresses name no screen, and so show the first screen of the person's view, as any unknown address does.
- Links the app makes itself use the new addresses: the web's push notifications and the Android app's own address mapping.
- The address of a group (`#/groups/{id}`), of the Receiver page and of every other screen does not change, and neither do the server's API routes (`/groups/{id}/santa/letters` and the rest).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: the readable web addresses name the Giver page, and the Gifter addresses are no longer known.
- `client-app-links`: the group addresses that open in the Android app name the Giver page.
- `client-letters`: the Send letter page's address is `#/groups/{id}/outbox`.

## Impact

- `app/shared/src/ui/navigation/WebAddresses.kt` (the one place that maps addresses to screens; `LinkTarget` and the Android link handling read it) and its tests, `WebAddressesTest`, `LinkTargetTest` and `NotificationTargetTest`.
- `app/webApp/resources/firebase-messaging-sw.js`, which builds the address a tapped web notification opens.
- The address table in `README.md`.
- The Kotlin names (`GifterRoute`, `SendLetterRoute`) and the pages' titles and wording do not change; neither does any server code or API.
- Bookmarks and links already sent that use `/gifter` or `/letters` stop working, which was chosen over keeping them as aliases.

# Proposal

## Why

The owner's wishlist and question sheets show when things were added, changed, asked and answered, but in UTC with a "UTC" label, two or three hours off the clock everyone in the group lives by (Bucharest). Participants do not see these times at all, so nobody can tell when their own item was added or changed, when a question was asked, or when the receiver last touched their list, although the app already receives them.

## What Changes

- Every time the app shows is in Bucharest time (`Europe/Bucharest`, with summer time) and no longer labelled UTC, for example `1 Dec 2026, 12:00`, whatever time zone the device is set to.
- Participants see the same times as the owner: on their own wishlist items ("Added …", and "Changed …" when it differs), on the receiver's wishlist on the Gifter page (the same), and on questions on both the Gifter and Receiver pages ("Asked …", "Answered …" and, when the answer was edited, "Answer changed …").
- The owner's sheets (a participant's wishlist, a pair's thread) use the same format and zone.
- No server change: the times are already sent as UTC instants.

## Capabilities

### New Capabilities

- `client-times`: the zone and format of every time the app shows, and which times participants see.

### Modified Capabilities

None. The existing specs say "when it was added or last changed" and "time asked" without a zone or format, and stay true.

## Impact

- `app/shared/src/ui/groups/GroupFormat.kt` (`formatInstant`), `OversightModels.kt`, `wishlist/WishlistModels.kt` and its view models, `gift/GiftModels.kt` and its view models, `WishlistScreen.kt`, `ReceiverWishlistScreen.kt`, `ReceiverScreen.kt` (`QuestionCard`), `OversightSheets.kt`, `Strings.kt`, tests, the README.

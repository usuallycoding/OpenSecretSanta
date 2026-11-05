# Proposal

## Why

The pair rows on a group page are small and terse: a username, an arrow and another username, and a lone gift icon while the group is Pending. Everything a person can do about their gift is behind them, but nothing says so. Two cards with labelled buttons say what each place leads to.

## What Changes

- **BREAKING (participant view):** the pair rows are removed from the group page in every state: the single row with the person's username and the gift icon while Pending, and the two rows `me -> receiver` and `Secret Santa -> me` once Active or Archived. Taking tappable usernames away means the cards below have to carry every way in.
- A **"You're Secret Santa for:"** card goes under the information card. Its next row, in a bold title font, is "TBD" while the group is Pending and the receiver's username in capitals once it is Active or Archived. Under it, in italics, is the line "Shh... Keep it a secret!". From Active on it has the buttons "See Wishlist" and "Ask Questions", which open the Gifter page on its wishlist or questions tab, and last a "Delivery Info" button (the receiver's details sheet). While Pending it has no button.
- A **"Your stuff:"** card follows, with the line "Tell your santa what you want and let them know you!" in italics under its heading. It has "Edit Wishlist" (the Receiver page on the wishlist tab), and from Active on "Respond to Santa" (the Receiver page on the "Respond to Secret Santa" tab). In an Archived group it also has "Reveal Santa", which opens the sheet that used to open from the "Secret Santa" row. Last comes "Delivery Info" (the person's own details sheet).
- The receiver's username still comes from `GET /groups/{id}/receiver`, and a failure shows its message with "Try again" in the first card.
- The pair link stays for the administrator's pair list, and the administrator's view does not change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-group-pages`: the pair rows and their layout are removed; the Secret Santa and Your stuff cards are added; the details sheets, the Receiver and Gifter pages, the receiver's username and the Santa reveal are reached from the cards' buttons.
- `client-pair-link`: the pair link is used only in the administrator's pair list, not on the participant's group page.
- `client-groups`: the group page lists the two cards where it listed the pair rows.

## Impact

- Client only: `PairRows.kt` is replaced by the two cards, `GroupPage.kt` passes the tab when it opens the Gifter and Receiver pages, and `ShellScreen.kt` carries the `questions` flag into `GifterRoute` and `ReceiverRoute`, which already have it. New strings in `Strings.kt`; the strings only the rows used are removed. No server, contract or database change.
- README: the paragraph on the group page.
- Out of scope: the administrator's view, the Gifter and Receiver pages themselves, and the server's Santa route and its rules.

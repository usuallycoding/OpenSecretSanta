# Tasks

## 1. The Secret Santa card

- [x] 1.1 Add the heading, button and "TBD" strings and a composable for the "You're Secret Santa for:" card: "TBD" and no button while Pending; from Active the receiver's username in capitals (spoken as written), "See Wishlist" and "Ask Questions" on one row and "Delivery Info" last on its own row; "..." with the buttons disabled and the failure message with "Try again" while the receiver is not known; verify with a look at the card in a Pending, an Active and an Archived group
- [x] 1.2 Pass whether to open on the questions tab through the group page's `onOpenGifter` and `onOpenReceiver` and into `GifterRoute` and `ReceiverRoute` in `ShellScreen`; verify "See Wishlist" opens the wishlist tab and "Ask Questions" the questions tab
- [x] 1.3 Add the line "Shh... Keep it a secret!" in italics under "TBD" or the username in the first card, in every state; verify it shows in a Pending, an Active and an Archived group

## 2. The Your stuff card and the removal of the rows

- [x] 2.1 Add the "Your stuff:" card: "Edit Wishlist" (Receiver page, wishlist tab), "Respond to Santa" from Active (Receiver page, questions tab), "Reveal Santa" only when Archived (the Santa sheet) and "Delivery Info" last (own sheet); verify each button in a Pending, an Active and an Archived group, and that no Santa name or reveal exists before Archived
- [x] 2.2 Replace `PairRows` in `GroupPage` with the two cards, delete `PairRows.kt` and the strings only it used (`MY_WISHLIST`, `TITLE_GIFTER`, `TITLE_RECEIVER`, `SECRET_SANTA`), and update `PageTitlesTest`; verify the group page has no pair row for a participant and the administrator's pair list is unchanged, with `./kotlin test` passing apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`
- [x] 2.3 Add the line "Tell your santa what you want and let them know you!" in italics under "Your stuff:", in every state; verify it shows in a Pending, an Active and an Archived group

## 3. Docs

- [x] 3.1 Rewrite the README paragraph on the group page for the two cards; verify it no longer mentions the rows, the arrows or the gift icon on a participant's page
- [x] 3.2 Edit the Purpose of `openspec/specs/client-group-pages/spec.md`, which still names "the pair rows", to name the cards; run `openspec validate --specs --strict`; verify all specs pass

## Workflow follow-up

- Archive the change after trying both cards in a Pending, an Active and an Archived group.

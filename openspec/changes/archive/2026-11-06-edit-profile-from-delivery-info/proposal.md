# Proposal

## Why

The "Delivery Info" sheets read like plain text dumps: the username is pushed to the start while the details sit in a card below it, and the person who sees that their own details are missing or wrong has to leave the group, find the Profile tab and come back to nothing. The sheet should look balanced and should lead to where the details are edited.

## What Changes

- In both "Delivery Info" sheets, the person's own and the receiver's, the username is centered horizontally, with its copy icon beside it.
- The person's own sheet ends with an "Edit Delivery Info" button. Choosing it closes the sheet and opens the Profile tab. It is there when the details are shown and also when the person has no profile yet.
- The receiver's sheet has no such button: those details belong to someone else.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-group-pages`: two added requirements, for the centered username and for the "Edit Delivery Info" button. The existing "User details sheets" requirement is unchanged.

## Impact

- Client only: `PairSheets.kt` (the own and receiver sheets), `ReadOnlyText.kt` (a centered option), `GroupPage.kt` (a callback to open the Profile tab) and `ShellScreen.kt` (which opens the tab). One new string. No server, contract or database change.
- Out of scope: the Santa reveal sheet and the administrator's sheets, which keep their layout.

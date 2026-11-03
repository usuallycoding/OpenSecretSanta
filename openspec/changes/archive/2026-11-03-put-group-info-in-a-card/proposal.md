# Proposal

## Why

On the group page the description, the date and time and the location are loose lines of text under the group's state, and the date and the location carry no label, so a bare "Grandma's house" has to be read as a place. Putting the information in a card with labelled lines sets it apart from the state and the actions around it and matches the cards used everywhere else in the app.

## What Changes

- On the participant's group page and on the admin group screen, the description, the date and time and the location are shown together in one card, in that order, after the name and the state.
- The date and time line reads "When? - <date and time>" and the location line reads "Where? - <location>", for example "When? - 24 Dec 2026, 18:30" and "Where? - Grandma's house". The label is semi-bold.
- A part the group does not have is left out, and a group with none of the three shows no card.
- The admin screen's actions (edit, announce, next step, delete) stay below the card, outside it.
- Both screens use one shared card, so they cannot differ.
- The name, the state and its note, the pair rows and the participant chips stay as they are, on the page and not in the card. The group list is unchanged.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-groups`: the group page shows its description, date and time and location in a card with "When?" and "Where?" labels.
- `client-admin-groups`: the admin group screen shows the same card above its actions.

## Impact

- `app/shared/src/ui/groups/GroupPage.kt` (`Header`) and `app/shared/src/ui/admin/AdminGroupScreen.kt` (`Header`).
- A new shared composable in `app/shared/src/ui/groups/` for the card, and two strings in `Strings.kt`.
- No server, API or data change.

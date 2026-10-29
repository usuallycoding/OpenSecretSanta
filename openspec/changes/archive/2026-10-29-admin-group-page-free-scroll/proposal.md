## Why

The administrator's group page keeps the people (participants, or the pairs once drawn) in a box that scrolls inside itself while the details and buttons stay above it. The box is a scroll area inside a scrolling page: it is awkward to use with a finger or a wheel, it hides how long the list is, and the member's own group page already scrolls as one page. The page should simply scroll.

## What Changes

- The administrator's group page scrolls as one page: the details, the buttons, the search field and the participants or pairs all move together, with no box that scrolls inside the page. The header no longer has a height limit of its own either.
- The "Who gives to whom" label gains the number of participants, `Who gives to whom (4 participants)`, since the length of the list can no longer be read from the size of a box. The number counts the whole group and does not change while a search narrows the list.
- The empty-state, no-match and error messages of the people area are shown in the page, not in a box.
- Pulling the page down still reloads it on Android and iOS, and the web has no refresh control, as before.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-admin-groups`: "Group detail shows who takes part" drops the scrolling box and says the page scrolls as one.
- `client-admin-oversight`: "The draw" drops the box, says the list is part of the page, and gives the label its participant count.

## Impact

- Client only: `AdminGroupScreen`, the pair list in `OversightSheets.kt`, and the chips of a Draft group (`ParticipantChips` already has an unboxed form that the member's page uses). `HeaderWithFooter` and `ScrollingBox` are no longer used by the administrator's page and are removed if nothing else uses them.
- One string changes (`Who gives to whom` with a count). No server, contract or database change.

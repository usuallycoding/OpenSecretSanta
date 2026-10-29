## Context

`AdminGroupScreen` lays the page out with `HeaderWithFooter`: a header (details and buttons) with its own height limit and scroll, and a footer that fills the rest of the height and holds a `ScrollingBox` (an outlined card that scrolls inside itself) with the participants or the pairs. The member's group page (`GroupPage`) is the other way: a `PullPage` holding one column that grows and scrolls as a whole, with `ParticipantChips(boxed = false)`.

## Goals / Non-Goals

**Goals:** the administrator's group page scrolls as one page; the label of the pair list tells how many participants there are.

**Non-Goals:** changing what the rows show or do, the search, the sheets, or the member's pages.

## Decisions

- **Use the member page's structure.** `AdminGroupScreen` shows one column inside `PullPage` (which keeps pull-to-refresh on Android and iOS and nothing on the web) holding the header, the notices, the search field and the people. The people are plain rows and chips in that column, so there is a single scroll area. `HeaderWithFooter` and `ScrollingBox` are deleted if nothing else uses them once this is done.
- **No card around the rows.** Without the box the rows sit directly on the page under their heading; the pair row layout (Santa at the start, pair link, receiver at the end) is unchanged. A thin divider between rows can be added if the list is hard to read, which the device check will tell.
- **The count.** `Who gives to whom (N participants)` where N is the number of pairs of the whole draw (each participant is a Santa once), not the filtered list. The string becomes a function of the count; the Pending label already says `Participants (N)`.
- **Messages in the page.** "No participants yet", "No participant matches your search." and the draw's "Try again" are plain content of the column.
- **Search field and the keyboard.** The field is part of the scrolling column, so the existing keyboard handling for forms (content lifted above the keyboard, scrolling closes it) applies to it as to the other screens; this is checked on the device.

## Risks / Trade-offs

- A very long list pushes the buttons off the screen, which is the point of the change; the buttons are not pinned.
- Rows inside a `verticalScroll` column are all composed at once. Groups have tens of participants, not thousands, so a lazy list is not needed.

## Open Questions

None.

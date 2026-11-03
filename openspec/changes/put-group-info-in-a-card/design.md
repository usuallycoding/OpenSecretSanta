## Context

`GroupPage.kt` and `AdminGroupScreen.kt` each have a `Header` that lays out the name, the state and its note, the description, the date and time and the location as loose text, and the admin one adds the action buttons. The group list's `GroupCard` and the question cards already use a plain Material `Card`.

## Goals / Non-Goals

**Goals:** put the description, the date and time and the location in one labelled card on both screens, from one shared composable.

**Non-Goals:** the name, the state, the pair rows, the chips, the admin actions, or the group list.

## Decisions

- **One shared card.** `GroupInfoCard(description, whenText, location)` in `app/shared/src/ui/groups/`, used by both headers. It draws nothing when all three are null. This replaces the three duplicated lines in each header.
- **Plain `Card`.** Not clickable, 16 dp padding and 8 dp between lines, the default container colour, like `GroupCard`.
- **Labels.** The lines are built as an `AnnotatedString` with the label ("When?", "Where?") in `FontWeight.SemiBold` followed by " - " and the value, so one `Text` reads as one line for a screen reader and wraps as one. The strings are `Strings.WHEN_LABEL` and `Strings.WHERE_LABEL`, and a pure function builds the line's text so it can be tested.
- **The description** stays a `ReadOnlyText` with its "Description" label and its own long-text dialog, first in the card and unlabelled on screen.
- **The date line** takes the existing `whenText`, so "Dates and times are shown as the group has them" is unchanged.
- **Admin.** The card sits after the state and before the actions row; the buttons and the draw note stay where they are.

## Risks / Trade-offs

- A very long location wraps inside the card and makes it taller; nothing is cut, as before.
- The admin header gets one more box above its buttons; on a short screen the page already scrolls as one.

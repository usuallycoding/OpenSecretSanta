# Design

## Context

The admin groups list (`AdminGroupsScreen`, `AdminGroupsViewModel`) already holds every group as an `AdminGroup` (name, description, location, date, time, participants with their flags). `CreateGroupRequest` carries name, description, location, date, time and participants, and `ApiClient.createGroup` posts it. Group names are limited to 100 characters (`GroupDetails.MAX_NAME_LENGTH`). Creating a group from the form opens the new group through `onSaved` in `ShellScreen`. The list cards are `Card(onClick = onOpen)`.

## Goals / Non-Goals

**Goals:**
- Duplicate any group as a Draft from the list, with a confirmation, always producing a valid group.

**Non-Goals:**
- Server changes, a duplicate button on the group's own page, copying the draw, wishlists or questions.
- Choosing which fields to copy.

## Decisions

1. **Client-side copy.** The view model builds the `CreateGroupRequest` from the `AdminGroup` it already has and calls `createGroup`; no new route. The usernames are taken from `participants`, in their order.
2. **Name.** A pure function `copyName(original)` returns "Copy of $original" when it fits in 100 characters, otherwise `"Copy of " + original.take(100 - "Copy of ".length - 1) + "…"`, so the result is exactly 100 characters. It lives beside the view model and is unit tested, including the boundary (92 characters fit exactly, 93 are cut).
3. **State.** `AdminGroupsViewModel` gains `duplicating: AdminGroup?` (the group the dialog is about), `busy` and `error`, and functions `askDuplicate(group)`, `cancelDuplicate()` and `confirmDuplicate()`, which on success hands the new group's id to the screen through a `created: Long?` value that the screen consumes (like `saved` in the form view model) and then clears.
4. **Button.** A copy icon button (`ContentCopy` is in the extended icon set, which the app does not use, so the existing drawn `CopyIcon` of the copy-text button is reused) at the end of the card's participants row, content description "Duplicate". A button inside a clickable card takes its own tap, so the card does not open.
5. **Dialog.** An `AlertDialog` "Duplicate this group?" with the body "A new Draft called "<copy name>" is created with the same details and participants." and Duplicate and Cancel buttons; while busy the Duplicate button reads "Duplicating..." and is disabled; a failure shows the message in the dialog and keeps it open.
6. **Navigation.** `AdminGroupsScreen` gets `onCreated: (Long) -> Unit`, which `ShellScreen` wires to `navController.navigate(AdminGroupRoute(id))`, as the new-group form does.

## Risks / Trade-offs

- The list may be stale (a group changed on another device) when copied; the copy is of what the list showed, and the next refresh corrects the list. Accepted.
- An ellipsis in a cut name is a judgement call; the copy can be renamed in the details.
- A participant list copied from an Active group may include people who no longer should take part; the copy is a Draft and can be edited.

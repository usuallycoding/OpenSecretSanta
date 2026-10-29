# Design

## Context

- Both lists get their groups whole: `GET /groups` and `GET /admin/groups` return every group with its `description` and `state`. The participant's row model (`GroupRow`) drops the description; the administrator's model (`AdminGroup`) keeps it.
- Both lists are in `LoadContent`, which draws the loaded value under an optional progress bar and error line, and wraps it in pull-to-refresh when the platform has the gesture. The refresh needs a scrollable child, which is why the empty states use `ScrollableMessage`.
- Each tab's navigation entry is saved and restored (`openTab` uses `saveState` / `restoreState`), so a screen's view model lives as long as the person stays signed in. A sign-in builds a fresh shell, so nothing outlives a sign-out.
- States reach the app as the server's lowercase strings (`draft`, `pending`, `active`, `archived`); `groupStateInfo` already labels them, and the label of an unknown state is its name.
- `groups.created_at` (a `CURRENT_TIMESTAMP` text column, second resolution) exists and is never updated. Both list queries sort by `id`, ascending: `findOwnedBy` (`WHERE owner_username = ? ORDER BY id`) and `findVisibleTo` (`... AND g.state <> 'draft' ORDER BY g.id`).

## Goals / Non-Goals

**Goals:**
- Newest first in both lists, decided in one place.
- One implementation of search and chips shared by both screens, whose rules can be tested without a screen.
- Filtering never changes the order and never needs another request.

**Non-Goals:**
- Other sort orders, searching location, participants or dates, accent-insensitive matching, highlighting the match, counts on the chips, remembering the search across restarts, searching on the server.

## Decisions

### Newest first is decided on the server
Both queries change to `ORDER BY created_at DESC, id DESC`. `id` is `AUTOINCREMENT`, so it breaks ties within a second strictly newest-first, and it is what makes the "same second" scenario true.
- Why: the clients already show "the order the server returns them" (the `client-groups` and `client-admin-groups` requirements stay true as written), no field has to be added to the DTOs, and one query change covers every platform.
- Alternative: send `createdAt` and sort in each client. Rejected: more surface in the API and in two view models for a single fixed order. If another sort order is ever wanted, the field can be added then.
- No migration: the column exists, and nothing updates it, which is what keeps edits, drawing and archiving from moving a group.
- "Created" is the group's creation, not when a participant was added. The join date is not stored, and a participant added to an old group sees it lower down; accepted.

### Filtering is client-side, over the loaded list, with a pure function
A small interface, `GroupListItem` (`name`, `description`, `state` as the server's lowercase string), is implemented by `GroupRow` and `AdminGroup`. `filterGroups(items, query, states)` returns the matching items in their original order:
- the query is trimmed and split on whitespace into words, compared with Kotlin's `lowercase()`;
- an item matches when every word is contained in its name or its description (a missing description counts as empty);
- with no words every item passes; with no states every state passes; with states, an item passes only when its state is one of them, so a group in a state without a chip is shown only when no chip is on.
- Why a pure function: the rules are the part most likely to be got wrong, and it runs in the multiplatform tests on Android, iOS and Wasm alike.
- Why not search per field with weights or fuzzy matching: not asked for, and a plain "contains every word" is predictable.

### The choices live in a small state holder owned by each view model
`GroupListFilter` holds the query and the set of chosen states as Compose state (not flows), with `setQuery`, `toggle(state)` and `clear()`. Each of `GroupsViewModel` and `AdminGroupsViewModel` owns one, next to its loader, so the filter survives reloads (the loader replaces the list, never the filter) and tab switches (the view model lives with the entry) and goes with the shell on sign-out. The screen combines the loaded list with the filter and calls `filterGroups`; the view models do not duplicate the list.
- Compose state, not flows, because the text field keeps its own copy of what is typed and takes back the value it is given after every keystroke: a flow delivers it a frame late, and on a device that machine-typed "snacks" the field ended up as "snks". State read in the same frame is never behind.
- The chips on offer are a parameter: `[pending, active, archived]` for participants and `[draft, pending, active, archived]` for administrators, so the two rules in the spec are each one list in one place.

### Layout: the controls are pinned above the list, inside the loaded content
`GroupSearchBar` (an `OutlinedTextField` with a clear button and a row of `FilterChip`s in a `FlowRow`, as `ParticipantChips` already wraps) goes in the content lambda of `LoadContent`, above the `LazyColumn`, in a column that fills the space. That puts it:
- only when there is data (so not while loading, and not on a first-load error), and only when the list is not empty, as the spec says;
- above the list, not in it, so it stays in view while a long list scrolls;
- inside the pull-to-refresh area, which keeps working because the `LazyColumn` below is the scrollable child.
Alternative: the bar as the first item of the list, which would scroll away. Rejected: it defeats the point on a long list.
- The "no group matches" message is shown in place of the list, under the bar, with a "Clear" button, and is scrollable so that pulling down still works (like `ScrollableMessage`). It is a different message from "no groups yet", which is chosen before the bar is shown at all.
- The administrator's "New group" button is unaffected: it floats over the list's bottom padding as now.
- Strings (field label, chip labels come from the existing state labels, "No group matches", "Clear") go into `Strings`.

### What a row keeps
`GroupRow` gains `description: String?`, filled in `toRow()` like the administrator's model (blank counts as none). Nothing in the row's look changes. `GroupsViewModelTest`'s expected `GroupRow` gains the field.

## Risks / Trade-offs

- [Reversing both lists surprises people used to oldest-first] → it is the requested behaviour; the Duplicate and ordering effects are named in the proposal.
- [A long list makes the per-keystroke filter slow] → a group list is a handful to a few hundred small items and the filter is one pass over them; no debounce is needed.
- [The soft keyboard covers the list on a phone] → the bar sits at the top, and the shell already hides its bottom bar while the keyboard is up; the search action closes the keyboard (a task checks it on a device).
- [Only the loaded groups are searched] → every group is loaded already; there is no paging.
- [`created_at` has second resolution] → the `id` tiebreak.

## Migration Plan

Nothing to migrate: the server's order changes as soon as it is deployed, and the clients show whatever order they get, so old clients simply show the new order without search. Rolling back is reverting the two queries.

## Open Questions

- The exact wording of the field's label and of the "no group matches" message (to be settled with the strings).

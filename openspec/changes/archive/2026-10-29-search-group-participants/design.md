## Context

`GroupViewer` builds the owner's view of a group from the accounts it already loads for the participants, and `GroupView.toDto()` maps it to `AdminGroupDto` with `ParticipantDto(username, hasAccount, profileComplete)`. The client shows a Pending group's participants from that payload and an Active or Archived group's pairs from `GET /admin/groups/{id}/assignments`, turned into `AdminPair` rows with the participants' flags. Group search on the lists already exists (`filterGroups`, `GroupListFilter`, `GroupSearchBar`).

## Goals / Non-Goals

**Goals:** find a person on a group page by username or full name; pairs stay when either side matches; no extra requests.

**Non-Goals:** searching by email, phone or address, searching the groups a participant sees, server-side search, keeping the search across screens.

## Decisions

- **Carry the full name in the group payload.** `ParticipantStatus` and `ParticipantDto` gain an optional `fullName`, filled from the account `GroupViewer` already has; absent for someone without an account or a name. The owner can already read these profiles in the user pages, so nothing new is exposed, and the member's view of a group (usernames only) does not change. The default null keeps older clients working.
- **Filter on the client.** A pure function over a list of participants (and one over pairs) with the rule of the group search: split the typed text at spaces, ignore case, every word must be found in `username + " " + fullName`. A pair matches when its Santa or its receiver does. This is easy to test without a screen.
- **The pair rows need the names.** `AdminPair` already carries each side's username and flags; it also carries the full name of each, taken from the participants the page has loaded, so the same function serves both lists.
- **The search state is Compose state in the view model**, like the group list filter and the announcement fields, so that typing quickly never loses letters; it is dropped with the page's view model.
- **The field is `SheetTextField`-based like the groups search**, above the list, with a clear control, shown when the group has participants. The heading's count and the legend stay about the whole group.

## Risks / Trade-offs

- A participant without an account has only a username to search by.
- A very long full name only matters to the search, not to the layout, since rows still show usernames.

## Open Questions

None.

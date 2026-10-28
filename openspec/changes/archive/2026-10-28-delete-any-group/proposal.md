# Proposal

## Why

Only a Draft can be deleted today, so a finished, abandoned or mistaken group (Pending, Active or Archived) stays in the owner's list for ever. The database already removes everything that hangs off a group (wishlist items, the draw, questions and answers) when the group row is deleted; only the "Draft only" rule stands in the way. Deleting that much is destructive, so the owner must be asked twice whenever there is more than the group's details and participants to lose.

## What Changes

- The owner can delete a group in any state, with `DELETE /admin/groups/{id}`.
- A group that has linked data (a draw, wishlist items, questions or answers; so every Active and Archived group, and a Pending group that has wishlist items) is not deleted by a plain request: the server answers `409` with the code `confirmation_required` and the counts (wishlist items, questions, answers, and whether there is a draw). Repeating the request with `?confirm=true` deletes the group and all of that data. A Draft, or a Pending group with no wishlist items, is deleted at once, as a Draft is today.
- The app offers "Delete" on every group's page. The first confirmation is as today; when the server asks for confirmation the app shows a second dialog that says what will be deleted ("12 wishlist items, 7 questions, 5 answers and the draw"), that it cannot be undone, and deletes only if that is confirmed.
- Participants who have the group open find it gone ("no longer available"), as already happens.
- No soft delete and no change to the database schema: the existing cascades do the removal.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `groups`: "Delete a Draft group" becomes "Delete a group", with the confirmation rule for groups that have linked data.
- `client-admin-groups`: "Deleting a Draft" becomes "Deleting a group", with the second confirmation; "Moving a group forward" now has "Delete" in the row for every group.
- `admin-oversight`: "Oversight is read-only" names deleting the whole group as the only way these items go.

## Impact

- Server: `DeleteDraftGroup` becomes a delete for any state with an impact check, `GroupRepository.deleteDraft` becomes `delete` (with a count of the linked data), the route reads `confirm`, a new domain error and its 409 body (`ErrorResponse` gains an optional `impact`), `core/contract` (the DTO), tests, the README route table.
- App: `ApiClient.deleteGroup(id, confirm)`, a confirmation-required error with the counts, `AdminGroupViewModel` (delete for every phase, the second dialog), `AdminGroupScreen`, `Strings.kt`, tests and the README.

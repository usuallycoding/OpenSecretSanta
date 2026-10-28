# Proposal

## Why

Groups are often repeated (the same people, the same place, a new year). Today the administrator has to create a new group by hand and retype the details and every participant. A copy of any group, whatever its state, as a fresh Draft would save that work.

## What Changes

- Every card of the admin groups list gets a copy button (icon, named "Duplicate"), for groups in every state.
- Choosing it asks for a confirmation ("Duplicate '<title>' as a new Draft?"). Confirming creates a Draft with the same description, location, date, time and participants, titled "Copy of <original title>", and opens the new Draft's page.
- A title that would be longer than 100 characters once prefixed is cut, ending in an ellipsis, so the copy is always valid.
- A failure (server unreachable, a refused field, the group gone) is said in the dialog and nothing is created.
- Nothing from the original's draw, wishlists or questions is copied. No server change: the app sends `POST /admin/groups` with the details and participants it already holds.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-admin-groups`: the groups list offers duplicating a group, and a new requirement describes it.

## Impact

- `app/shared/src/ui/admin/AdminGroupsScreen.kt` (button and dialog), `AdminGroupsViewModel.kt` (duplicate action and its state), `Strings.kt`, `ShellScreen.kt` (open the new group), tests and the README.

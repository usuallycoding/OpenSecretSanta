## Context

An administrator's scope is computed by `AccessPolicy` from `GroupMembershipRepository.membersOfGroupsOwnedBy` (members of the groups they own that are not Draft) plus themselves. Accounts are created in two places: `NewAccountPlanner` (adding a new username to a Pending group, and bulk creation) and `SeedDefaultAdmin`. The `group_participants` table has no foreign key to `users` (a Draft may name people without accounts), so the database will not stop an account that is still named in a group from being deleted; the use case has to check. `push_devices` and `notification_prefs` already delete with their user; `login_attempts` is not tied to users and is cleared by hand, as the password reset already does.

## Goals / Non-Goals

**Goals:** an administrator can delete the stray accounts they created, only while the person is in no group; the Users tab can be searched.

**Non-Goals:** deleting administrators or anyone in a group, deleting accounts another administrator created, an undo, bulk delete, changing who may see accounts that are in groups.

## Decisions

- **Record the creator.** Migration `V008` adds `users.created_by TEXT REFERENCES users (username) ON DELETE SET NULL` (nullable, so `ALTER TABLE ... ADD COLUMN` is allowed). `NewUser` gets an optional `createdBy`; `NewAccountPlanner.plan` takes the administrator and passes it, `SeedDefaultAdmin` passes none. The migration backfills `created_by` for every non-administrator account to the administrator when exactly one administrator exists, so an installation with a single administrator (the usual one) can clean up what typos already left behind; with several administrators the creator of older accounts is unknown and they stay out of reach.
- **Scope grows by one rule.** `AccessPolicy.managedUsernames` adds the non-administrator users whose `created_by` is the caller and who are in no group (`group_participants`, any state, any owner); `canManage` follows. That is the only change to who an administrator sees, and it keeps view and reset available for those users like any other managed user. A user the caller created who then joins another administrator's Draft drops out of the scope until that group stops naming them.
- **Delete is a use case with the same checks as the flag.** `DeleteUser(actor, target)`: administrator required; target outside the scope is not found; refuse (`InvalidState`, 409) when the target is an administrator, is the caller or is in any group; otherwise delete in one transaction (the membership check and the deletes together, so a group that names the person at that moment loses the race cleanly), clear the login attempts, and let the cascades remove devices and settings. `UserRepository` gains `delete(username)`; the check uses a new `GroupMembershipRepository.isInAnyGroup`.
- **`canDelete` is computed by the same function.** One rule (`canDelete(actor, user)`) serves the list, the detail and the delete route, so the button and the server cannot disagree. `ManagedUserDto` gains `canDelete` (default false, so older clients ignore it).
- **A deleted user's token.** `installAccessTokens` loads the user for each request; a missing user must come out as 401, which a test checks.
- **Search is client-side and reuses the matching of the participant search** (`matchesSearch`, words in any order over username and full name). The typed text is Compose state in `AdminUsersViewModel`; the field, the clear control and the no-match message follow the groups and participants search.
- **The list deletes.** A deletable user's row shows "Not in any group" and a delete icon (named "Delete user" for assistive technology); the icon opens a confirmation dialog naming the person, and confirming deletes and reloads the list. The detail page, which is also shown in a group's sheet, offers no delete. A 409 or 404 shows a message and reloads the list.

## Risks / Trade-offs

- Deleting is permanent. The confirmation says so, and only accounts with no group, so no wishlist or question data, can reach it.
- Two administrators who share an installation do not see each other's stray accounts, which is the point of choosing the creator over "any administrator".
- The backfill assumes one administrator means that administrator made the accounts, which is true for accounts only administrators can create.

## Open Questions

None.

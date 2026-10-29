## Why

Adding a participant to a Pending group creates an account for any username that has none, so a typo leaves a stray account behind, and removing the person from the group does not remove it. Nothing in the app can delete an account, and a user who is in no group is outside every administrator's scope, so the stray account cannot even be seen. The Users tab also has no way to find a person in a long list.

## What Changes

- The server records which administrator created each account (a new `created_by` on users): accounts made by adding a new username to a Pending group, or by bulk creation, get that administrator. The first administrator and accounts made before this change have no creator, except that when exactly one administrator exists the migration gives all existing non-administrator accounts to that administrator, so a single-administrator installation can clean up what is already there.
- An administrator's managed scope also covers the non-administrator users they created who are in no group at all (any owner, any state, Draft included). Those users now appear on their Users tab, marked "Not in any group", and can be viewed and reset like any managed user.
- `DELETE /admin/users/{username}` deletes such a user: only one the caller created, who is not an administrator and not the caller, and who is in no group. It removes the account with its devices and notification settings and its login lockout record, and the person's sign-in stops working. A user who is in a group, an administrator or the caller is refused with 409, and one outside the scope is answered as not found. The server decides again at delete time, whatever the app showed.
- `GET /admin/users` and `GET /admin/users/{username}` say, per user, whether the caller may delete them (`canDelete`).
- Each row of the Users list for a user the server says can be deleted shows a delete icon; it asks for a confirmation that names the person and says it cannot be undone, then deletes and reloads the list. The user's detail page offers no delete.
- The Users tab gets a search field over username and full name, with the matching rules of the participant search (words in any order, ignoring case) and a message when nobody matches.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `user-administration`: the managed scope also covers the caller's own unassigned users; new requirements for recording who created an account and for deleting an unassigned user.
- `client-admin-users`: the Users list includes and marks those users; new requirements for searching the list and for deleting a user.

## Impact

- Database: migration `V008` adds `users.created_by` and the backfill above. Domain: `NewUser` carries the creator, `AccessPolicy` and the user repository learn the new scope and a delete, a new `DeleteUser` use case. Server: the route, the DTO field, the creator passed where accounts are planned. No change to groups, wishlists or questions.
- Client: `ManagedUser` gets `canDelete`; the Users list (delete icon, confirmation dialog), its view model, strings; the search reuses the participant search matching.
- Limits: accounts made before this change by an administrator when more than one exists have no creator and cannot be deleted from the app; a deleted username can be created again by naming it in a group.

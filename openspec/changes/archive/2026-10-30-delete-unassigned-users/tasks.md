## 1. The creator of an account

- [x] 1.1 Add migration `V008` (`users.created_by`, backfill when there is exactly one administrator) and list it in `index.txt`; a migration test for one and for two administrators
- [x] 1.2 Carry the creator through `NewUser`, `NewAccountPlanner` and the repository insert; the group and bulk-creation use cases pass the acting administrator, `SeedDefaultAdmin` passes none; tests

## 2. Scope and deletion in the domain

- [x] 2.1 Extend `AccessPolicy`/`GroupMembershipRepository` with the caller's own users who are in no group, and add `isInAnyGroup`
- [x] 2.2 Add the single `canDelete` rule and the `DeleteUser` use case (not found / 409 / delete in one transaction, lockout record cleared) with `UserRepository.delete`; domain tests for every rule and scenario of the spec

## 3. Server

- [x] 3.1 Add `canDelete` to `ManagedUserDto` and its mapping, and `DELETE /admin/users/{username}` (204, 404, 409) with Koin wiring
- [x] 3.2 Server tests: scope, listing, delete, refusals, devices and settings gone, a signed-in user's next request is 401, the name can be reused

## 4. Client

- [x] 4.1 Add `canDelete` to `ManagedUser` and the API call `deleteUser`, with the `ScriptedServer` fake and API tests
- [x] 4.2 Users tab: the search field, filter and no-match message (view model state, tests), and the "Not in any group" mark
- [x] 4.3 Users list: a delete icon on each deletable user's row with the confirmation dialog and the result handling in the list's view model, with tests; no delete on the detail page; strings

## 5. Verify

- [x] 5.1 Run the full test suites and `openspec validate delete-unassigned-users --strict`
- [x] 5.2 Check in a browser (the phone was not connected): two typo accounts made by a Pending group and then removed from it, the Users tab marking them "Not in any group", the search by name, cancel sending nothing, deleting one
- [x] 5.3 Docker build and the person's web check

# Tasks

## 1. Domain model

- [ ] 1.1 Add `kotlinx-datetime` to the version catalog and `core/domain`, add `DomainException.InvalidState` (code `invalid_state`), `GroupState` with a single forward `next()`, and `GroupDetails` validation (name 1-100 chars, description at most 2000, location at most 200, strict ISO date and `HH:mm` time); verify common tests for the forward-only order, every illegal step, blank/over-long name, `2026-02-30`, `24:00`, and that `./kotlin build -m core/domain` succeeds on all platforms
- [ ] 1.2 Add the `Group` model, `Assignment`, the `GroupRepository` port (create, find, owned, visible-to, details update, draft delete, `moveToPending`, `replaceParticipants`, `activate`, `archive`, assignments) and the `Shuffler` port; verify `./kotlin build -m core/domain` succeeds on all platforms

## 2. Use cases (pure Kotlin, in-memory fakes)

- [ ] 2.1 Implement `AssignmentPlanner` (shuffle, then each participant gives to the next in the ring); verify tests with a fixed shuffler and a reversing shuffler for every size from 3 to 50: each participant is Santa once and receiver once, nobody is their own receiver, and following the chain visits everyone before returning
- [ ] 2.2 Split the account planning out of `CreateMissingUsers` into a reusable planner returning new users with their clear temporary passwords; verify the existing `CreateMissingUsers` tests still pass unchanged and a planner test covers existing, missing and duplicate-by-case names
- [ ] 2.3 Implement `CreateGroup`, `UpdateGroupDetails`, `DeleteDraftGroup`, `ListOwnedGroups` and `GetOwnedGroup` with owner-only access (404 for others) and participant flags (`hasAccount`, `profileComplete` = full name and address); verify tests for minimal group, non-admin refused, other admin gets 404, Archived not editable, delete only in Draft, and the flags for complete, incomplete and account-less participants
- [ ] 2.4 Implement `SetParticipants`: Draft stores deduplicated valid names without creating accounts, Pending also creates accounts for unknown names and returns their credentials, Active and Archived are refused with `invalid_state`; verify tests for names without accounts, case duplicates, invalid name leaving the list unchanged, adding a new person, removing a person (account stays), adding an existing user (untouched) and the frozen roster
- [ ] 2.5 Implement `MoveToPending`, `ActivateGroup` (at least 3 participants, draws assignments, never blocked by incomplete profiles) and `ArchiveGroup`, plus `ListAssignments`, `ListMyGroups` and `GetMyGroup`; verify tests for every legal and illegal transition, accounts created only at Pending, fewer than 3 participants refused, incomplete profile still activates, assignments absent before Active and unchanged by edit or archive, assignments hidden from non-owners, and participants never seeing Draft groups or assignment data

## 3. Contract

- [ ] 3.1 Add group, participant, created-account, group-change and assignment DTOs plus the new routes to `core/contract`; verify round-trip serialization tests for each DTO and that `./kotlin build -m core/contract` succeeds on all platforms

## 4. Persistence

- [ ] 4.1 Add migration `V004__groups.sql` (drop and recreate `groups`, create `group_participants` without a user foreign key, create `assignments` with its constraints) and register it; verify migration tests show schema version 4, `/health` reports 4, and the database rejects an unknown state, a self-assignment and a receiver drawn twice
- [ ] 4.2 Implement `SqliteGroupRepository` with state-checked updates (`WHERE state = ?`), the atomic `moveToPending`, `replaceParticipants`, `activate` (including the in-transaction participant check) and `archive`; verify repository tests for each transition, rollback when a user insert fails, `invalid_state` on a stale expected state, assignment constraints, cascade delete, and a two-thread double-activation race that yields exactly one set of assignments
- [ ] 4.3 Change `SqliteGroupMembershipRepository` to count participants of non-Draft owned groups that have an account, and update the test fixtures (`addGroup` takes a state and participants); verify the existing admin and repository tests pass again plus new tests for Draft participants being out of scope and for removal ending the scope

## 5. Server routes

- [ ] 5.1 Map `InvalidState` to 409 and implement `POST/GET/PUT/DELETE /admin/groups...` and `PUT /admin/groups/{id}/participants`, with 403 for non-admins and 404 for other owners and non-numeric ids; verify HTTP tests for creation, listing only own groups, editing, participant replacement in Draft and Pending (response includes created accounts), frozen roster, Draft-only delete, and the password-change gate
- [ ] 5.2 Implement `POST /admin/groups/{id}/pending|activate|archive`, `GET /admin/groups/{id}/assignments`, and the participant routes `GET /groups` and `GET /groups/{id}`; verify HTTP tests for the full Draft to Archived path, account creation and credentials at Pending, the three-participant rule, assignment listing for the owner only, participants seeing Pending groups but not Draft ones, and no assignment data in participant responses
- [ ] 5.3 Register the group components in Koin and update the existing Koin graph test; verify the server graph test passes and `./kotlin test -m server` is green

## 6. Integration

- [ ] 6.1 Document the group endpoints and lifecycle in `README.md`, run `./kotlin build` and `./kotlin test` for the whole repo, then start the server and verify with `curl`: create a Draft group naming one existing and one new user plus a third, move it to Pending and see the new account's temporary password, add a fourth person, activate, list the assignments as owner, and confirm another admin gets 404 and a participant sees the group without any assignment data

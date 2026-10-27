# Proposal

## Why

Users exist, but there is still no way to run an actual Secret Santa: no groups, no lifecycle to move a group from planning to exchange, and no assignment of who gives to whom. This slice delivers the core of the product on the server: admins create and manage their own groups, move them through Draft, Pending, Active and Archived, and the system creates participant accounts and draws the assignments at the right moments. What participants see of their receiver, and the questions and answers between Santa and receiver, follow in the next change.

## What Changes

- Groups: an admin creates groups (name, description, location, date, time, participants) that start in Draft; the owner edits, lists, views and deletes (Draft only) their own groups; participants see the non-Draft groups they belong to.
- Participants: in Draft they are plain usernames that need not exist; in Pending the owner can still add (creating new accounts with temporary passwords) and remove participants; from Active the roster is frozen.
- Lifecycle: the owner moves a group Draft to Pending to Active to Archived, never backwards. Draft to Pending creates the missing accounts atomically; Pending to Active requires at least 3 participants and draws the assignments atomically.
- Assignment: one random cycle over the participants so nobody draws themself; the owner can list all assignments of their group.
- Admin view of participants flags whether each profile is complete (full name and address) so the owner can chase stragglers; activation is never blocked by it.
- Managed-user scope changes: an admin manages users who participate in a non-Draft group they own, no longer anyone merely named in a Draft.
- New `invalid_state` error (HTTP 409) for transitions and edits that the group's state does not allow.
- **BREAKING**: migration `V004` replaces the unused `groups` / `group_members` skeleton from `V003` with the full group tables (`group_participants` has no foreign key to users, so Draft can hold names without accounts). No API could write to the old tables, so no data is lost.

Out of scope: what a participant sees about their receiver, the Q&A, the Santa reveal on Archive, the wishlist, client screens, group ownership transfer, notifications.

## Capabilities

### New Capabilities
- `groups`: group records and their fields, owner-only management, participant lists by state, and the participant-facing view of groups they belong to.
- `group-lifecycle`: the Draft, Pending, Active, Archived state machine, its preconditions, its side effects (account creation, assignment) and the atomicity of each transition.
- `gift-assignment`: the random single-cycle assignment drawn at activation and the owner-only listing of all assignments.

### Modified Capabilities
- `user-administration`: the managed-user scope and the group ownership/participant records requirement change to count only participants of non-Draft groups.

## Impact

- Code: new group, lifecycle and assignment use cases and ports in `core/domain`; DTOs and routes in `core/contract`; group repository, `V004` migration and routes in `server`; `SqliteGroupMembershipRepository`, `CreateMissingUsers` and the test fixtures that insert groups change.
- APIs: new `/admin/groups...` (owner), `/groups...` (participant) and assignments endpoints; `/health` reports schema version 4.
- Dependencies: `kotlinx-datetime` in `core/domain` for validated local dates and times.
- Systems: no new environment variables.

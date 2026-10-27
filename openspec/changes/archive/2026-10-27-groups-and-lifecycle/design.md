# Design

## Context

`auth-and-users` left a minimal `groups(id, owner_username)` and `group_members(group_id, username REFERENCES users)` skeleton (`V003`), an `AccessPolicy` that asks a `GroupMembershipRepository` which users an admin manages, and `CreateMissingUsers`, which inserts accounts through the user repository in its own transaction. Every repository opens its own connection, so nothing today can change several tables atomically. See proposal.md for motivation and specs/ for required behavior. Draft participants may have no account, which the `group_members` foreign key to `users` forbids.

## Goals / Non-Goals

**Goals:**
- Each lifecycle step (account creation, state change, assignment) commits entirely or not at all, and cannot be applied twice.
- State rules and the assignment algorithm live in `core/domain` and are testable without a database.
- The database itself refuses impossible data (self-assignment, a receiver drawn twice, an unknown state).

**Non-Goals:**
- What participants see about their receiver, Q&A and the Santa reveal (next change).
- Ownership transfer, co-owners, notifications, client screens.
- A general transaction manager spanning all repositories.

## Decisions

### D1: Schema `V004` replaces the skeleton
```
groups(id PK, owner_username FK users, name NOT NULL, description, location,
       event_date TEXT, event_time TEXT,
       state TEXT NOT NULL DEFAULT 'draft' CHECK(state IN draft|pending|active|archived),
       created_at)
group_participants(group_id FK groups ON DELETE CASCADE, username TEXT NOT NULL
       CHECK(username = lower(username)), position INTEGER NOT NULL,
       PRIMARY KEY(group_id, username))               -- no FK to users
assignments(group_id FK groups ON DELETE CASCADE, santa TEXT NOT NULL,
       receiver TEXT NOT NULL, PRIMARY KEY(group_id, santa),
       UNIQUE(group_id, receiver), CHECK(santa <> receiver))
```
`V004` drops `group_members` and `groups` and recreates them: both were unreachable through any API, so they are empty. `group_participants` has no foreign key so that Draft can name people without accounts; `position` keeps the owner's order for display. Dates and times are stored as ISO text (`2026-12-24`, `18:30`). Managed-user scope becomes one query: participants of groups owned by the admin whose state is not `draft`, joined to `users` so only existing accounts count. *Alternative:* keep `group_members` and add a second table for Draft names (two sources of truth that must be reconciled at every transition).

### D2: Domain model
`GroupState` is an enum with a single `next()`; `Group(id, owner, details, state, participants)` and `GroupDetails` (validated: name 1-100 chars, description at most 2000, location at most 200, date and time parsed strictly). Dates and times use `kotlinx-datetime` `LocalDate` / `LocalTime` in `core/domain` (pure Kotlin, available on every target) so impossible dates such as 30 February are rejected by a library instead of by hand-written checks. The API carries them as ISO strings; mapping happens in the server. *Alternative:* strings plus regexes (misses real-calendar validation).

### D3: Atomicity by plan-then-commit
Use cases compute everything that can be computed (new accounts with hashed and encrypted passwords, the shuffled assignment) and hand the finished plan to one `GroupRepository` method that applies it in a single SQL transaction:
- `moveToPending(id, newUsers)`: state `draft` to `pending` plus user inserts.
- `replaceParticipants(id, participants, newUsers)`: allowed while `draft` or `pending`; inserts users only for Pending.
- `activate(id, assignments)`: state `pending` to `active` plus assignment inserts.
- `archive(id)`, `updateDetails(id, details)`, `deleteDraft(id)`.

Each method states the expected current state in its SQL (`UPDATE groups SET state = ? WHERE id = ? AND state = ?`) and throws `InvalidState` when no row matched, which makes double submission and races harmless (spec: "at most once"). `activate` additionally checks inside the transaction that the assignment's participants equal the stored participants, because a participant change could slip in between the use case reading the group and committing. Hashing happens before the transaction opens so SQLite's single write lock is held only briefly. *Alternative:* a transaction-aware connection threaded through all repositories (cleaner call sites, but a large refactor for three call sites).

### D4: Account planning is shared
`CreateMissingUsers` is split into a pure planner that, given usernames and the set of existing ones, returns the `NewUser`s and their clear temporary passwords, and the existing insert step. Group use cases call the planner and pass the result to the repository; `CreateMissingUsers` keeps its behavior and tests.

### D5: Assignment
`AssignmentPlanner` shuffles the participants with a `Shuffler` port (secure-random implementation on the server, a fixed one in tests) and assigns each to the next in the shuffled ring, wrapping around. The result is always a single cycle with no self-assignment, for any group of at least 2. It is uniform over all cyclic arrangements, not over all derangements; that is deliberate, because it rules out mutual pairs and detached sub-circles, which is a property people expect of a Secret Santa. Minimum size 3 is enforced by the activation use case, not the planner.

### D6: Authorization shape
Admin group routes load the group and compare `owner` with the caller inside one `GroupAccess` helper; a mismatch and a missing id both throw `NotFound`, so ids reveal nothing. Non-admins are refused with `Forbidden` before any lookup. Participant routes (`/groups...`) use `findVisibleTo(username)` (participant and state not Draft) and return only usernames, never profile data. Non-numeric ids are `NotFound`. All group routes sit behind the existing password-change gate, which already blocks everything except `GET /me` and `PUT /me/password`.

### D7: API surface and errors
```
POST   /admin/groups                       create (201, Draft)
GET    /admin/groups, /admin/groups/{id}   list / view with participant flags
PUT    /admin/groups/{id}                  edit details (not Archived)
PUT    /admin/groups/{id}/participants     replace list (Draft, Pending)
DELETE /admin/groups/{id}                  Draft only (204)
POST   /admin/groups/{id}/pending|activate|archive
GET    /admin/groups/{id}/assignments      owner only, Active/Archived
GET    /groups, /groups/{id}               participant view, non-Draft
```
A new `DomainException.InvalidState` maps to 409 `invalid_state`. Responses that create accounts (`pending`, and `participants` in Pending) return `{group, createdAccounts:[{username, temporaryPassword}]}` once for convenience; the same passwords stay retrievable through the admin user view until the user changes them. Participant flags: `hasAccount` and `profileComplete` (full name and address present).

### D8: Tests
Domain: fakes for `GroupRepository` and the shuffler; table tests for the state machine and for the planner (cycle, no self, every size from 3 to 50, fixed seeds). Server: SQLite repository tests for each compare-and-set, rollback on a failing insert (an existing account name inserted as new), the database constraints, and a two-thread double-activation race; HTTP tests per route. Existing fixtures (`addGroup`) gain a state and participants.

## Risks / Trade-offs

- [`V004` drops two tables] → Both were unreachable until now; the migration comment says so, and the change's migration plan requires a backup of any database someone populated by hand.
- [Adding an existing user to a Pending group gives the owner view-and-reset power over that account] → Accepted: admins are trusted by design and the owner already hands out credentials; Draft deliberately grants nothing so merely typing a name is harmless.
- [Hashing one account per new participant takes about 250 ms each at cost 12, so 40 new people is about 10 s in one request] → Done off the request threads and outside the database transaction; revisit with parallel hashing if groups get large.
- [Removed Pending participants keep their account and temporary password] → Intended (they may join another group); the admin can still reset or ignore them.
- [A ring over a random shuffle is not uniform over all derangements] → Intended, see D5.
- [Concurrent edits in Pending racing with activation] → The in-transaction participant check in `activate` makes the loser fail with 409 instead of silently drawing for a stale list.
- [Sequential integer group ids] → Harmless because every route authorizes by owner or participation and answers 404 uniformly.

## Migration Plan

Deploy normally; `V004` applies on start and `/health` then reports schema version 4. Back up the database file first if anyone has inserted group rows by hand (nothing in the application can). Rollback is restoring the previous build and the pre-upgrade database file.

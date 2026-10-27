# Design

## Context

Everything the owner needs is already stored: wishlists per `(group, username)` in `wishlist_items`, questions and answers per Santa thread behind `QuestionRepository.askedBy`, and the draw behind `GroupRepository.assignments`. The owner is resolved by `OwnedGroupLoader`, which already answers 403 to non-admins and 404 to other admins and to missing groups, and owner routes live under `/admin/groups/{id}/...` inside the authenticated block, so the password-change gate applies. `GET /admin/groups/{id}/assignments` already exists. See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- Add the two read views without touching participant-facing code paths, so the participant privacy guarantees cannot regress.
- No new stored data: reading must be incapable of leaving a trace.
- Reuse existing loaders, repositories and result types instead of parallel ones.

**Non-Goals:**
- Any admin write, moderation, notifications, audit log or client screens (see proposal.md).
- A server-side "surprise mode" for an owner who also participates: the server sees one account and one token, so it cannot tell which view the client is showing.

## Decisions

### D1: No schema change
The views are pure reads over existing tables. There is no migration, no read table, no last-read column and no counter; `/health` still reports the current schema version. This is what makes "leaves no trace" true by construction rather than by discipline.

### D2: Two use cases in `core/domain`, built on existing ports
```
GetParticipantWishlist(loadOwned, wishlists)
  loadOwned(actor, id)                      403 non-admin, 404 other admin / missing
  state == DRAFT          -> InvalidState   (409)
  username not in group.participants -> NotFound   (404)
  wishlists.list(group.id, username) -> List<SharedWishlistItem>   (no ids, same type the Santa's view uses)

ListQuestionThreads(loadOwned, groups, questions)
  loadOwned(actor, id)
  state not ACTIVE/ARCHIVED -> InvalidState   (409)
  groups.assignments(group.id).map { AdminQuestionThread(santa, receiver, questions.askedBy(group.id, santa)) }
```
`SharedWishlistItem` is reused so the admin view cannot grow an id field by accident. `AskedQuestion` is reused for the question entries. The new `AdminQuestionThread(santa, receiver, questions)` lives next to the other question types. The state check comes after `loadOwned`, matching the precedent of `ListAssignments`: strangers learn nothing about a group's state. *Alternatives:* one combined "overview" endpoint (large, harder to cache and to reason about; the two views have different state windows), new repository methods that join across tables (more SQL surface for the same data).

### D3: API
```
GET /admin/groups/{id}/participants/{username}/wishlist   [SharedWishlistItemDto]
GET /admin/groups/{id}/questions                          [AdminQuestionThreadDto]
  AdminQuestionThreadDto(santa, receiver, questions: [QuestionDto])
```
`QuestionDto` already carries number, text, asked time and the optional answer and times. The routes go in a new `adminOversightRoutes(...)` in the presentation layer, registered next to `adminRoutes` and `groupLifecycleRoutes`. Group ids that are not numbers are 404, as for groups. A path username that is not a valid username, or is valid but not a participant, is 404 with the same message, so the response cannot be used to probe accounts. Threads follow the order of the assignments list so both admin views agree.

### D4: Consistency
The questions view reads the assignments and then each thread in separate statements, without one transaction. Assignments never change after activation, and a question or answer arriving between two reads only means the owner sees a slightly newer or older thread, as any two page loads would. A single transaction would add locking against writers for no benefit.

### D5: The owner who participates
The use cases do not look at whether the actor participates. Nothing is filtered out for the owner's own pairs, which is exactly the decision recorded in the specs. Participant routes are untouched: `/groups/{id}/santa` still refuses before archive even for the owner. The privacy statements in `gift-visibility`, `gift-assignment`, `wishlist` and `santa-questions` are narrowed to say so, so the specs and the code agree.

### D6: Tests
Domain with fakes: wishlist view for Pending, Active and Archived, Draft being `invalid_state`, another admin and a non-admin, a non-participant and an unknown username being `not_found`, order preserved, no ids in the result type; thread view for Active and Archived, empty threads present, answers and edit times, Draft and Pending being `invalid_state`, the owner who participates seeing their own pair, another admin and a non-admin refused. Server HTTP tests for both routes for each of those cases plus 401 without a token and `password_change_required` with a temporary password. A no-trace test: the owner reads both views repeatedly and every participant-facing response (own wishlist, receiver wishlist, both question threads, group view) is byte-for-byte unchanged. A sweep that no other `/admin/...` response contains wishlist or question text. A regression test that an owner who participates still gets `invalid_state` from `/groups/{id}/santa` while Active.

## Risks / Trade-offs

- [This deliberately widens who can read personal content] → Limited to the group's owner, read-only, owner-only 404s for everyone else, and covered by the sweeps in D6 so no other admin endpoint starts returning it.
- [An owner who participates can spoil their own surprise, and the server cannot stop it] → Accepted and written into the specs; the client's admin/participant switch is presentation only.
- [One admin account is also a participant and so reads threads about themselves] → That is their own thread, visible to them anyway; nothing extra is revealed except the Santa's identity, by decision.
- [Reading one thread per Santa is a query per participant] → Groups are small (at least four people, in practice tens) and SQLite is local; a batch query is a later optimisation if ever needed.

## Migration Plan

Deploy normally. No migration, no new configuration and no data changes. Rollback is restoring the previous build.

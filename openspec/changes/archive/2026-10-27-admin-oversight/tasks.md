# Tasks

## 1. Domain: the owner's wishlist view

- [x] 1.1 Add `GetParticipantWishlist` to `core/domain` (owned-group check, `invalid_state` in Draft, `not_found` for a username that is not a participant, returns the participant's `SharedWishlistItem`s in their order); verify common tests with fakes for Pending, Active and Archived, Draft, an empty list, order, another admin and a non-admin being refused, and a stranger or unknown username being `not_found`, and that `./kotlin build -m core/domain` succeeds on all platforms

## 2. Domain: the owner's question threads

- [x] 2.1 Add `AdminQuestionThread(santa, receiver, questions)` and `ListQuestionThreads` (owned-group check, `invalid_state` before Active, one thread per assignment in assignment order, each with that Santa's `AskedQuestion`s); verify tests for Active and Archived, a Santa with no questions still present, answers with first-given and last-changed times, Draft and Pending, an owner who participates seeing their own pair, and another admin and a non-admin being refused

## 3. Contract and routes

- [x] 3.1 Add `AdminQuestionThreadDto` and the two `ApiRoutes` entries (`ADMIN_GROUP_PARTICIPANT_WISHLIST`, `ADMIN_GROUP_QUESTIONS` with path helpers) to `core/contract`, reusing `SharedWishlistItemDto` and `QuestionDto`; verify a round-trip serialization test and that `./kotlin build -m core/contract` succeeds on all platforms
- [x] 3.2 Implement `adminOversightRoutes`, bind the two use cases in `ServerModules` and register the routes in `Application`; verify HTTP tests for both endpoints: the owner's success and ordering, empty list and empty thread, 409 `invalid_state` in the wrong states, 404 for another admin's group, an unknown group, a non-numeric id, a non-participant and an invalid username, 403 for a non-admin, 401 without a token, `password_change_required` with a temporary password, and the Koin graph test

## 4. Privacy guarantees

- [x] 4.1 Add server tests that prove the oversight views leave no trace and open nothing else: after the owner reads both views repeatedly, every participant-facing response (own wishlist, receiver wishlist, both question threads and the group view) is byte-for-byte unchanged; no other `/admin/...` response contains wishlist or question text; and an owner who also participates still gets `invalid_state` from `/groups/{id}/santa` while the group is Active, while the oversight views and the assignments list name their Santa
- [x] 4.2 Verify there is no admin endpoint that writes a wishlist item, question or answer (route table or tests asserting 404/405 for such requests), and that `/health` still reports the current schema version

## 5. Documentation and integration

- [x] 5.1 Document both endpoints, the states in which they work and the owner-only, read-only, no-trace rules in `README.md`, and verify the documented requests with `curl` against a real server
- [x] 5.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then verify end to end with `curl`: an owner who is also a participant reads a participant's wishlist in a Pending group, reads every thread in an Active one (including their own Santa), another admin gets 404, and the participants' own responses are identical before and after the owner reads

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

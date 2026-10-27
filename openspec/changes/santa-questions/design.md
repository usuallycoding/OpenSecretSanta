# Design

## Context

`groups-and-lifecycle` left an `assignments(group_id, santa, receiver)` table with `PRIMARY KEY (group_id, santa)` and `UNIQUE (group_id, receiver)`, a `GroupRepository` that already applies state-checked changes inside one transaction (`UPDATE ... WHERE state = ?`), participant routes under the password-change gate, and `ActivateGroup.MIN_PARTICIPANTS = 3`. See proposal.md for motivation and specs/ for behavior. The core constraint here is negative: nothing a receiver can read may identify their Santa before the group is Archived.

## Goals / Non-Goals

**Goals:**
- Anonymity enforced by the shape of the types and responses, not by remembering to omit a field.
- Question writes that are safe under concurrent requests (numbering, the 30-question cap, the Active check).
- Reuse the existing patterns: state check in the `UPDATE`, one transaction, domain errors mapped in `StatusPages`.

**Non-Goals:**
- Notifications, question edit or delete, an admin view of Q&A, a wishlist, client screens.
- Hiding timing correlations that happen outside the system.

## Decisions

### D1: Anonymity by construction
The receiver-side read model is a separate type (`ReceivedQuestion`) with no field that could hold a Santa; the Santa-side type (`AskedQuestion`) carries no receiver username either, since the caller already knows it. Routes are addressed by relationship (`/receiver/...`, `/santa/...`) and never by username. Error messages for hidden things (`/santa` before archive, wrong state) are fixed generic strings that do not interpolate any user. A response test greps every receiver-facing body for every other participant's username. *Alternative:* one shared DTO with a nullable `from` field filled only after archive (one slip leaks the Santa).

### D2: Schema `V005`
```
questions(group_id, santa, number, text, asked_at INTEGER, answer,
          answered_at INTEGER, answer_updated_at INTEGER,
          PRIMARY KEY(group_id, santa, number),
          FOREIGN KEY(group_id, santa) REFERENCES assignments(group_id, santa) ON DELETE CASCADE,
          CHECK(number >= 1), CHECK(answer IS NULL OR answered_at IS NOT NULL))
```
A question belongs to a Santa's assignment, so its receiver is always derived from `assignments` and cannot disagree with it. `number` counts per `(group, santa)`, so numbers leak nothing about other threads. Times are epoch milliseconds from the injected `Clock` (testable) and formatted as ISO-8601 UTC strings only at the API edge.

### D3: Safe numbering, cap and state check
`ask` runs in one transaction that first executes `UPDATE groups SET state = state WHERE id = ? AND state = 'active'`. That statement takes SQLite's write lock and doubles as the Active check, so the following `SELECT MAX(number), COUNT(*)` and `INSERT` cannot interleave with another writer; when it matches no row the answer is `InvalidState` (or `NotFound` for a missing group). The cap (30) is enforced there too. `answer` uses the same guard and then updates the row found by joining `assignments` on `receiver = caller`, so a receiver can only ever touch questions addressed to them. First answer sets `answered_at` and `answer_updated_at`; edits change only `answer` and `answer_updated_at`.

### D4: Ports and use cases
`GroupRepository` gains `receiverOf(groupId, santa)` and `santaOf(groupId, receiver)`; a new `QuestionRepository` has `ask`, `answer`, `askedBy` and `askedTo`. Use cases in `core/domain`: `GetMyReceiver`, `GetMySanta`, `AskQuestion`, `ListMyQuestions` (as Santa), `ListQuestionsFromMySanta` (as receiver) and `AnswerQuestion`. A shared `ParticipantGroupLoader` returns a group only if it is not Draft and the caller participates, otherwise `NotFound` (same rule as `GetMyGroup`). State rules: details and reads need Active or Archived, writes need Active, the reveal needs Archived.

### D5: API and DTOs
```
GET  /groups/{id}/receiver                       ReceiverDto
GET  /groups/{id}/santa                          SantaRevealDto (username, fullName)
GET  /groups/{id}/receiver/questions             [QuestionDto]   (as Santa)
POST /groups/{id}/receiver/questions             AskQuestionRequest -> 201 QuestionDto
GET  /groups/{id}/santa/questions                [QuestionDto]   (as receiver)
PUT  /groups/{id}/santa/questions/{n}/answer     AnswerRequest -> QuestionDto
```
`QuestionDto(number, text, askedAt, answer?, answeredAt?, answerUpdatedAt?)` is the same shape on both sides. All routes sit inside the existing authenticated block, so the password-change gate already applies.

### D6: Minimum of four participants
`ActivateGroup.MIN_PARTICIPANTS` becomes 4. With 3 participants a person's Santa is deducible whatever the draw (their receiver is one of the other two, and a pair is impossible), so anonymity needs at least 4, where the Santa is one of two people. The `group-lifecycle` requirement is renamed and modified, the README table is updated, and every test that activates with 3 participants moves to 4. Groups already Active with 3 are unaffected because the rule applies only at activation. *Alternative:* keep 3 and warn (the guarantee would simply be false for those groups).

### D7: Tests
Domain: fakes for the question repository, table tests for state rules, numbering per Santa, the cap, edit semantics and the anonymity types. Server: repository tests for numbering, cap, concurrent asks (distinct, gapless numbers), answer scoping through `assignments`, cascade delete; HTTP tests for each route including a sweep that asserts no receiver-facing response contains any other participant's username, and that the owner who does not participate gets 404.

## Risks / Trade-offs

- [With 4 participants the Santa is a 50/50 guess, with more it gets better] → Documented in the README; the minimum is a constant and larger groups are better.
- [Question timestamps could be correlated with something the Santa did offline ("they asked at 9:03 while sitting next to me")] → Accepted; timestamps could be rounded to the minute later without a schema change.
- [The group owner can list assignments, so an owner who participates knows everyone's Santa] → Already accepted when assignments were introduced; the Q&A content itself stays out of admin reach.
- [No notifications, so answers are seen only when someone looks] → Intended for now (credentials are also passed by hand); a later change can add polling hints or notifications.
- [The cap of 30 questions is arbitrary] → Single constant, easy to change.
- [Editing only answers, never questions, means a typo in a question stays] → Intended: it keeps the receiver's answers attached to what was actually asked.

## Migration Plan

Deploy normally; `V005` is additive and applies on start (`/health` reports schema version 5). Groups that are already Active keep working and get an empty question thread. Rollback is restoring the previous build and the pre-upgrade database file.

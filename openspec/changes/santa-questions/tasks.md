# Tasks

## 1. Minimum of four participants

- [ ] 1.1 Change `ActivateGroup.MIN_PARTICIPANTS` to 4 with a message that does not interpolate any user, update every domain and server test that activates a group with exactly 3 participants to use 4, add tests that 3 is refused (stays Pending, no assignments) and exactly 4 is accepted, and update the README activation row; verify `./kotlin test -m domain -m server` passes

## 2. Domain: ports and model

- [ ] 2.1 Add `receiverOf` and `santaOf` to `GroupRepository`, a `QuestionRepository` port (ask, answer, asked-by, asked-to) and the read models `AskedQuestion`, `ReceivedQuestion` (no field that can hold a Santa), `ReceiverDetails` and `SantaReveal`, with in-memory fakes for tests; verify `./kotlin build -m core/domain` succeeds on all platforms

## 3. Domain: visibility

- [ ] 3.1 Implement a participant-only group loader, `GetMyReceiver` (Active and Archived; live username, full name, address, phone, delivery information; `invalid_state` before Active) and `GetMySanta` (Archived only, username and full name only, generic `invalid_state` before); verify tests for each state, live values after the receiver edits their profile, unset fields, non-participants and Drafts getting `not_found`, and that no error message names any user

## 4. Domain: questions

- [ ] 4.1 Implement `AskQuestion` (1-1000 characters, Active only, number per Santa starting at 1, at most 30 per Santa and group), `ListMyQuestions`, `ListQuestionsFromMySanta` and `AnswerQuestion` (1-2000 characters, Active only, replace allowed, first-answer time kept, last-changed time updated, unknown number is `not_found`); verify tests for numbering per Santa, the cap, blank and over-long text, Archived and Pending refusals, edits, both threads of one participant staying separate, reads still working when Archived, and the receiver-side result type carrying no Santa identity

## 5. Contract

- [ ] 5.1 Add `ReceiverDto`, `SantaRevealDto`, `QuestionDto`, `AskQuestionRequest` and `AnswerRequest` plus the new routes to `core/contract`; verify round-trip tests, that `ReceiverDto` has no email or credential fields, and that a serialized receiver-side question list contains no key that could hold a Santa; `./kotlin build -m core/contract` succeeds on all platforms

## 6. Persistence

- [ ] 6.1 Add migration `V005__questions.sql` and register it, then implement `receiverOf` and `santaOf` in `SqliteGroupRepository`; verify migration tests show schema version 5 and `/health` reports 5, the database rejects a question for a Santa without an assignment and a duplicate number, and deleting a group removes its questions
- [ ] 6.2 Implement `SqliteQuestionRepository` using the state-checked write-lock pattern for asking and answering; verify repository tests for gapless numbering per Santa, the 30-question cap, `invalid_state` when the group is not Active, an answer reaching only the question addressed to the caller, first-answer versus last-changed times, and two threads asking at once getting distinct consecutive numbers

## 7. Server routes

- [ ] 7.1 Implement `GET /groups/{id}/receiver` and `GET /groups/{id}/santa` and register the new components in Koin; verify HTTP tests for receiver details in Active and Archived, `invalid_state` in Pending, the reveal only after archive with username and full name only, 404 for non-participants, Drafts and the non-participating owner, and the password-change gate, plus the Koin graph test
- [ ] 7.2 Implement the question routes (`GET` and `POST /groups/{id}/receiver/questions`, `GET /groups/{id}/santa/questions`, `PUT /groups/{id}/santa/questions/{n}/answer`); verify HTTP tests for asking, answering, editing, the limits and cap, Archived read-only, per-Santa numbering, the owner who does not participate getting 404, and a sweep asserting no receiver-facing response ever contains any other participant's username

## 8. Integration

- [ ] 8.1 Document the new endpoints and the four-participant minimum in `README.md`, run `./kotlin build` and `./kotlin test` for the whole repo, then verify with `curl` on a real server: a four-person group is activated, a Santa reads their receiver, asks two questions, the receiver sees them without any Santa identity and answers and edits an answer, the Santa sees the edit, the Santa is refused before archive, and after archive the reveal returns username and full name while writes return 409

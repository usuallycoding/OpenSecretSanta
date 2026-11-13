# Tasks

## 1. Contract, domain and server

- [x] 1.1 Add the migration `V017` (and its line in `index.txt`) that adds `assignments.last_question_number`, filled from the highest existing question number, with a migration test (new `QuestionNumbersMigrationTest`, like `QuestionMigrationTest`) that checks existing threads continue at their highest number
- [x] 1.2 Make `SqliteQuestionRepository.ask` number from the counter, and add `QuestionRepository.delete` with its SQLite version (one conditional delete, then 404 versus 409); verify with repository tests for: gap kept, number not reused after deleting the highest, answered question refused, unknown number, 200 limit freed
- [x] 1.3 Add the `DeleteQuestion` use case (participant's group, Active only) and verify with domain tests for Active, Pending and Archived groups and for a Santa who is also a receiver
- [x] 1.4 Add the `DELETE /groups/{id}/receiver/questions/{number}` route (204, no notification) and its route constant, with `QuestionRoutesTest` cases for 204, 404, 409 answered, 409 not Active, the receiver's list losing the question, and answering a deleted question giving 404; run `./kotlin test -m server -p jvm`, `-m domain` and `-m contract`
- [x] 1.5 Update the server's other users of the thread (admin oversight and the group deletion count) only if their tests fail; verify `AdminOversightRoutesTest` and the deletion-impact tests pass unchanged

## 2. App

- [x] 2.1 Add `ApiClient.deleteQuestion` and its test in `GiftApiTest` (204 success, 404 and 409 as failures)
- [x] 2.2 Add the delete state to the Gifter page's view model (pending confirmation, confirm, cancel, reload and notice on success and on each refusal) and verify with view-model tests for every scenario in `client-receiver-and-questions` (delete, cancel, answered meanwhile, already gone or archived, unreachable)
- [x] 2.3 Add the "Delete" action and the confirmation dialog on the Gifter page with the strings and screen-reader labels, shown only for unanswered questions in an Active group; verify with the existing screen or model tests that an answered question and an Archived group have no action, and run `./kotlin test -m shared -p android`
- [x] 2.4 Run `./kotlin build` and confirm it passes apart from the known iOS device code-signing failure

## 3. Checks

- [x] 3.1 Run `openspec validate delete-unanswered-question` and confirm it passes
- [x] 3.2 Manual: with a server and two accounts in an Active group, ask two questions, delete the first, confirm the receiver's list and the numbers (2 stays 2, the next is 3), and confirm an answered question has no "Delete"

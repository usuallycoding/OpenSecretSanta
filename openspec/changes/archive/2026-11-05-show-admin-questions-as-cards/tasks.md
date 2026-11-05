# Tasks

## 1. The pair thread

- [x] 1.1 In `ThreadQuestions` (`OversightSheets.kt`), put each `QuestionThread` in a `Card` with 16dp padding and space the cards 8dp apart, as `WishlistItems` does; verify by hand in the owner's pair sheet on Android, iOS and the web that the questions are cards like the wishlist items above them, with the circle, the indented answer and the line inside, and that a long answer still reads well
- [x] 1.2 Run `openspec validate show-admin-questions-as-cards --strict` and `./kotlin test`; verify validation passes and only the unrelated `ServerAddress` test fails

## Workflow follow-up

- Archive the change after review so `client-admin-oversight` picks up the new sentence and scenario.

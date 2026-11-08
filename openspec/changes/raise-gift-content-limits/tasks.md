# Tasks

## 1. Raise the limits

- [x] 1.1 Set `QuestionLimits.MAX_QUESTIONS_PER_SANTA` and `LetterLimits.MAX_LETTERS_PER_RECEIVER` to 200 and `WishlistLimits.MAX_ITEMS` to 200, keeping the three as separate constants; verify the domain tests that fill a list to its limit (`QuestionsTest`, `LettersTest`, `WishlistUseCasesTest`) pass unchanged, since they read the constants
- [x] 1.2 Reword the KDoc on the question use case (`QuestionUseCases.kt`) and the letter use case (`LetterUseCases.kt`) that says "up to 30" so it points at the constant instead of repeating a number; verify `grep -rn "up to 30" core app server` finds nothing

## 2. Client test double

- [x] 2.1 In `app/shared/test/ui/TestSession.kt` replace the hard-coded `sent.size >= 30` with `LetterLimits.MAX_LETTERS_PER_RECEIVER`; verify no other `30` or `50` stands for one of the three limits in `app/shared/test` or `core/domain/test` (`grep -rnE "\b(30|50)\b"`), and the client tests pass

## 3. Verify

- [x] 3.1 Run the full build and all tests (`./kotlin build` and the test tasks) and verify they pass
- [x] 3.2 Run the app against a server, add the 51st wishlist item to a list and send the 31st letter and ask the 31st question (past the old limits), and verify each succeeds; then check the wishlist with many items and a long question thread scroll and drag smoothly on a device, and note in the change if lazy lists are needed as a separate change

## Workflow follow-up

- Archive the change after review, which syncs the six delta specs into the main specs.

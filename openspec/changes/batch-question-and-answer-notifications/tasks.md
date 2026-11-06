# Tasks

## 1. The words

- [x] 1.1 Make `NotificationTexts.questionAsked` and `questionAnswered` take a count (singular for one, "Your Secret Santa has questions for you in <group>" and "Your receiver answered questions in <group>" for more) and pass an optional `count` (default 1) through `Notifier.questionAsked` and `questionAnswered`, with tests in `NotifierTest` that verify the singular wording for 1, the plural for 2 and 5, that the group name is still the only variable part, and that nothing names a Santa or a question

## 2. The hold

- [x] 2.1 In `NotificationDispatcher` key the held map by (kind, group, person) with a count, route `questionAsked` and `questionAnswered` through the same hold as `wishlistChanged` (same quiet and maximum-wait constants, the count passed to the notifier when the timer fires) and leave the wishlist's behaviour unchanged, with tests in `NotificationDispatcherTest` using the manual scheduler that verify three questions inside a minute make one plural push after the quiet time, one question stays singular, a long run is capped at five minutes after the first of the batch, answers batch the same way, questions and answers and wishlist changes of one person stay apart, two groups and two people stay apart, the recipient is worked out when the timer fires, and a scheduling failure sends nothing and fails nothing
- [x] 2.2 Check the question and answer routes still return as before and that existing question and answer notification tests are updated to the held behaviour (pushes now arrive after the quiet time); verify `QuestionRoutes` responses are unchanged and the whole `server` suite passes

## 3. Docs and check

- [x] 3.1 Update the README's push notifications text to say that questions, answers and wishlist changes that follow one another make one notification each (a minute of quiet, at most five minutes after the first), with a plural text when there are several, and that waiting notifications are forgotten if the server restarts; verify `README.md` says it
- [x] 3.2 Run `./kotlin test` and `openspec validate batch-question-and-answer-notifications --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`

## Workflow follow-up

- Archive the change after trying a burst of questions and a burst of answers on a device.

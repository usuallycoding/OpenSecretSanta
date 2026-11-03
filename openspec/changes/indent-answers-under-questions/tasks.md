## 1. Shared layout

- [x] 1.1 Add a composable in `app/shared/src/ui/gift/` that draws a question with its times and an indented answer slot joined by a line (`outlineVariant`), with "Not answered yet" muted in the slot when there is no answer
- [x] 1.2 Show the question's number in a 24 dp filled circle at the start of the question, drop "N." from its text, start the line at the circle and indent the answer to line up with the question's text
- [x] 1.3 Keep the "Question" and "Answer" labels on the texts and give the line no semantics

## 2. Use it

- [x] 2.1 Make `QuestionCard` use it and keep the action row below it
- [x] 2.2 Make `ThreadQuestions` in `OversightSheets.kt` use it

## 3. Check

- [x] 3.1 Run `./kotlin test -m shared` and `./kotlin build`
- [x] 3.2 Check on a device or the web, light and dark, with a short answer, a six-line answer, a question long enough to wrap, an unanswered question and an Answer action

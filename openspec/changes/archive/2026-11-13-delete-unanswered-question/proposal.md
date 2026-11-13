# Proposal

## Why

A question cannot be taken back once it is asked. A Santa who typed it too fast, asked it twice or no longer wants an answer is stuck with it, and the receiver is left with a question that may confuse or burden them. Withdrawing a question is harmless as long as nobody has answered it yet, so that is when it should be allowed.

## What Changes

- A Santa can delete one of their own questions while the group is Active and the question has no answer. A question that has been answered stays, and so does every question once the group is Archived.
- New endpoint `DELETE /groups/{id}/receiver/questions/{number}`. It names the Santa's relationship, never a username, like the other question routes. It answers 204; 404 when the question does not exist; 409 `invalid_state` when the question has been answered or the group is not Active.
- A question number is never given out twice in one Santa's thread. After a delete the numbers keep their gap (1, 3, 4), and the next question continues after the highest number ever used. A receiver who still sees a deleted question and answers it gets a plain 404.
- The 200-question limit counts the questions that exist, so deleting one frees a place.
- The Gifter page shows a "Delete" action on each unanswered question while the group is Active, behind a confirmation. The receiver's page just stops listing the question.
- The requirement that a question can never be changed or deleted is replaced by this one.
- Out of scope: editing a question, deleting an answered question, deleting an answer, a notice or placeholder for the receiver, recalling a push notification already sent, any admin way to delete.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `santa-questions`: the Santa can delete an unanswered question; question numbers are never reused; the "Questions are not editable" requirement is replaced.
- `client-receiver-and-questions`: the Gifter page offers "Delete" on unanswered questions, with a confirmation.

## Impact

- **Contract** (`core/contract`): a route constant for the delete endpoint; no DTO change.
- **Domain** (`core/domain`): a `DeleteQuestion` use case and a `delete` on `QuestionRepository`.
- **Server**: a new migration adds a per-Santa "last number used" to `assignments`, filled from the existing questions; `SqliteQuestionRepository` uses it to number questions and gains `delete`; one new route.
- **App** (`app/shared`): `ApiClient.deleteQuestion`, the Gifter page's view model and screen (action and confirmation), strings.
- **Compatibility**: the apps in use never call the new route, and they already handle a question vanishing from a list and a 404 on answering. No `ApiVersion` bump.
- **Notifications**: a receiver may already have been told about a question that is then deleted; they find it gone when they open the app.

# Design

## Context

Questions live in `questions`, keyed by `(group_id, santa, number)`, and the number is also the identity used on the wire (`PUT .../santa/questions/{number}/answer`). `ask()` computes `MAX(number) + 1` inside a transaction that first takes SQLite's write lock with the "group is Active" update; `answer()` does the same. So today numbers never repeat only because nothing is ever deleted. See proposal.md for why deletion is wanted.

## Goals / Non-Goals

**Goals:**
- A delete that can never remove an answered question, even against a concurrent answer.
- A number that never means two different questions.

**Non-Goals:**
- Soft deletes or a tombstone the receiver can see; editing; admin deletes.

## Decisions

**1. Numbers come from a counter, not from `MAX(number)`.** `assignments` gets `last_question_number INTEGER NOT NULL DEFAULT 0` (new migration `V017`), filled for existing rows with the highest number in `questions`. `ask()` increments it in the same transaction and uses the result. It lives on `assignments` because that row already is the Santa's thread (`questions` and `read_marks` hang off it and vanish with the group), so no new table or cascade is needed.
- Alternative: keep `MAX+1`. Rejected: after deleting the highest question, a receiver on a stale screen could answer the old number and attach the answer to a new question.
- Alternative: renumber after a delete. Rejected: the number would stop being a stable id and races would get worse.

**2. The delete is one conditional statement.** `DELETE FROM questions WHERE group_id = ? AND santa = ? AND number = ? AND answer IS NULL`, inside the same transaction that takes the write lock through the Active check. If no row was deleted, a follow-up lookup tells 409 (the question exists and is answered) from 404 (it does not). `answer()` already updates by `(group, santa, number)` and answers 404 when no row matches, so the two requests serialise and the second is refused cleanly.
- Alternative: read, check, then delete. Rejected: needs care to avoid a gap between the check and the delete; the single statement does not have one.

**3. Count for the limit stays `COUNT(*)`.** Deleting frees a place with no change to that code.

**4. Layers follow the existing pattern.** A `DeleteQuestion` use case (load the participant's group, require Active, call the repository) and `QuestionRepository.delete(groupId, santa, number)`, throwing `InvalidState` or `NotFound`; one `delete` route next to the other question routes; the route has no notification, so the receiver is not pinged when a question is withdrawn.

**5. Route and compatibility.** The path reuses `GROUP_RECEIVER_QUESTIONS` plus `/{number}`, so no username appears. The apps in use never call it; they already cope with a question missing from a refreshed list and with a 404 when answering. No `ApiVersion` change, since nothing an older app relies on changes.

**6. App.** `ApiClient.deleteQuestion`; the Gifter page's view model gets a pending-delete state and a confirm/cancel pair, modelled on the wishlist's delete confirmation (`confirmDelete`); after a success or a refusal the list is reloaded, and a refusal says why through the page's existing notice. The "Delete" action goes at the bottom of the question card, where "Answer" sits on the Receiver page, so the layout does not move when an answer arrives. The unanswered counts used by badges are computed from the reloaded list and need no change.

## Risks / Trade-offs

- [A push "your Santa asked a question" was already sent for a question that is then deleted] → Accepted: the notification is held and merged and cannot be recalled; the receiver finds the list without it.
- [A receiver sees a question vanish from their list on refresh] → Accepted: deliberate, and a stale screen that answers it gets a 404 that the app already reports.
- [Gaps in the numbering] → Accepted: they are honest and keep the number a safe id.

## Migration Plan

Deploy the server with `V017`; it runs at start-up like the earlier migrations. Existing threads get `last_question_number` equal to their highest number, so numbering continues unchanged. Roll back by redeploying the previous server: the extra column is ignored, and `MAX+1` resumes from the highest number left.

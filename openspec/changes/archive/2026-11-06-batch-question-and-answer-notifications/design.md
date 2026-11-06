# Design

## Context

`NotificationDispatcher` is what the routes call after storing an action. Announcements, the draw, questions and answers are handed to the executor and sent at once; `wishlistChanged` instead keeps a `held` map keyed by (group, person), restarts a timer on each change (60 s of quiet, never more than 300 s after the first) and runs the send when it fires. The held state is in memory. `Notifier` works out the recipient and category and sends; `NotificationTexts` holds the words. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A burst of questions, and a burst of answers, make one notification each, with the wishlist's timing.
- One mechanism for all three held kinds, not a copy.

**Non-Goals:**
- No change to the wishlist timing or text, no persistence of held notifications, no combining of different kinds into one push, and no counts in the text.

## Decisions

- **One hold for three kinds.** The held map's key becomes (kind, group, person), with the kind one of wishlist, question, answer, and each entry also counts how many events it holds. The existing timer rules (quiet, then capped at the maximum wait after the first event) apply to all kinds unchanged, with the same constants. Alternative: a second map for questions; rejected as duplicated timer logic.
- **The count goes to the notifier, the words stay in one place.** When the timer fires the dispatcher calls `Notifier.questionAsked(groupId, santa, count)` or `questionAnswered(groupId, receiver, count)`; the count defaults to 1 so the other callers and tests stay as they are. `NotificationTexts.questionAsked(group, count)` and `questionAnswered(group, count)` return the singular wording for one and the plural for more. The text says only that there are several, never how many, so the wording is the same for everyone and the count adds no information about anyone.
- **Recipient work stays at send time.** The notifier still finds the receiver or Santa when it sends, after the wait, so a draw that changed meanwhile is respected exactly as it is for a wishlist.
- **Scheduling failures behave as the wishlist's do.** If a timer cannot be scheduled the notification is dropped with a log line, not sent at once; for a burst-held kind a lost push is better than a flood. It never fails the request.
- **Kept apart by key.** Because the kind, group and person are all in the key, a person's wishlist changes, the questions they ask and the answers they give each have their own timer, and two Santas asking the same receiver have separate batches that each send one push.

## Risks / Trade-offs

- [A restart forgets held notifications, now also questions and answers] → at most a few minutes of pending pushes are lost; the question or answer itself is stored, and the person sees it on opening the app. Persisting the queue is out of scope.
- [A question is announced a minute late] → the quiet window is short on purpose; a person who wants it sooner can open the app, and the cap keeps a long burst from waiting more than five minutes.
- [Two Santas cannot both ask one receiver, but a receiver can have one Santa only] → batches are per Santa and receiver pair already by the person key, so nothing merges across pairs.
- [Plural wording for a batch of one that was a question and a follow-up] → any batch of two or more is plural, which is true.

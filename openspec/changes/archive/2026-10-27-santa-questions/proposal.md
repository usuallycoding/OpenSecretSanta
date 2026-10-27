# Proposal

## Why

Groups can now be activated and assignments drawn, but no participant can see anything about them: nobody knows who they are buying for, and the Santa has no way to ask their receiver what they would like. This change delivers the participant side of the exchange: each person sees their receiver's details, a Santa can ask questions that the receiver answers (and may later correct), and the Santa is revealed when the group is archived. Throughout, a receiver must not be able to learn who their Santa is before the reveal.

Anonymity also exposed a problem in the activation rule. With exactly 3 participants every person can deduce their Santa from the one fact they already know (their receiver), whatever the draw, so the minimum group size rises to 4, where a participant's Santa is a genuine guess between two people.

## What Changes

- Receiver details: in an Active or Archived group, a participant can read their own receiver's username, full name, address, phone number and delivery information (live values, empty where unset).
- Questions: while the group is Active, a Santa can ask their receiver any number of questions; the receiver sees each and can answer, and edit their answer, while the group stays Active. Both sides can read the whole thread at any time, and it becomes read-only on Archive.
- Anonymity by construction: the receiver's side of the thread and every group response never contain the Santa's identity before the group is Archived; question numbers are per Santa so they leak nothing about other pairs.
- Reveal: once the group is Archived, a participant can read their Santa's username and full name (nothing else about them).
- Admins can still list assignments (unchanged) but get no access to the content of questions and answers.
- **BREAKING**: activation now requires at least 4 participants instead of 3; groups with exactly 3 can no longer be activated. The `README.md` is updated to match.
- Migration `V005` adds the questions table, tied to assignments so that deleting a group removes its questions.

Out of scope: the wishlist, notifications of new questions or answers, deleting or editing a question once asked, client screens, any admin view of Q&A content.

## Capabilities

### New Capabilities
- `gift-visibility`: what a participant may learn about the exchange: their own receiver's details in Active and Archived groups, the Santa reveal only in Archived groups, and the guarantee that nothing before the reveal discloses a Santa.
- `santa-questions`: the question and answer thread between a Santa and their receiver: asking, answering and editing answers, limits, who can read what, and when it is read-only.

### Modified Capabilities
- `group-lifecycle`: the participant minimum for activation changes from 3 to 4.
- `gift-assignment`: the rule that participants cannot see assignments at all is replaced by the narrower rule that a participant sees only their own receiver (and, after archive, their Santa).

## Impact

- Code: new question and visibility use cases in `core/domain`; DTOs and routes in `core/contract`; a question repository, `V005` migration and routes in `server`; `ActivateGroup.MIN_PARTICIPANTS`, its tests and the README change.
- APIs: new `/groups/{id}/receiver`, `/groups/{id}/receiver/questions`, `/groups/{id}/santa/questions...` and `/groups/{id}/santa` routes; `/health` reports schema version 5.
- Dependencies: none.
- Systems: no new environment variables.

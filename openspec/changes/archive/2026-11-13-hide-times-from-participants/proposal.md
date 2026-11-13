# Proposal

## Why

Participants see a small "Added / Changed / Asked / Answered / Sent" time line under every wishlist item, question, answer and letter. For a Secret Santa that is noise, and it can give away more than it should (when a receiver last looked at their list, or how quickly they answer). The times are useful to the group owner when overseeing a group, so they stay there.

## What Changes

- The time lines under wishlist items, questions, answers and letters are no longer shown in the participant view: not on the person's own wishlist, the receiver's wishlist, the Gifter and Receiver question pages, or the Send letter and Inbox pages.
- The rule follows the **view**, not the account: the Admin view (the owner's oversight sheets for a participant's wishlist, a pair's thread and a pair's letters) keeps showing them; an administrator who switches to the Participant view sees none, like everyone else.
- Announcements keep their "Sent" time for everyone.
- The "New", "Edited" and "Unanswered" pills and the read marks are unchanged. They sit where the time line was and keep working from the server's timestamps, not from what is displayed.
- The server and the API are unchanged: it still sends every timestamp, because the Admin view needs them. No API version bump.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-times`: participants no longer see the times; only the Admin view does. The Bucharest-time rule applies to every time the app shows.
- `client-letters`: letter cards on the Send letter and Inbox pages show no sent time.
- `client-read-marks`: the answer and letter pills no longer sit "beside the time".
- `client-receiver-and-questions`: the "Unanswered" pill no longer sits "beside the time it was asked".

## Impact

- Client UI only (`app/shared/src/ui`): `TimesLine` and its callers in the wishlist, question thread and letter screens, plus the admin oversight sheets that must keep showing times.
- Tests that read the time lines on participant screens; read-mark tests must still pass.
- No change to `core`, `server`, the contract DTOs or the API version.

# Proposal

## Why

A group's owner organises the exchange: they create the accounts, hand out temporary passwords and run the lifecycle. Once the group is running they are blind, with no way to tell who has filled in a wishlist or whether a question is stuck unanswered, and nothing to go on when a participant asks for help. The owner can already list the draw (`GET /admin/groups/{id}/assignments`) and read every participant's profile and temporary password, but the two things that make up the exchange itself, wishlists and the question threads, are explicitly closed to admins. The owner is a trusted organiser and may also be a participant, so they should be able to see it all, without leaving any trace for the participants.

## What Changes

- An admin can read, for a group they own, the wishlist of any participant. Available while the group is Pending, Active or Archived.
- An admin can read, for a group they own, every question thread of the group: for each Santa and receiver pair, the Santa, the receiver and every question with its answer and times. Available while the group is Active or Archived.
- Both views are read-only and owner-only (anyone else gets the usual "not found"), return no item ids, and are never recorded: nothing a participant can see changes when the owner reads.
- The owner who is also a participant sees everything, including their own Santa and their own threads, before the group is archived. The server cannot tell the admin and participant views of the same account apart, so the spec states this exception instead of promising a "surprise mode".
- The existing privacy requirements that said "no admin" are narrowed to say "no admin except the group's owner through the oversight endpoints"; participant-facing behavior does not change.
- No migration and no new stored data: the views read what already exists.

Unchanged and already covered: full profile and temporary password of managed users (`user-administration`), password reset, and the owner's list of all assignments (`gift-assignment`).

Out of scope:
- Any admin write to wishlists or threads (edit, delete, answer on someone's behalf), moderation, re-draw, changing the roster of an Active group.
- Notifications, audit logs, a role above the group owner, client screens.

## Capabilities

### New Capabilities
- `admin-oversight`: what a group's owner can read about the exchange inside their own group: participants' wishlists and the Santa-to-receiver question threads, when they are available, who may use them and that reading leaves no trace.

### Modified Capabilities
- `wishlist`: "Wishlist is private" gains the owner exception (read-only, through `admin-oversight`).
- `santa-questions`: "Threads are private to the pair" gains the same exception.
- `gift-visibility`: "Santa never disclosed before archive" and "Nothing about other people's assignments" are scoped to participant-facing endpoints, with the owner's oversight and assignments endpoints as the stated exception.
- `gift-assignment`: "Participants see only their own receiver" gets the same scoping, so an owner who participates is not contradicted by the assignments list they already have.

## Impact

- Code: two read use cases in `core/domain` (owned-group loader, wishlist and question repositories already exist); DTOs and two routes in `core/contract` and `server`; the README.
- APIs: `GET /admin/groups/{id}/participants/{username}/wishlist` and `GET /admin/groups/{id}/questions`. `/health` and the schema version are unchanged.
- Dependencies: none. Systems: no new environment variables.
- Privacy: this is a deliberate widening of who can read personal content. It is limited to the group's owner, read-only and unlogged to participants.

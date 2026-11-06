# Design

## Context

A Santa reads their receiver's side through three existing routes (`/receiver/wishlist`, `/receiver/questions`, `/receiver/letters`), each already carrying the times needed: items have creation and last-changed times, answers have the time given and last changed, letters the time sent. The routes are named by relationship and carry no username; the wishlist spec requires that reading reveals nothing about the reader to the receiver. Questions, letters and their repositories hang off the `assignments` row. The group page already loads the Santa's data in parts and the Gifter page has two tabs sharing one page. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One shared notion of "unread" per Santa, group and kind, the same on every device.
- Counting and marking decided by the server's own times, never the device clock.
- Marks that cannot be seen by anyone but their Santa.

**Non-Goals:**
- No read tracking for the receiver's side, the owner or the questions the Santa asked; no per-item read flags, no notification changes, no undo.

## Decisions

- **One reads route and one marking route, under `/receiver/`.** `GET /groups/{id}/receiver/reads` returns `{wishlist, answers, letters}`, each `{unread, seenUpTo}`; `PUT /groups/{id}/receiver/reads/{kind}` takes `{seenUpTo}` and returns that kind's marks. The group page gets all three counts in one request, and each page gets its baseline from the same route. Alternative: counting on the device from three full reads; rejected as three requests on every group page load, and it would tie the answer to what each device happened to load.
- **The server counts with what it already has.** The use case reads the receiver's wishlist (`WishlistRepository.list`), the Santa's questions (`QuestionRepository.askedBy`) and the letters (`LetterRepository.sentToSanta`), all small and capped, and counts those after `seenUpTo`: items by created or changed time (each once), answers by the time last changed (so a changed answer counts again), letters by sent time. No new queries over the data, and the rules live in domain code that is unit tested.
- **Marks hold a time, not a flag, and the client sends it.** `read_marks (group_id, santa, kind, seen_up_to)` is keyed by the three and hangs off `assignments (group_id, santa)` with `ON DELETE CASCADE`. The app sends the newest time it showed, so an item created between its load and its mark still counts as new. The server keeps the later of the old and the new time and clamps to its own clock, so a bad client can neither move a mark back nor hide the future.
- **Private by omission.** No route but the Santa's reads route returns a mark, and no existing response changes. The receiver's responses and the owner's oversight routes stay as they are, and a privacy test compares every participant response before and after a Santa reads and marks. Nothing here makes reading visible to the receiver, which is what "Reading a wishlist reveals nothing about the reader" asks for.
- **The baseline is fixed for a visit.** Each of the Gifter page's wishlist and questions tabs and the Inbox page gets its baseline when it is opened (from `reads`) and keeps it for the whole visit, so marks survive a pull to refresh and a new arrival is marked too. Re-opening makes a new view model and so a new baseline.
- **Marking happens when the list is shown.** Each view model has a `markSeen()` the UI calls once its list has loaded and its tab is the one shown; it sends the newest time among the shown items and does nothing for an empty list. The questions tab and the wishlist tab are marked apart, so opening the page on one does not clear the other. A failing mark is ignored: it only means the marks come back next time.
- **Pure functions decide the marks.** Over the times and the baseline: an item is new when created after it, edited when created before and changed after, a response or letter new when after it; and the newest time of a list. Times are compared as instants, not as text, since the server's ISO text can differ in its fraction of a second. These are unit tested apart from the screens.
- **The look reuses what exists.** The "New" and "Edited" pills generalise the `UnansweredPill` of the questions; the accent bar is the start-edge bar of `QuestionCard`, drawn behind the card the same way; the badges are `CountBadged` (tertiary), as on "Respond to Santa", with a spoken form "Inbox, 2 new" (and "See Wishlist, 3 new or edited").
- **Archived groups behave as Active ones.** Marks are read and written in both, since a Santa may read after the archive; Draft and Pending are `invalid_state` as for the other reads.

## Risks / Trade-offs

- [The server now stores when a Santa last looked] → only per Santa, group and kind, returned only to them, removed with the group, and pinned by a privacy test; it adds nothing the receiver can see.
- [A client that never marks leaves the badge up] → the badge only reflects reality: changes the Santa has not been shown; marking is sent whenever the list is shown.
- [Three more counts per group page load] → one small request that lists three capped tables; the group page already loads several parts.
- [A changed answer counts again] → by design: the Santa has not read its current text; there is no "edited" pill for answers, only "New".
- [Clamping to the server clock hides a client time a little in the future] → the next real change is later than the server's own time, so nothing is lost.

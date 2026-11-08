# Design

## Context

Each limit is one constant in `core/domain` and is read in one place by the use case, which passes it to the repository as an argument (`maxQuestions`, `maxLetters`, `maxItems`). The server and the apps share these constants, and the UI messages (`letterLimit(max)`, `questionLimit(max)`, `wishlistFull(max)`) take the number as a parameter, so raising a constant changes behavior everywhere with no other code edit. Nothing in the server, the database layer or the notifications depends on the old numbers: a grep found no other hard-coded 30 or 50, no pagination and no request-size cap.

The apps render the lists as plain `Column`s inside one scrolling page, not as lazy lists. The `group-pair-pages` design chose that because "a list holds at most 50 items and a thread at most 30 questions, so nothing needs to be lazy", and `replace-wishlist-arrows-with-drag-reorder` kept the wishlist a hand-rolled `Column` because a reorder library would need a `LazyColumn` nested in the page's scroll. See proposal.md for why the limits are rising.

## Goals / Non-Goals

**Goals:**
- Change the three limits to 200 with no other behavior change.
- Leave no copy of the old numbers in comments, specs or test doubles that could drift from the constants.

**Non-Goals:**
- Making the lists lazy, or reworking `PullPage` scrolling or the wishlist drag measurement. 200 is a ceiling, not a typical size, and a reflow of the page is far larger than this change.
- Changing the character length caps, notifications, the API or stored data.
- Pagination of the question, letter or wishlist responses.

## Decisions

**Three separate constants stay three constants.** `QuestionLimits.MAX_QUESTIONS_PER_SANTA`, `LetterLimits.MAX_LETTERS_PER_RECEIVER` and `WishlistLimits.MAX_ITEMS` each become 200. One shared constant would be less to read, but the three belong to different features and can sensibly diverge again.

**The lists stay non-lazy.** The earlier designs assumed 50 and 30, and 200 is 4 to 7 times that, so the assumption no longer holds as written. It is accepted rather than redesigned: at 200 the first render is heavier, most of all for the wishlist, whose rows each measure themselves and animate during a drag, and the long question and letter texts make those lists tall. If a device shows it is slow, lazy lists become a separate change. The decision is checked on a device after this change, not assumed.

**The client test double follows the real constant.** `TestSession.kt` refuses a letter with its own `sent.size >= 30`; it reads `LetterLimits.MAX_LETTERS_PER_RECEIVER`, so the client tests exercise the real limit and cannot drift from it.

**Comments that state the number are reworded to point at the constant.** The KDoc on the question and letter use cases says "up to 30"; it says the limit comes from the constant, so changing the constant never leaves a wrong comment.

## Risks / Trade-offs

- [200 rows render slowly on an older phone, or the web and iOS builds] → Check the wishlist (the heaviest, with its drag handling) and a long question thread on a device after the change, and open a separate change for lazy lists only if they feel slow.
- [A 200-item wishlist is awkward to reorder by dragging, because a drag has to scroll the page a long way] → The drag already scrolls the page at its edges and each row also has move up and down buttons; no change is planned.
- [More stored text per group] → A group has tens of participants, and the length caps are unchanged, so the worst case is bounded and small next to the rest of the data.

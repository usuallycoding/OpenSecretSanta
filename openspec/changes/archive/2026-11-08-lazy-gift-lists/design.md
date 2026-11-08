# Design

## Context

Every page is a `PullPage`: a `verticalScroll` column inside the pull-to-refresh, with the page's content as a `ColumnScope` lambda. The five lists are sections composed inside that lambda, each emitting its own `Column` (a notice, a header, then the cards in a `for` loop), wrapped by `LoadSection`, which shows progress, the failure with "Try again" or the content. Each list is on a page of its own (a tab, or a column on a wide window), and the call sites in `GroupPages.kt` pass the section's own loading flag and refresh to the `PullPage` around it. The groups, admin groups and admin users lists are already `LazyColumn`s, with `rememberLazyListState()` and `ForwardOutsideScroll(listState)`, inside the same pull-to-refresh. See proposal.md for why.

A `LazyColumn` takes a `LazyListScope` builder, which is not composable: it cannot read a view model's state, run an effect or use `LoadSection`. A section therefore has to read its state and run its effects first and hand the rows to the page, so the section and its page cannot stay two separate layers.

## Goals / Non-Goals

**Goals:**
- Compose only the rows on screen for the five lists, with no visible change.
- Reuse the existing pull-to-refresh, bottom space and scroll-beside-a-page registration.

**Non-Goals:**
- The person's own wishlist: its drag-to-reorder measures every row (`LocalPageScroll`, the `tops` and `heights` maps), so it stays on `PullPage`, which keeps providing `LocalPageScroll`.
- The admin pages, the group page, paging the server's lists, or changing any view model.

## Decisions

**A section owns its page.** Each of the three sections reads its state, runs its effects (reload on resume, mark as seen, the ask or answer sheet) and then calls `LazyPullPage` itself, building the items inside it. The loading flag and refresh the call site used to pass are the ones the section already has. A call site becomes `ReceiverQuestionsSection(questions, shown, bottomSpace = fabClearance())`, still inside the `Box` that holds the floating button; the Outbox and Inbox already own their `PullPage` and only swap it. Alternative rejected: keep the sections inside a page and make the page lazy through a scope parameter. The state reading and the effects cannot be done in the builder, so each section would split in two anyway.

**`LazyPullPage` mirrors `PullPage`.** It takes `loading`, `onRefresh`, `bottomSpace` and a `LazyListScope` content, wraps a `LazyColumn` in the same `PullRefresh` and provides `LocalPullActive`, with the same padding and 12dp spacing. It creates its `LazyListState` and calls `ForwardOutsideScroll(state)`, so scrolling beside a page keeps working. It does not provide `LocalPageScroll`, which only the wishlist uses.

**A lazy counterpart of `LoadSection`.** A `LazyListScope` function takes the `LoadState`, the retry and a `LazyListScope.(T) -> Unit` for the loaded rows. While there is nothing to show it adds one item with the spinner or the failure and retry; with a value it adds the thin loading bar (an item, left out during a pull, as `Loaded` does) and the error line if a reload failed, then the loaded rows. `LoadSection` stays for the sections that are not lazy.

**Notices, headers and empty texts are items; cards are keyed items.** The notice, the thread-closed or limit line and the "none yet" text become `item { }` blocks. Questions and letters use `items(list, key = { it.number })`, the keys the code already uses for `key(...)`. The receiver's wishlist has no id and keeps the position as its key, which is stable because the list is read-only and replaced whole on a refresh.

**Spacing is one 12dp gap between all items.** The old nesting had 8dp between a notice or header and the list and 12dp between cards. The one change is that the notice and the thread-closed line sit 12dp above the first card instead of 8dp.

**Effects and sheets are unchanged.** Marking as seen is tied to the page being shown and loaded, not to rows on screen, so a lazy list marks the same letters, answers and wishlist changes as before. The floating buttons, the ask, answer and letter sheets, and their view models are not touched. No new automated test is added: the view-model tests do not depend on how the list is laid out, and the lazy rendering is checked on a device (see tasks).

## Risks / Trade-offs

- [A lazy list can lose its scroll place, or reload, when its tab is left and shown again] → `rememberLazyListState` survives the way `rememberScrollState` did; check on a device that a tab keeps its place and that a reload on resume does not jump.
- [A zero-height item from the loading bar adds a 12dp gap during a quiet reload] → Only the quiet reload shows the bar, and it is gone when the reload ends; accepted unless it looks wrong on a device.
- [The 8dp to 12dp change above the first card] → Accepted as a small, deliberate simplification; revert to an explicit 8dp spacer if it looks off.
- [No automated test covers the lazy layout] → The device check in tasks covers each page and the wide layout; the view-model tests still run.

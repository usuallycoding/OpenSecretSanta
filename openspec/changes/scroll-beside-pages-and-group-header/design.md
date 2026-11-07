# Design

## Context

On a wide window the shell puts every page that is not a "spread" page (the two-column Giving and Receiving pages and the owner's group screen) in a box at most 640dp wide, centered; the Scaffold is itself capped at 640dp between 640 and 840dp. The scrolling lives inside each page (`PullPage`'s column, three `LazyColumn` lists, a few plain scrolling columns), so the empty space beside a page belongs to a container with no scroller. The group page's header is `ReadOnlyText(group.name, headlineSmall, ...)` and `GroupInfoCard` draws the description as plain text and then one `SnowCard` for the date, place and budget.

## Goals / Non-Goals

**Goals:**
- A wheel or a drag beside a page scrolls it, one mechanism, with no change to how pages are laid out.
- The description in its own card and the name centered, on both group screens, from one shared place where there is one.

**Non-Goals:**
- Making the pages wider, moving the scrollers to full width, or changing the sidebar, the bars or the two-column pages.
- Centering the state line and its note.

## Decisions

**Forward the scroll, don't move the scrollers.** Moving every scroller to full width (with the content limited inside) is about ten containers, and cannot help between 640 and 840dp, where the bars themselves are capped. Instead one `scrollable` sits on a full-size transparent box behind the Scaffold, and it forwards what it receives to the page that is shown.
- A small `OutsideScroll` host holds the scroll states of the shown screens, in the order they appeared; the last one is the target. It is provided by the shell through a composition local, `LocalOutsideScroll`, which is null outside the signed-in app.
- A composable `ForwardOutsideScroll(state: ScrollableState)` registers a scroller with the host while it is shown (a `DisposableEffect`) and takes it out when it leaves, so a screen that closes hands the target back to the one before. `ScrollState` and `LazyListState` are both `ScrollableState`s, so one registration works for every kind of scroller.
- The shell draws two transparent boxes with the same `Modifier.scrollable(state = rememberScrollableState { delta -> host.scrollBy(delta) }, orientation = Vertical, reverseDirection = true)`: one behind the Scaffold, for the space beside the capped column between 640 and 840dp, and one inside the Scaffold's content, behind the page, for the empty space on a wide window. The Scaffold is a Material `Surface`, which takes every pointer event over its whole area, so a box only behind it would never be reached there. The combination `verticalScroll` uses so that the direction and amount match. The wheel, a trackpad and a touch drag all reach `scrollable`, which also gives the fling after a lift. The direction and amount are checked against the page's own scrolling.
- Each is a sibling **behind** what is in front of it (the Scaffold, or the page), not an ancestor of the pages. Compose hands a pointer event to the front-most sibling that handles it, so over a page (its scroller covers its area) the page's own scroller takes the gesture and nothing is doubled, and a page that reaches its end does not pass leftover movement on to this box or to another page. Only where nothing in front handles pointer input does the box behind get the gesture: the empty space beside the page, and between 640 and 840dp the space beside the capped column.
- The top and bottom bars are part of the Scaffold's surface, which takes the events over them, so a wheel or a drag over a bar scrolls nothing; only the empty space beside a page does. That is accepted.
- Two pages shown together (the spread pages) each register; the last wins, and as they fill the window there is no empty space for anyone to scroll in, and a page's own scroller always takes the gesture over it first.

**Registration is one line in each scroller.** `PullPage`, the lists of groups, admin groups and users, Settings, a user's page and the rules page call `ForwardOutsideScroll(state)` next to where they create their scroll state (`rememberScrollState()` or `rememberLazyListState()`). The lists that used an unnamed `LazyColumn` get a named state. The short empty-state messages do not register: they have nothing to scroll, which they keep only so that a pull still reloads.

**The description card.** `GroupInfoCard` draws, in order, a `SnowCard("group-description")` holding the description (`ReadOnlyText` with its "Description" label and the card's 16dp padding) when there is one, then the existing information card. Both group screens already call it, so both change at once. The card is a `SnowCard` like the others, so it has the snowflake pattern and the same look. A group without a description draws no card.

**The centered name.** `ReadOnlyText` already has a `centered` option; the two headers pass `centered = true` for the name, and the name keeps its single row with an ellipsis and its copy-on-tap behaviour. The state line and note are not touched.

## Risks / Trade-offs

- [The wrong page is the target while screens change] → the registry is ordered and entries are removed when a screen leaves, so the newest shown page is the target; checked by a unit test of the host and by eye while navigating.
- [The box behind also gets events over a page where the page has nothing that handles pointer input] → a page's scroller covers its whole area, so this does not happen; found by trying it if it does.
- [The sign of the forwarded delta is wrong, scrolling the wrong way] → verified against the page's own scrolling in a test of the host's arithmetic and by eye on a wheel and on a touch drag.
- [A long description makes the description card tall] → the card shows the description as `ReadOnlyText` does now, with its maximum rows and ellipsis, and a tap opens the whole text as before.
- [A scroller that is not registered] → it simply does not respond beside its page, as today; the list of scrollers is part of the tasks.

## Migration Plan

None: no data, API or setting changes.

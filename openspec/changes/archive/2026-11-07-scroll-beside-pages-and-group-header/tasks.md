# Tasks

## 1. Scrolling beside a page

- [x] 1.1 Add `OutsideScroll` (an ordered list of the scroll states shown, the last one the target, with a function that passes a delta to the target and says how much it used), `LocalOutsideScroll` and `ForwardOutsideScroll(state)` to `ui/layout`, and unit tests: the last registered is the target, removing it hands the target back, no target uses nothing, and a delta moves a fake scroll state in the page's own direction.
- [x] 1.2 In `ShellScreen.kt` create the host, provide it, and draw a transparent box with the `scrollable` that forwards to it behind the Scaffold, over the same area, so that it takes the gestures where nothing in front does: the empty space on a wide window and between 640 and 840dp.
- [x] 1.3 Register every scroller: `PullPage`, the lists of groups, admin groups and users (naming their `LazyListState`), Settings, a user's page and the rules page (the short empty-state messages have nothing to scroll and do not register).
- [x] 1.4 Try it by wheel and by drag in the empty space on a wide window and between 640 and 840dp: the page scrolls once over itself, a page that fits does not move, and the Giving and Receiving columns are unchanged.

## 2. The group header

- [x] 2.1 Show the group's name centered on the group page and on the owner's group screen (`centered = true`), leaving the state and its note as they are.
- [x] 2.2 Draw the description in a card of its own in `GroupInfoCard` above the information card, with its "Description" label, and nothing when there is no description; update the comment that says it is plain text.

## 3. Verification

- [x] 3.1 Build for Android, iOS and the web, run the shared tests and `openspec validate --specs`.
- [x] 3.2 Look at the group page and the owner's group screen with and without a description, a long description, and a long name, in both themes, and at scrolling beside the pages on a wide window.

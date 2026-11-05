# Design

## Context

The wishlist is a section of the Receiver page, drawn by `WishlistSection` as a plain `Column` of `ItemRow` cards (at most 50), inside the page's own scroll. It is shared Compose Multiplatform code for Android, iOS and wasmJs, so one gesture implementation covers touch and mouse. `WishlistViewModel.move()` builds the full id list, sets `ui.moving`, calls `reorderWishlist`, and replaces the list with the order the server returns; a refused move goes through `failed(...)`, which reloads on a 400 naming `itemIds`. The core icon set has no drag-handle icon, and `SwapIcon` shows the project's way of drawing one. Pull-to-refresh already exists on the page. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Drag to reorder from a handle, on touch and mouse, in the existing `Column`.
- One request per drag, and the server's order stays authoritative.
- Reordering still possible without a pointer.

**Non-Goals:**
- Long-press-anywhere dragging, drag between groups, or multi-select moves.
- Changing the server, the API, or the 50-item limit.
- Moving the list into a `LazyColumn`.

## Decisions

**Hand-rolled drag in the `Column`, no library.** `sh.calvin.reorderable` would need a `LazyColumn`, which nested in the page's scroll means restructuring the page. With 50 items a `Column` is fine. Alternative rejected: the library plus a page restructure; more invasive than the gesture it saves.

**Handle-only drag with `detectDragGestures` on the handle.** The handle consumes its own pointer events, so swiping the card body scrolls the page and a downward pull near the top does not trigger pull-to-refresh. It starts at once (no long press), which works the same for a mouse. Alternative rejected: long-press on the whole card, which competes with scrolling on touch and is undiscoverable with a mouse.

**Drag state is local to the list, committed on drop.** The list keeps the dragged item's id, its pointer offset, and a working order. Each row's top and height come from `onGloballyPositioned`, since cards differ in height. The dragged card follows the pointer with `graphicsLayer { translationY }`; when its centre passes a neighbour's midpoint the working order swaps them and the offset is corrected by the swapped height. Other rows animate to their new slots. Only on drop, if the order differs from the start, is the ViewModel called.

**ViewModel gets a move to an index; the one-step moves use it.** A new `moveTo(itemId, index)` builds the id list and goes through the same path as `move()` (the `moving` flag, `reorderWishlist`, replace with the server's list, `failed(...)`). `moveUp` and `moveDown` become `moveTo(id, from -/+ 1)`, so their behavior and tests are unchanged. A failed save simply leaves the loaded list untouched, and discarding the working order makes the list snap back.

**Auto-scroll by dragging the page's scroll state.** While dragging, when the pointer is within a band at the top or bottom of the visible area, a loop scrolls the page at a speed growing with depth into the band. The Receiver page must hand the wishlist its scroll state (or a scroll-by callback) and the viewport bounds; the wishlist adjusts the dragged card's offset by the scrolled amount so it stays under the pointer.

**Screen-reader actions via semantics.** Each row gets `customActions` "Move up" and "Move down" (the existing `Strings.MOVE_UP`/`MOVE_DOWN`), present only when the move is possible and not saving, calling `moveUp`/`moveDown`. The handle gets a content description for the drag.

**Handle icon drawn locally.** An `ImageVector` built like `SwapIcon` from the Material "drag indicator" path, since the core icon set lacks it and adding the extended icon set is far larger than one path.

**One number circle for questions and items.** The circle in `QuestionThread.kt` (24dp, `secondaryContainer`, `labelMedium` number) becomes a shared `NumberBadge(number)` in `ui/form`, used by the question thread and by items, so the two cannot drift apart. `ItemText` is already what all three places draw an item with, so it takes the item's 1-based position and puts the circle before its text column; the three call sites pass the index. The title is labelled "Item N" for screen readers, as a question is "Question N". Alternative rejected: numbering in each call site, three layouts to keep equal.

**Live numbers while dragging.** The rows keep their resting places during a drag, so a row's index is not what it shows. A pure function gives the number a row shows from its index, the dragged row and the target slot: the dragged row shows the target's place, the rows it has passed show one more or one less, the rest keep theirs. That is the same rule `rowShift` follows, so the numbers change exactly when the rows move. Once the server answers, the list has the saved order and the numbers are plain indexes again.

## Risks / Trade-offs

- [The circle takes about 32dp of the card's width, so item text wraps a little earlier, most on a narrow phone] → Titles are one row with a dialog for the whole text, so only notes wrap; check a long note on a small screen.

- [Auto-scroll and drag offset can drift or jitter] → Keep the offset in absolute list coordinates and derive the on-screen position from it; verify on Android, iOS and web by hand with a 50-item list.
- [Mouse and touch behave differently on wasm (e.g. touch scrolling in mobile browsers)] → Test the web build in a desktop browser and on a phone browser; the handle consumes its events so the page should not scroll under it.
- [Gesture math is hard to test through the UI] → Put the swap-point and auto-scroll-speed calculations in plain functions with unit tests, as `GestureTracker` is.
- [Drag is a pointer-only interaction] → The screen-reader actions keep reordering accessible.
- [A drop races a refresh or another device's change] → Unchanged: the server refuses a stale list with 400 `itemIds` and the app reloads.

## Migration Plan

Client-only; ship with the next app and web release. No data or API migration. Rollback is reverting the client change.

# Tasks

## 1. Reorder to an index in the view model

- [x] 1.1 Add `moveTo(itemId, index)` to `WishlistViewModel` going through the existing move path (`moving` flag, one `reorderWishlist` call, server order shown, failures via `failed`), make `moveUp` and `moveDown` use it, and verify the existing `WishlistViewModelTest` move tests still pass
- [x] 1.2 Add `WishlistViewModelTest` cases for `moveTo`: first to last, last to first, same index sends nothing (returns null), unknown id or out-of-range index returns null, and a refused move reloads and shows the changed-elsewhere notice; verify they pass

## 2. Drag logic

- [x] 2.1 Add plain functions for the working order while dragging (which slot the dragged card is over given row tops and heights, and the resulting id order) and for the auto-scroll speed in an edge band, in `app/shared/src/ui/wishlist`, with unit tests for uneven heights, the ends of the list, and the edge band; verify with `./kotlin test` for the shared module
- [x] 2.2 Add a drag-handle `ImageVector` drawn like `SwapIcon` and a `Strings` label for the handle; verify it compiles and the handle shows in the item row

## 3. Item row and list

- [x] 3.1 Replace the up and down buttons in `ItemRow` with the drag handle next to the edit button; the handle is shown only while the group is editable and is disabled while `ui.moving`; verify no arrow buttons remain and the Archived list shows neither handle nor edit button
- [x] 3.2 Implement the handle's `detectDragGestures` in `WishlistSection`: the dragged card follows the pointer, others make room, and drop calls `moveTo` only when the order changed; cancelling or a failed save leaves the list in the server's order; verify by hand on Android, iOS and the web build (mouse and phone browser) that swiping the card body scrolls and pull-to-refresh still works
- [x] 3.3 Add auto-scroll near the top and bottom edges by passing the Receiver page's scroll state or scroll-by callback to the wishlist, keeping the dragged card under the pointer; verify a 50-item list can be reordered from the last position to the first in one drag
- [x] 3.4 Add the "Move up" and "Move down" semantics custom actions on each row, present only when possible and not saving; verify with TalkBack or VoiceOver (or a semantics test) that they reorder and that the first and last rows lack one each

## 4. Specs and docs

- [x] 4.1 Run `openspec validate replace-wishlist-arrows-with-drag-reorder --strict` and fix anything it reports
- [x] 4.2 Check README and any user-facing help for mentions of the move arrows and update them; verify with a search for "move up", "arrow" and "reorder"

## 5. Numbered items

- [x] 5.1 Move the number circle out of `QuestionThread.kt` into a shared `NumberBadge(number)` in `ui/form` and use it there; verify the question thread looks the same on the Gifter and Receiver pages and the existing tests pass
- [x] 5.2 Give `ItemText` the item's 1-based position, draw the circle before the text and label the title "Item N"; pass the index from `WishlistSection`, `ReceiverWishlistSection` and the oversight wishlist sheet; verify by hand that all three places show 1, 2, 3 in order, and that a long note on a narrow screen still reads well
- [x] 5.3 Add a plain function for the number a row shows while an item is dragged (the dragged row shows the target's place, the rows it passed shift by one, the rest keep theirs), use it in `ItemList`, and unit-test it in `DragReorderTest`; verify by hand that the numbers swap as items pass each other and are the server's order after the drop
- [x] 5.4 Update the README's wishlist paragraph to mention the numbers, run `openspec validate replace-wishlist-arrows-with-drag-reorder --strict` and `./kotlin test`; verify the validation passes and only the unrelated `ServerAddress` test fails

## Workflow follow-up

- Archive the change after review so `client-wishlist` picks up the new requirements.

# Proposal

## Why

Reordering a wishlist means tapping the up or down arrow once per position, and moving an item from the bottom of a 50-item list to the top is tedious. Dragging an item to where it belongs is the familiar way to do this, and it should work the same on Android, iOS and the web app.

## What Changes

- Each wishlist item gets a drag handle in place of the visible move up and move down buttons. Dragging the handle moves the item; dropping it saves the new order.
- Dragging works with touch and with a mouse, and only from the handle, so scrolling the page and pull-to-refresh are unaffected.
- The list near the top or bottom edge of the screen scrolls while an item is being dragged, so long lists can be reordered in one drag.
- A drag is one save: the complete list of ids in the new order is sent once, on drop. A failed save puts the list back in the order the server has.
- Move up and move down remain available to screen readers as custom actions on each item, so reordering does not depend on a pointer.
- Each wishlist item shows its place in the list, from 1, in a small filled circle at its start, like the questions: on the person's own list, on the Gifter page's read-only list of the receiver, and in the owner's wishlist sheets. While an item is dragged the numbers follow the items as they swap places.
- The circle is taken out of the question thread and shared, so questions and wishlist items look the same.
- No server, API or dependency changes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-wishlist`: "Reordering items" is replaced by "Reordering items by dragging" and a new "Reordering with a screen reader" keeps move up and move down as accessibility actions; "Editing an item" no longer describes the edit button as sharing a column with the move buttons; and a new "Wishlist items are numbered in a circle" covers the numbers wherever an item is shown (own list, Gifter page, owner sheets).

## Impact

- `app/shared/src/ui/wishlist/WishlistScreen.kt`: item row layout, drag gesture, auto-scroll, accessibility actions.
- `app/shared/src/ui/wishlist/WishlistViewModel.kt`: a move to an arbitrary position alongside the existing one-step moves.
- `app/shared/src/ui/Strings.kt`: a label for the drag handle.
- A drag-handle icon drawn in the app, as `SwapIcon` is, since the core icon set has none.
- `app/shared/src/ui/gift/QuestionThread.kt`: its number circle moves to a shared composable. `ItemText` (in `WishlistScreen.kt`) draws the circle for an item, so `ReceiverWishlistScreen.kt` and `OversightSheets.kt` get it through their existing calls.
- Shared code, so Android, iOS and wasmJs all change together. Tests in `app/shared/test/ui`.
- `openspec/specs/client-wishlist` at archive time.

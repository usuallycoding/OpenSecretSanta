# Proposal

## Why

On the person's own wishlist the whole item card opens the edit sheet when tapped, so there is nothing that says an item can be edited, and a tap on a cut title or note cannot both show the full text and edit. Titles also wrap onto several lines, which makes the list uneven.

## What Changes

- A wishlist item's title is shown on one row, with an ellipsis when it is longer; choosing a cut title opens the dialog with the whole title, as for other cut text. This holds wherever an item is shown: the person's own list, the Gifter page and the owner's wishlist sheet.
- An item on the person's own list ends with an edit (pencil) button, in one column with the move up and move down buttons. The button opens the edit sheet.
- The item card no longer opens the edit sheet when tapped; only the pencil does.
- The edit sheet, deletion, reordering and the rules for when an item can be changed are unchanged: the pencil is shown only where the move buttons are (Pending or Active).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-wishlist`: an item is edited with its edit button, not by choosing the item.
- `client-app`: a wishlist title is cut to one row, not three; on the person's own list the rest of the item no longer opens it for editing.

## Impact

- `app/shared/src/ui/wishlist/WishlistScreen.kt` (`ItemRow`, `ItemText`), `ReceiverWishlistScreen.kt` and `admin/OversightSheets.kt` (they use `ItemText`), `Strings.kt`, the README.
- No server, API or data changes.

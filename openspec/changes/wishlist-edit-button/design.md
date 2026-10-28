# Design

## Context

`ItemRow` in `WishlistScreen.kt` draws the person's own item as a `Card(onClick = onEdit)` holding `ItemText` and, while the list is editable, a column with the move up and move down `IconButton`s. `ItemText(title, note, url)` is shared with the Gifter page (`ReceiverWishlistScreen`) and the owner's wishlist sheet (`OversightSheets`), and draws the title with `ReadOnlyText(... titleMedium)` at the default three-line limit. `ReadOnlyText` already has a `maxLines` parameter and opens the full-text dialog for cut text (used for the group name with `maxLines = 1`).

## Goals / Non-Goals

**Goals:**
- A one-row title everywhere an item is shown.
- A visible edit button in the column of the move buttons, and no card tap.

**Non-Goals:**
- Changing the edit sheet, deletion, reordering or the server.
- Making titles editable inline.

## Decisions

1. **Title limit in `ItemText`.** Pass `maxLines = 1` for the title only, so the three places that show an item change together. The note keeps the three-row limit. A cut title opens the full-text dialog; the copy icon stays as it is for read-only text.
2. **Card not clickable.** `ItemRow` always uses the plain `Card`; the `if (editable) Card(onClick…)` branch goes. With no card tap, a tap on a cut title or note can only mean "show the full text", which removes the earlier conflict.
3. **Edit button.** A third `IconButton(Icons.Filled.Edit)` in the same `Column`, above the arrows, shown only `if (editable)` (the same condition as the arrows), with the content description "Edit item". The existing sheet entry point `viewModel.openEdit(item.id)` is reused.
4. **Column height.** Three default 48dp buttons make a card at least 144dp high. To keep cards compact the three buttons use `Modifier.size(40.dp)` so the column is 120dp; the touch target stays above the 36dp that Material allows for dense icon buttons. Cards of items with a short body therefore grow to the height of the column.

## Risks / Trade-offs

- Cards are taller than before, even for a title-only item. Accepted, because the user asked for the pencil in the column with the arrows.
- A person used to tapping the card to edit must now use the pencil; the pencil is the stated way to edit.

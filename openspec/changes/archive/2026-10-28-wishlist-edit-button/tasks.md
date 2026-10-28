# Tasks

## 1. One-row titles

- [x] 1.1 Give the title in `ItemText` a one-row limit (the note keeps three rows) so it applies on the person's own list, the Gifter page and the owner's wishlist sheet; verify a long title shows an ellipsis and opens the full title in a dialog in each place

## 2. Edit button

- [x] 2.1 Make the item card not clickable, add the edit button (pencil, "Edit item") in the column of the move buttons while the list is editable, sized so the column stays compact, and open the existing edit sheet from it; verify in headless Chrome that the pencil opens the sheet, tapping the card does nothing, an Archived list shows no pencil, and a cut title or note opens its dialog

## 3. Check and document

- [x] 3.1 Update the README (the pencil replaces tapping an item), run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (phones)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

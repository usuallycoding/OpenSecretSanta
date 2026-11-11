# Proposal

## Why

Choosing a wishlist link first opens a dialog with the whole link and "Open in browser" and "Cancel" buttons. Someone who taps a link wants the page, so the dialog is an extra step on every link, and with up to 10 links per item the extra step is repeated often.

## What Changes

- Choosing a wishlist link opens it straight away in the platform's browser or default handler. The link dialog, with the whole link and its "Open in browser" and "Cancel" buttons, is removed.
- A link is still one line in the link colour, cut with an ellipsis when it is long, and still has its copy icon, which copies the whole link.
- A link is read out as a link ("Open link in browser") by a screen reader.
- Unchanged: only the `http` and `https` addresses the server accepted are shown as links, and when nothing can open a link nothing happens.
- **BREAKING** (behaviour): the whole link is no longer shown before the person leaves the app; it can still be copied. See design.md.
- The dialog's code and its two strings, "Link" and "Open in browser", are removed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-wishlist`: "Links open in a dialog" is replaced by links opening directly in the browser, with the row staying one line with its copy icon.
- `client-admin-oversight`: the owner's participant wishlist opens links directly, and "Wishlist links open the link dialog" goes away.

## Impact

- **App** (`app/shared/src/ui/form/ReadOnlyText.kt`): `LinkText` opens the link on choosing it; `LinkDialog` is removed. `Strings.WISHLIST_LINK` and `Strings.OPEN_IN_BROWSER` are removed.
- Every place that shows a wishlist link goes through `ItemText`, so the person's own list, a Santa's view of the receiver's list and the owner's participant wishlist all change together.
- No server, contract, database or domain change.
- Out of scope: showing the host or the middle of a long link, wrapping the link, a confirmation for unfamiliar sites.

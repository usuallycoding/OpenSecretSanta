# Proposal

## Why

Text that other people wrote (a wishlist note, a user's address and delivery information, a question) is shown in full wherever it appears, so one long note pushes a whole screen down, while a link is cut to one line and cannot be read in full without opening it. There is also no way to copy anything out of the app: an address for a parcel label, a link for a message, a question to quote. Long text should take a bounded space and open in full on request, any read-only text should be copyable with one tap, and a link should be shown whole before it takes the person out of the app.

## What Changes

- **Long text is cut at six lines.** Read-only text shows at most six lines, followed by an ellipsis when there is more. Tapping text that was cut opens a dialog with its full text. Text that fits does nothing when tapped.
- **A copy icon ends every read-only value.** At the end of the read-only text a small copy icon puts that text on the clipboard and turns into a check mark for about a second. It is on: the username headings and the profile fields of a user (name, email, phone, address, delivery information), the same fields of a receiver, a wishlist item's title, note and link, a question and its answer, a group's description, the Santa reveal and the temporary password. Labels, screen titles, status words and buttons do not get one.
- **A link opens a dialog first.** Choosing a wishlist link no longer leaves the app at once. A dialog shows the whole link, wrapped, with an "Open in browser" button and a "Cancel" button. This applies wherever a link is shown: the person's own list, the receiver's list and the owner's oversight sheets.
- **Unchanged.** What is stored, the server and the editing sheets; tapping an item on the person's own list anywhere but on its cut-off text still opens the item to edit.

Out of scope:
- Copying text that is being edited (the fields already have the platform's own selection), a "copied" confirmation beyond the icon, sharing, and changing the six-line limit of the editable fields.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-app`: long read-only text is cut and opens in full; read-only text can be copied.
- `client-wishlist`: a link opens a dialog with its full text and the choice to open it, replacing "a link opens in the browser".
- `client-admin-oversight`: a link in the owner's wishlist sheet opens the same dialog.

## Impact

- Code: new shared composables in `app/shared/src/ui/form` (read-only text with its copy icon and full-text dialog, the link dialog, a drawn copy icon), their strings, and their use in the wishlist items, question cards, user detail, receiver, Santa reveal, group description and the oversight sheets. No server, API or dependency change.
- Platforms: Android, iOS and web. On the web the clipboard only works on a secure page (`https` or `localhost`); the README says so.

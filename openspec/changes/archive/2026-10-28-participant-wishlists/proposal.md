# Proposal

## Why

A wishlist is what lets a Santa choose a gift without asking, and the server already supports it end to end: each participant keeps a list per group, can add, edit, delete and reorder items while the group is Pending or Active, and their Santa can read it once the group is Active. The app has no screen for any of it, so nobody can say what they would like and nobody can see what their receiver would like. This is the next step after the groups and profile screens: the group detail is where it starts.

## What Changes

- **Wishlists section on the group detail.** "My wishlist" is always there; "Your receiver's wishlist" appears once the group is Active or Archived. While the group is Pending the section says the receiver's list appears after the draw.
- **My wishlist.** The person's items in their own order, each with title, note and link. They can add an item, edit it, delete it (after a confirmation) and move it up or down with buttons. Adding and editing happen in a bottom sheet over the list. A list holds at most 50 items; at 50 adding is unavailable with an explanation. Once the group is Archived the list is read-only and every editing control is gone.
- **Your receiver's wishlist.** The items the person's receiver keeps for that group, read-only, in the receiver's order, with no way to tell the receiver that it was read. It carries no name; who the receiver is arrives with the receiver details in a later change.
- **Links.** A link opens in the platform's browser. Only links the server accepted (`http` or `https`) are ever shown.
- **Rules the app shares with the server.** Before anything is sent, the form checks what the server checks (title 1 to 200 characters, note up to 1000, link up to 2000 and a web address), and the server's field errors appear next to their fields.
- **Concurrent changes.** If the list was changed elsewhere while the person was reordering, the app reloads it and says so. If the group was archived or an item deleted meanwhile, the app closes the sheet, reloads and says what happened.

The server needs no change.

Out of scope:
- Who the receiver is (name, address) and the questions between Santa and receiver: the next change.
- Drag and drop reordering, timestamps ("updated 24 Dec"), notifications, marking an item as bought and all admin screens.

## Capabilities

### New Capabilities
- `client-wishlist`: how a participant keeps their own wishlist in the app (add, edit, delete, reorder, the limits and the read-only state) and how they read their receiver's list.

### Modified Capabilities
- `client-groups`: the group detail gains the Wishlists section with its two entries and the state-dependent note.

## Impact

- Code: five wishlist calls in `ApiClient`, two view models and two screens with the bottom sheet and the delete confirmation in `app/shared`, two typed routes, the group state's phase on the detail model, strings and the README.
- APIs: none new. The app additionally uses the existing `/groups/{id}/wishlist...` and `/groups/{id}/receiver/wishlist` routes with the contract types that already exist.
- Dependencies: none (the bottom sheet, dialog and link handler are part of Compose Multiplatform).
- Systems: none.

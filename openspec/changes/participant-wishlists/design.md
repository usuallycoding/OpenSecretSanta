# Design

## Context

The participant view is a `NavHost` with `GroupsRoute`, `GroupRoute(id)` and `ProfileRoute` under a `Scaffold` (`ShellScreen`); the Groups tab stays selected on every screen below a group. Screens load through `Loader` and `LoadContent` (loading, retry, a refresh that keeps what is shown) and call `refresh()` on `ON_RESUME`; view models take a `CoroutineScope` and are created through Koin; tests drive them over `ScriptedServer` with a real `ApiClient`. `GroupDetail` knows a group's state only as a label and a note (`GroupStateInfo`). The wishlist contract types (`WishlistItemDto`, `WishlistItemRequest`, `ReorderWishlistRequest`, `SharedWishlistItemDto`) and the routes already exist, and `WishlistEntry.of(title, note, url)` in `core/domain` validates an item exactly as the server does (fields `title`, `note`, `url`). See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- A wishlist editor that is hard to put into a wrong state: the server's answer is what the screen shows, and concurrent changes (another device, an archived group) lead to a reload and a sentence, not a stale or broken screen.
- Reuse of the loading, retry and refresh plumbing and of the server's validation rules.

**Non-Goals:**
- The receiver's identity, questions, drag and drop, timestamps, "bought" marks and admin screens (see proposal.md).

## Decisions

### D1: Routes and where the screens hang
```
 GroupsRoute -> GroupRoute(id) -+-> WishlistRoute(groupId)          "My wishlist"
                                +-> ReceiverWishlistRoute(groupId)  "Your receiver's wishlist"
```
Typed routes like the existing ones, so the browser history binding and the Android back button work without extra code. The top bar shows a back arrow on all three screens below a group (today only on `GroupRoute`), and the Groups tab stays selected. The sheet is not a route: it has no history entry, and the platform's way of dismissing (back on Android, a tap outside, Escape on the web) closes it.

### D2: The group's phase on the detail model
`GroupDetail` gains `phase: GroupPhase` (`Pending`, `Active`, `Archived` or `Other` for an unknown state), set from the state string next to `groupStateInfo`. The detail screen uses it for the Wishlists section (receiver entry only for `Active` and `Archived`, the Pending note otherwise); the wishlist view model uses it to decide whether the list is editable. The entry "My wishlist" shows no item count: a count would cost an extra request on every detail load.

### D3: Calls
`ApiClient` gains `wishlist(groupId)`, `addWishlistItem`, `updateWishlistItem`, `deleteWishlistItem`, `reorderWishlist` and `receiverWishlist(groupId)` over the existing routes and contract types, through the same `call` helper (bearer token, 401 handling, error mapping). `DELETE` answers 204 with no body, so it is called as `Unit`; a test makes sure that parses on every platform. Item errors arrive as `Validation(field, message)` (400), `Rejected(409, ...)` (archived, or the list is full) and `Rejected(404, ...)` (item or group gone).

### D4: `WishlistViewModel`, the one place that knows the rules
```
 data  : Loader<WishlistData(items: List<WishItem>, phase)>   wishlist + group loaded together
 state : editable = phase is Pending/Active and not locked
         editor   = null | ItemEditor(itemId?, title, note, url, errors, saving)   (the sheet)
         deleting = null | WishItem                                                  (the confirmation)
         moving   = Boolean                                                          (a reorder is being saved)
         notice   = null | ChangedElsewhere | ItemGone | Locked | ListFull | Unreachable
```
Loading fetches the list and the group together (`api.wishlist` then `api.group`, the first failure wins), so the screen knows the phase from the same source as the detail screen and a vanished group shows as such. `Loader` gains `replace(value)` so a successful change updates the list from the server's answer without another request. How each call ends:
- **Add or edit succeeded:** the item from the answer is appended or put in its place, the sheet closes.
- **Delete succeeded:** the item is removed, the others keep their order.
- **Reorder succeeded:** the order the server returns replaces the list. A move sends all ids in the new order.
- **400 naming `itemIds`:** reload, notice `ChangedElsewhere`.
- **400 naming `title`, `note` or `url`:** shown on that field; the sheet stays open with the edits.
- **409:** reload, then notice `Locked` if the phase is no longer editable, `ListFull` if it still is (the 50-item limit was hit from another device).
- **404 on an item:** close the sheet or confirmation, reload, notice `ItemGone`. 404 on the group makes the whole screen "no longer available", as on the detail screen.
- **Network failure:** the sheet keeps the edits and says so; for a delete or a move a notice says the server could not be reached.

Before sending an add or an edit, `WishlistEntry.of` validates and its exception names the field. The 50-item limit is also checked locally so "Add" is unavailable at 50. *Alternative:* optimistic updates (changing the list before the server answers); rejected, because the server can refuse any of these changes for a reason the screen does not know yet, and a wrong list is worse than a short wait.

### D4a: Why not move buttons that reorder locally and send once
Each press sends the full order and replaces the list with the answer, and the buttons stay unavailable until it arrives. Several quick presses cannot interleave, and the 400 "list changed elsewhere" case has one clear handling.

### D5: The sheet, the confirmation and links
- **Sheet:** Material 3 `ModalBottomSheet` (an experimental API, opted in once) opened fully (`skipPartiallyExpanded`), with title, note and link fields, Save, and Delete when editing. The content scrolls and accounts for the keyboard (`imePadding`) so the fields stay visible. Its state lives in the view model, so it survives recomposition; signing out discards the view model.
- **Confirmation:** a Material 3 `AlertDialog` naming the item.
- **Links:** an item's link is shown as text and opens through `LocalUriHandler`; any failure to open is ignored (nothing to tell the person that they could act on). Only links that came from the server are shown.

### D6: `ReceiverWishlistViewModel`
A plain `Loader` over `receiverWishlist(groupId)` mapping to the same row model without ids. It has no state of its own and no editing, so the screen cannot change or record anything.

### D7: Tests
Common tests over `ScriptedServer`, extended with the wishlist routes (list, add, edit, delete, reorder, receiver list) and scripted failures:
- `ApiClient`: every call, 204 on delete, 400 with a field, 404, 409.
- `WishlistViewModel`: order, empty, editable by phase, add (appended), edit (same place, cleared note and link), delete with confirmation and cancel, reorder up and down and the ends, the buttons locked while saving, each local validation error, server field errors keeping the sheet open, 400 `itemIds`, 409 as archived and as full, 404 on an item, the 50-item limit, a network failure.
- `ReceiverWishlistViewModel`: items in order, empty, unreachable then retry.
- The group detail phase.

The screens, the sheet and the dialog are verified by running the app: the Android emulator, headless Chrome (including that the sheet and keyboard behave in the browser) and your phone for iOS.

## Risks / Trade-offs

- [`ModalBottomSheet` is experimental and sheets with text fields can differ between Android, iOS and the web] → Built and tried on all three early (the first UI task), with the content kept simple (fields, two buttons) so a fallback to a full-screen form is a small change.
- [The 409 answer is ambiguous: archived or full] → Decided from the reloaded phase (D4), never guessed from the message.
- [A stale phase can show editing controls that the server then refuses] → The refusal is handled (reload, notice, read-only), and the screen reloads on return to the app.
- [`DELETE` returning 204 with no body could fail to parse on one platform] → A dedicated test on every target in the first task.
- [Reordering by buttons is slow for a long list] → Accepted for now; drag handles are a possible follow-up and would only change the screen, not the view model.

## Migration Plan

Nothing to migrate: new screens, no server change and no new dependency. Rollback is the previous build.

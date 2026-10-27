# Tasks

## 1. Domain: items and validation

- [x] 1.1 Add `WishlistItem`, `SharedWishlistItem` (no id) and `WishlistEntry.of(title, note, url)` to `core/domain` with the limits (title 1-200, note at most 1000, link at most 2000), trimming, blank-to-empty and a hand-written `http`/`https` link check (case-insensitive scheme, ASCII dotted host with optional numeric port, no whitespace or user-info); verify common tests for every accepted and rejected link in the spec (`javascript:`, `data:`, `file:`, `ftp:`, `//host/path`, `not a url`, uppercase `HTTP://`), length boundaries, whitespace and blanks, and that errors name `title`, `note` or `url`; `./kotlin build -m core/domain` succeeds on all platforms

## 2. Domain: participant use cases

- [x] 2.1 Add the group-scoped `WishlistRepository` port and an in-memory fake, then implement `GetMyWishlist` (any state after Draft), `AddWishlistItem` (appended last, 50-item cap with `invalid_state`), `UpdateWishlistItem` (keeps place and creation time, updates the last-changed time), `DeleteWishlistItem` and `ReorderWishlist` (ids must be exactly the caller's current items in that group, each once, otherwise `validation_failed` naming `itemIds` and nothing changes), each starting with the participant check and the four writes refused with `invalid_state` once the group is Archived; verify tests for append order, the cap, edit keeping place, clearing note and link, delete keeping order, each reorder failure (missing, repeated, foreign, stale after another add), another participant's id and an id from the caller's list in a different group being `not_found`, one user keeping independent lists in two groups, editable in Pending and Active, read-only in Archived, and Drafts and strangers being `not_found`

## 3. Domain: the Santa's view

- [x] 3.1 Implement `GetReceiverWishlist` using the participant check, `receiverOf` and the repository, returning that group's `SharedWishlistItem`s in the receiver's order; verify tests for Active and Archived, `invalid_state` in Pending, `not_found` for strangers, Drafts, unknown groups and the non-participating owner, each participant getting only their own receiver's list, a receiver with different lists in two groups being read separately by each group's Santa, live changes, and an empty list

## 4. Contract

- [x] 4.1 Add `WishlistItemDto`, `WishlistItemRequest`, `ReorderWishlistRequest`, `SharedWishlistItemDto` and the new `/groups/{id}/wishlist...` routes to `core/contract`; verify round-trip tests, that the shared item has no `id` field, and `./kotlin build -m core/contract` succeeds on all platforms

## 5. Persistence

- [x] 5.1 Add migration `V006__wishlist.sql` (`wishlist_items` with `group_id` cascading on group delete, `UNIQUE (group_id, username, position)` and an index) and register it; verify migration tests show schema version 6, `/health` reports 6, the database rejects a duplicate position within one list and an unknown group or owner, the same position is allowed in another group or for another user, and deleting a group removes its items
- [x] 5.2 Implement `SqliteWishlistRepository` (state-checked write-lock `UPDATE` first, participant re-check, two-step reorder through temporary negative positions, lookups scoped by group, owner and id); verify repository tests for append positions with gaps after delete, reorder never violating uniqueness, the cap under concurrent adds, concurrent adds getting distinct positions, owner and group scoping of update and delete, writes refused in Archived and Draft groups, a removed participant's items staying hidden and returning when re-added, and rollback of a reorder that fails part-way

## 6. Server routes

- [x] 6.1 Implement `GET /groups/{id}/wishlist`, `POST /groups/{id}/wishlist/items`, `PUT` and `DELETE /groups/{id}/wishlist/items/{itemId}` and `PUT /groups/{id}/wishlist/order`, register the components in Koin; verify HTTP tests for every route including validation errors naming the field, the 50-item cap, 404 for someone else's, another group's or a non-numeric id, 409 `invalid_state` for writes in an Archived group while reads still work, 401 when not signed in, `password_change_required` with a temporary password, a user in two groups keeping independent lists, and the Koin graph test
- [x] 6.2 Implement `GET /groups/{id}/receiver/wishlist`; verify HTTP tests for the Santa reading in Active and Archived, order and live changes, an empty list, `invalid_state` in Pending, 404 for strangers, Drafts and the non-participating owner, each participant getting only their own receiver's list for that group, no ids in the response, and that repeated reads leave the receiver's own wishlist response and every other receiver-facing response unchanged

## 7. Integration

- [x] 7.1 Document the wishlist routes, limits, editable states and link rule in `README.md`, run `./kotlin build` and `./kotlin test` for the whole repo, then verify with `curl` on a real server: a participant adds, edits, reorders and deletes items in one group while their list in another group is untouched, bad links are refused, the cap holds, a Pending group accepts edits and an Archived one refuses them, their Santa reads the list in the new order without ids, and the receiver's own responses show no sign of the read

# Design

## Context

Groups reference users by username, participants of a group are stored in `group_participants` (no foreign key to users), and a Santa already reaches their receiver through relationship-named routes (`/groups/{id}/receiver...`) guarded by `ParticipantGroupLoader` and `GroupRepository.receiverOf`. Repositories apply state-checked changes in one transaction and take SQLite's write lock with a first `UPDATE` before read-then-write sequences (as the question repository does). Everything authenticated already sits behind the password-change gate. See proposal.md for motivation and specs/ for behavior.

A wishlist belongs to a participant *in a group*: the same person keeps a separate list in every group, so every wishlist operation is addressed by group.

## Goals / Non-Goals

**Goals:**
- A wishlist whose order can never be corrupted, even with two devices editing or racing requests.
- Reuse the participant access path (not Draft, caller participates) and the state rules groups already have instead of inventing new ones.
- Links that are safe for clients to render.

**Non-Goals:**
- Notifications. They are planned for a follow-up (every wishlist add or change and every answer created or edited will notify the other party in the app), and such a notification must never reveal a Santa to their receiver. This change builds nothing for it; it only keeps the creation and last-changed timestamps that a notification feature can use.
- Admin access (the `admin-oversight` change), marking items as bought, copying a list between groups, client screens.
- Internationalized (non-ASCII) host names in links.

## Decisions

### D1: Schema `V006` with an order the database protects
```
wishlist_items(id INTEGER PK AUTOINCREMENT,
               group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
               username TEXT NOT NULL REFERENCES users(username),
               position INTEGER NOT NULL,
               title TEXT NOT NULL, note TEXT, url TEXT,
               created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
               UNIQUE (group_id, username, position))
INDEX (group_id, username, position)
```
A list is the set of rows with one `(group_id, username)`. `UNIQUE (group_id, username, position)` means two items of one list can never share a place. SQLite checks uniqueness per statement, so reordering is done in two steps inside one transaction: first move the list's items to temporary negative positions (`-1 - position`, which cannot collide with real ones), then write the final positions `0..n-1`. Deleting leaves a gap, which is harmless because only the relative order matters; a new item takes `MAX(position) + 1` within its list. Deleting a (Draft) group removes its lists through the cascade. There is deliberately no foreign key to `group_participants`: a participant removed while the group is Pending keeps their rows, which are hidden because every access checks participation, and reappear if they are added back. Times are epoch milliseconds from the injected `Clock`, formatted as ISO-8601 at the API edge, as for questions. *Alternatives:* a per-user list with a group tag (cannot enforce one position space per list), a linked list of `next_id` (reads need recursion, no ordering in SQL).

### D2: Domain model and validation
`WishlistItem(id, title, note, url, createdAtMillis, updatedAtMillis)` for the owner, and `SharedWishlistItem(title, note, url, createdAtMillis, updatedAtMillis)` for a Santa (no id, so the type itself cannot leak one). A `WishlistEntry.of(title, note, url)` validates input once for add and edit: trimmed title 1 to 200, note and link blank to empty with caps of 1000 and 2000, errors naming `title`, `note` or `url`. The link check is hand-written in common Kotlin (no JVM-only URL class, so it also works on iOS and web): no whitespace or control characters, scheme `http` or `https` compared ignoring case, `://`, then a host made of dot-separated ASCII labels (`A-Z a-z 0-9 -`) with an optional numeric port, no user-info (`@`). Everything after the host is accepted as given. The link is stored as the user typed it (trimmed), not rewritten. *Alternative:* `java.net.URI` on the server only (accepts `javascript:` and relative references and would need extra checks anyway).

### D3: Ports and use cases
`WishlistRepository` (`list`, `add`, `update`, `delete`, `reorder`), every method taking the group id and the owner's username and applied in one transaction. The write methods start with `UPDATE groups SET state = state WHERE id = ? AND state IN ('pending', 'active')`, which takes the write lock and enforces the editable states in the same statement, then confirm the owner is still a participant (`group_participants`); only then do the count, the cap and the position arithmetic run, so none of them can interleave with another writer or with a participant being removed. Use cases in `core/domain` all start with `ParticipantGroupLoader` (not Draft, caller participates, otherwise `not_found`): `GetMyWishlist` (any state after Draft), `AddWishlistItem` (cap 50, `invalid_state` when full), `UpdateWishlistItem`, `DeleteWishlistItem` and `ReorderWishlist` (the ids must be exactly the caller's current ids in that group, each once, otherwise `validation_failed` naming `itemIds` and nothing changes) — the four writes are refused with `invalid_state` once the group is Archived — and `GetReceiverWishlist`, which also requires the group to be Active or Archived, finds the receiver with `receiverOf` and returns their `SharedWishlistItem`s for that group.

### D4: API
```
GET    /groups/{id}/wishlist                   [WishlistItemDto]
POST   /groups/{id}/wishlist/items             WishlistItemRequest -> 201 WishlistItemDto
PUT    /groups/{id}/wishlist/items/{itemId}    WishlistItemRequest -> WishlistItemDto
DELETE /groups/{id}/wishlist/items/{itemId}    204
PUT    /groups/{id}/wishlist/order             ReorderWishlistRequest(itemIds) -> [WishlistItemDto]
GET    /groups/{id}/receiver/wishlist          [SharedWishlistItemDto]
```
Item lookups are always `WHERE id = ? AND group_id = ? AND username = ?`, so someone else's id, or the caller's own item from a different group, looks exactly like a missing one (404). Non-numeric ids are 404 as for groups. The routes are inside the existing authenticated block, so the password-change gate already covers them.

### D5: No trace of reads
Nothing in the schema or the code records that a wishlist was read, by whom or when: there is no read table, no last-read column and no counter, and the owner's response has no field that could carry one. A test reads the receiver's list repeatedly as a Santa and asserts the owner's own `GET /groups/{id}/wishlist` is byte-for-byte unchanged.

### D6: Tests
Domain: item validation table (lengths, blanks, whitespace), a link table of accepted and rejected forms (including every case in the spec), use cases with fakes (append at the end, cap, edit keeps place, delete keeps order, reorder success and each way to fail, own-items-only, items of another group being `not_found`, Pending and Active editable, Archived read-only, Drafts and strangers `not_found`, the Santa path for Active, Archived, Pending and the non-participating owner). Server: migration test (schema version 6, unique position per list, same position allowed in a different group or for a different user, cascade on group delete), repository tests (two-step reorder never violates uniqueness, concurrent adds get distinct consecutive positions and respect the cap, gaps after delete, a participant removed mid-request cannot write), HTTP tests for every route, the gate, a participant in two groups keeping independent lists, and a sweep that admin responses never contain wishlist text.

## Risks / Trade-offs

- [Someone in several groups has to enter their wishes again for each one] → Intended: wishes differ per exchange. Copying a list between groups is a possible follow-up and is out of scope.
- [A participant removed while Pending leaves hidden rows behind] → They are never exposed (every access checks participation), vanish with the group on delete, and come back if the person is re-added; no cleanup step to get wrong.
- [Hand-written link validation can disagree with how a browser parses edge cases] → It is deliberately stricter than browsers (ASCII hosts, no user-info, no spaces), so what passes is plain and safe; internationalized hosts must be entered in their `xn--` form.
- [A Santa may buy an item the receiver then removes or changes] → The Santa's view carries last-changed times so a change is visible on the next look; the planned notifications will make it immediate.
- [Gaps in positions after deletes] → Harmless, since only order matters, and the next reorder renumbers from 0.
- [The cap of 50 and the field limits are arbitrary] → Constants in one place, easy to adjust.

## Migration Plan

Deploy normally; `V006` is additive and applies on start (`/health` reports schema version 6). Every participant starts with an empty list in every group. Rollback is restoring the previous build and the pre-upgrade database file.

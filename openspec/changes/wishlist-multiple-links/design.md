## Context

See proposal.md for motivation. Today `wishlist_items.url` holds one nullable link, and `WishlistEntry.of(title, note, url)` validates it with `WebLink`. The contract DTOs (`WishlistItemDto`, `WishlistItemRequest`, `SharedWishlistItemDto`) carry `url: String?`, and the same DTOs serve the owner's list, a Santa's read of the receiver's list and the owner's oversight read. The app (`ItemEditor`, `ItemText`/`LinkText`) renders one link everywhere through one shared `ItemText`.

Constraints:
- Android apps already distributed send and expect `url`. The server is deployed first, so old apps meet the new server for a while.
- Migrations are append-only (`index.txt`); the bundled sqlite-jdbc is 3.53, so `ALTER TABLE ... DROP COLUMN` is available.
- Every write in `SqliteWishlistRepository` runs in a transaction that takes the write lock via `lockForWrite`; link writes must stay inside that.
- The app's client `Json` uses `ignoreUnknownKeys = true`, so a new `urls` field does not break older readers, and the server ignores `url` only where the spec says so.

## Goals / Non-Goals

**Goals:**
- Up to 10 ordered links per item with no loss of existing data and no breakage for old apps.
- One place that normalizes and validates links (trim, drop blanks, de-duplicate, cap, per-link errors) shared by add and edit.

**Non-Goals:**
- Labels, reordering links, link previews.
- Removing the deprecated `url` alias; that is a later change once old apps are gone.

## Decisions

### Links live in a child table, not a JSON column
`V016` adds `wishlist_item_links (id, item_id REFERENCES wishlist_items(id) ON DELETE CASCADE, position, url NOT NULL, UNIQUE (item_id, position))`. Reads load an item's links ordered by `position`.

Alternatives: a JSON array in `wishlist_items.url` (no migration of shape, but no per-link constraint, awkward to evolve, and it overloads a column old code reads) or a delimiter-joined string (links can contain anything after the host). A child table follows how the lists already work (`position` with `UNIQUE`) and keeps cascade deletes when an item or group goes.

`V016` copies each non-blank `wishlist_items.url` into position 0, then runs `ALTER TABLE wishlist_items DROP COLUMN url`. The old column is dropped, not left behind, so no code path can read a stale copy. The migration does not touch `updated_at`, so Santas do not see every migrated item as edited. If the drop fails the whole migration rolls back, as migrations already do.

### The request distinguishes absent `urls` from empty `urls`
`WishlistItemRequest.urls` is `List<String>? = null`, with `url: String? = null` kept. A present list, even empty, replaces the links. An absent list means an older app:
- add: the links are `[url]` when `url` is non-blank, else none;
- edit: the first link becomes `url`, or is removed when `url` is blank/absent, and links 2 to 10 are kept.

This is why an old app saving only a title never wipes links 2 to 10. A new app always sends `urls`. When both are present, `urls` wins and `url` is ignored.

The "older app" edit is expressed in the domain as a different entry kind, not as a flag on the repository: the use case reads the item's current links, merges the first, and then hands the repository a complete list. The repository only ever replaces a full list.

### Normalization and errors in the domain entry
`WishlistEntry.of(title, note, urls)` trims each link, drops blanks, removes repeats keeping the first, then checks the count (> 10 → `ValidationFailed("urls", ...)`), then each link's length and `WebLink` (→ `ValidationFailed("urls[i]", ...)`), where `i` is the zero-based place after blanks and repeats are removed. Errors for the links are the server's naming the place the person sees only if the app sends links without gaps, which it does, so the app maps `urls[i]` to the i-th non-blank field it sent. `MAX_LINKS = 10` sits beside `MAX_URL_LENGTH` in `WishlistLimits`.

Count is checked before per-link validation so that eleven bad links answer with `urls`, which is the more useful message.

### Response carries both `urls` and `url`
Every DTO gets `urls: List<String> = emptyList()` and keeps `url`, filled with the first link by the route mappers (`toDto()` in `WishlistRoutes` and `AdminOversightRoutes`, and the receiver route). `encodeDefaults` is not assumed: the server's mapping sets `urls` explicitly. Domain types `WishlistItem` and `SharedWishlistItem` replace `url: String?` with `urls: List<String>`, so only the DTO layer knows about the alias, and removing it later is a DTO change.

### A link change is an item change
`update` always sets `updated_at`, and the links are rewritten in the same transaction. Rewriting is delete-all then insert for the item (at most 10 rows), which avoids `UNIQUE (item_id, position)` clashes without the parking trick `reorder` needs. `ReadMarkCounter` already counts an item as new or edited by `updatedAtMillis`, so no read-mark change is needed.

### Client: a list of fields in the editor, one shared renderer
`ItemEditor.url: String` becomes `urls: List<String>` (at least one field, at most 10), and `WishField.Url` becomes a per-index field so errors are kept per link. `WishlistEntry.of` on the client validates the same way as the server and maps errors to the right index. `ItemText` takes `urls: List<String>` and renders one `LinkText` per link under the note; because the own list, the Santa's list and the oversight sheet already share `ItemText`, all three change in one place. The existing link dialog is unchanged and takes the tapped link.

## Risks / Trade-offs

- [An old app edits an item with several links and shows only the first] → Accepted by design: it never deletes the others. The app is updated through the usual distribution; `url` stays until it is not needed.
- [Merging for an older app reads the item before writing] → Done inside the repository's write transaction after `lockForWrite`, so two devices cannot interleave.
- [Migration drops a column on a live database] → The data is copied in the same migration, a test checks that links survive with their items, and `DROP COLUMN` needs SQLite 3.35+, which the bundled driver exceeds. Back up the data volume before deploying, as the README's update steps already allow.
- [A deprecated alias is easy to forget] → It is confined to the DTOs and the request handling, marked deprecated in KDoc, with a test that pins its behavior.
- [Per-index error names can point at the wrong field if the app and server disagree about gaps] → The app sends only non-blank, de-duplicated links in order, so the indexes agree; the server's index is also checked in a route test.

## Migration Plan

1. Deploy the server: `V016` runs at startup, creating the table, copying links and dropping the old column. Apps in use keep working through the `url` alias.
2. Release the app with the new sheet. A new app talking to the new server sends `urls`.
3. Rollback of the server image after `V016` is not supported by an append-only migration scheme; roll back by restoring the database backup taken before deploying.
4. A later change removes `url` once old apps are gone.

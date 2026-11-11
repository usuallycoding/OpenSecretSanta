# Proposal

## Why

A wishlist item holds a single link, but a gift is often found in several shops or in several variants, and people end up pasting many addresses into the note, where they are not links. Letting an item carry up to 10 links keeps them tappable, safe to open and in the order the person chose.

## What Changes

- An item holds up to 10 links instead of one. Links are web addresses only, with no labels; each keeps the existing rules (at most 2000 characters, absolute `http` or `https`).
- Blank entries are dropped, repeated links are removed after trimming (the first occurrence stays), and the links keep the order they were entered in. There is no reorder control for links.
- More than 10 links is a 400 naming `urls`; a bad link is a 400 naming its place, such as `urls[2]`.
- Adding, removing or changing a link counts as editing the item: its last-changed time is updated, so a Santa sees it as edited.
- Wire format stays compatible with apps already installed: items carry a new `urls` list and keep `url` as a deprecated alias for the first link. A request with `urls` uses it; a request with only `url` replaces the first link and keeps the others, so an old app saving an item never wipes links 2 to 10.
- Existing links are migrated: the database moves each non-blank link into a new links table, as the item's first link, and nothing is lost.
- The item sheet shows one link field and an "Add link" button up to 10 links, with a remove button per link and a count; a Santa's view of the receiver's list, the person's own list and the owner's participant wishlist sheet show every link of an item, each opening the existing link dialog.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `wishlist`: an item holds up to 10 links with their own validation, order, de-duplication and error naming; reading, adding and editing carry `urls` and the deprecated `url`; changing links updates the last-changed time.
- `client-wishlist`: the add and edit sheet manages up to 10 links, form validation covers each link, and the link dialog applies to every link shown.
- `gift-visibility`: the receiver's wishlist returns each item's links.
- `admin-oversight`: the owner's read of a participant's wishlist returns each item's links.
- `client-admin-oversight`: the participant wishlist sheet shows every link of an item.

## Impact

- **Contract** (`core/contract/src/WishlistDtos.kt`): `urls: List<String>` added to `WishlistItemDto`, `WishlistItemRequest` and `SharedWishlistItemDto`; `url` kept and marked deprecated.
- **Domain** (`core/domain/src/wishlist`): `WishlistEntry`, `WishlistItem`, `SharedWishlistItem`, `WishlistLimits` (new max links), validation with per-link field names.
- **Server**: migration `V016` (new `wishlist_item_links` table, data copy, old column dropped when the bundled SQLite supports it), `SqliteWishlistRepository`, `WishlistRoutes`, `AdminOversightRoutes`, receiver wishlist route.
- **App** (`app/shared/src/ui/wishlist`, `ui/admin`): item editor state and sheet, `ItemText`, models, strings; the web, Android and iOS targets share this code.
- **Compatibility**: apps released before this change keep working but see and edit only the first link.
- Out of scope: link labels, reordering links, previews of the linked pages.

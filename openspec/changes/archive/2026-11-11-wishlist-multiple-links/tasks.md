## 1. Domain and contract

- [x] 1.1 Add `MAX_LINKS = 10` to `WishlistLimits`; change `WishlistItem` and `SharedWishlistItem` to carry `urls: List<String>` in place of `url`
- [x] 1.2 Change `WishlistEntry.of` to take a list of links: trim, drop blanks, remove repeats keeping the first, reject more than 10 with `urls`, and reject a bad link with `urls[i]` (length, then `WebLink`); update `WishlistEntryTest` (ten, eleven, blanks, repeats, per-index names, unsafe schemes)
- [x] 1.3 Add `urls: List<String>` to `WishlistItemDto`, `SharedWishlistItemDto` and `urls: List<String>? = null` to `WishlistItemRequest`, keeping `url` and marking it deprecated in KDoc; update `WishlistDtoSerializationTest` (round trips, key sets, an old request with only `url`)
- [x] 1.4 Update the use cases (`WishlistUseCases`, `GetParticipantWishlist`, `GetReceiverWishlist`): add with `urls` or with only `url`; edit with `urls`, or with only `url` replacing the first link and keeping the rest; update the fakes and `WishlistUseCasesTest`, including the older-app scenarios of the `wishlist` spec

## 2. Server

- [x] 2.1 Add `V016__wishlist_links.sql` (create `wishlist_item_links`, copy non-blank links to position 0 without touching `updated_at`, drop `wishlist_items.url`) and append it to `index.txt`
- [x] 2.2 Add a migration test next to `WishlistMigrationTest`: an item with a link keeps it as its only first link, an item without a link stays without, and titles, notes, places and times are unchanged
- [x] 2.3 Update `SqliteWishlistRepository` to read, add and replace links of an item in its existing write transaction (ordered by `position`), always updating `updated_at` on edit; cover it in `SqliteWishlistRepositoryTest` (ten links, replacing, removing all, cascade on item and group delete)
- [x] 2.4 Update `WishlistRoutes`, the receiver wishlist route and `AdminOversightRoutes` to return `urls` with `url` set to the first link; update `WishlistRoutesTest`, `ReceiverWishlistRoutesTest` and the oversight route tests (several links, no link, 400 names `urls` / `urls[1]`, older app add/edit/clear, `urls` winning over `url`)

## 3. App

- [x] 3.1 Update `WishlistModels` (`WishItem`, `SharedWishItem`, `ItemEditor`, `WishField`) and the oversight models to hold a list of links and per-link errors; map DTOs with `urls`, falling back to `url` when `urls` is empty and `url` is set
- [x] 3.2 Update `WishlistViewModel`: edit, add, remove and count link fields (start with one, at most 10), skip blanks on save, validate each link like the server, and map a server `urls[i]` error to the i-th link sent; update `WishlistViewModelTest` and `WishlistApiTest`
- [x] 3.3 Update `ItemSheetHost`: one field per link with a remove button, an "Add link" button that becomes unavailable at 10 with the count and a message, and errors next to each link; add the strings to `Strings.kt`
- [x] 3.4 Update `ItemText` and its callers (`WishlistScreen`, `ReceiverWishlistScreen`, `OversightSheets`) to show every link one under another, each opening the existing link dialog for that link; check `LinkTargetTest` and `WebAddressesTest` still hold
- [x] 3.5 Update the admin and receiver view models and their tests (`ReceiverWishlistViewModelTest`, `AdminOversightViewModelTest`) for several links

## 4. Check

- [x] 4.1 Run `./kotlin build` and the test suites of `core`, `server` and `app/shared`
- [x] 4.2 Run the app on the web target and one phone target: add an item with several links, edit it, check a Santa's view and the owner's sheet, and confirm the link dialog opens the tapped link
- [x] 4.3 With an older app build against the new server, edit the title of an item that has three links and confirm all three remain

# Tasks

## 1. Routes, addresses and titles

- [x] 1.1 Replace the five participant routes with `GifterRoute` and the new meaning of `ReceiverRoute`; update `WebAddresses` (addresses `#/groups/{id}/gifter` and `#/groups/{id}/receiver`, the removed addresses giving nothing) and `WebAddressesTest`, the shell's `belowGroups()`, `titleOf` and strings (titles "Gifter" and "Receiver"); verify `./kotlin build` and the tests

## 2. Sections and pages

- [x] 2.1 Add `LoadSection` (loading and error without scrolling or pull) and a page scaffold with one pull-to-refresh that calls every section's refresh; split `WishlistScreen` and `SantaQuestionsScreen` into `WishlistSection` and `SantaQuestionsSection` and build the Receiver page from them (the questions only while Active or Archived), wired to `ReceiverRoute` in the shell; verify in the web app that the list still adds, edits, reorders and deletes, and that the Santa's questions can be answered
- [x] 2.2 Split `ReceiverWishlistScreen` and `ReceiverScreen` into `ReceiverWishlistSection` and `ReceiverQuestionsSection` (without the postal details, which move to a sheet), build the Gifter page and wire `GifterRoute`; verify asking a question and seeing the answer

## 3. The group page

- [x] 3.1 Add `PairViewModel` (the receiver row: `GET /groups/{id}/receiver` with retry) with a unit test, the gift icon, the pair rows and the three details sheets (own, receiver's, Santa's, the Santa's only once Archived), and the small participants list at the end of the page with its inner scroll (spike the nested scroll first, fall back to a fixed-height lazy list), replacing the Wishlists and Your gift sections; remove the screens, entries and strings nothing uses any more; verify `./kotlin build` and the tests
- [x] 3.2 Verify in the web app against a real server in headless Chrome, with a Pending, an Active and an Archived group and many participants: the rows in each state, the sheets, both arrows and the gift icon, editing on the Receiver page, asking and answering, one section failing while the other shows, reload on each page's address, the participants list scrolling inside itself and back from a page; note what could not be tried (Android and iOS pull-to-refresh and the nested scroll)

## 4. Words

- [x] 4.1 Update the README (the group page, the pages and sheets, the addresses table) and run `./kotlin build` and `./kotlin test` for the whole repo

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

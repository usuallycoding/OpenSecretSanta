# Proposal

## Why

A Santa can now ask 200 questions and a receiver can send 200 letters per group (see `raise-gift-content-limits`). The lists that show them (the letters Outbox and Inbox, the two question lists and the receiver's wishlist) put every row in one scrolling `Column`, so every row is composed and measured at once, and each can be up to 1000 or 2000 characters long. A lazy list only composes the rows on screen, so a long list opens as fast as a short one.

## What Changes

- The five lists become lazy: the letters Outbox (`SendLetterContent`), the Inbox (`InboxContent`), the Santa's question list (`SantaQuestionsSection`), the person's own question list (`ReceiverQuestionsSection`) and the receiver's wishlist (`ReceiverWishlistSection`).
- A new `LazyPullPage` next to `PullPage` is the page for them: the same pull-to-refresh, padding and bottom space, with a `LazyColumn` in place of the scrolling `Column`. It registers its scroll for scrolling beside a page, like the lists of groups and users, which are already lazy.
- The three section functions own their page, so a call site no longer wraps them in `PullPage`.
- What the person sees and can do does not change: the same cards in the same order, the same notices, headers, empty texts, floating buttons and sheets, the same marking as seen, and a pull still reloads.
- Out of scope: the person's own wishlist, whose drag-to-reorder measures every row and needs its own change. Lazy lists for the admin pages, and paging the server's responses, are also not part of this.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. No requirement changes: lists look and work as before, so the change sets `skip_specs: true`.

## Impact

- `app/shared/src/ui/load/LoadContent.kt`: `LazyPullPage` and a lazy counterpart of `LoadSection`.
- `app/shared/src/ui/letters/LetterScreens.kt`, `gift/SantaQuestionsScreen.kt`, `gift/ReceiverScreen.kt`, `wishlist/ReceiverWishlistScreen.kt`: the five lists, each rebuilt as lazy items.
- `app/shared/src/ui/groups/GroupPages.kt`: the call sites of the three sections, which no longer wrap them in `PullPage`.
- No change to view models, the API, the stored data, the notifications or any spec.

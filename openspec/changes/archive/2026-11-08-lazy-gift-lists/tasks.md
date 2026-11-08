# Tasks

## 1. The lazy page

- [x] 1.1 Add `LazyPullPage` and the lazy counterpart of `LoadSection` to `app/shared/src/ui/load/LoadContent.kt`, as designed (pull-to-refresh, `LocalPullActive`, padding and bottom space as `PullPage`, `rememberLazyListState` with `ForwardOutsideScroll`); verify `./kotlin build` succeeds

## 2. Letters

- [x] 2.1 In `letters/LetterScreens.kt` rebuild the Outbox (`SendLetterContent`) and the Inbox (`InboxContent`) on `LazyPullPage`, with the notice, the limit or closed line and the empty text as items and the letters as `items(..., key = { it.number })`; verify the build succeeds and, on a device or the web build with a long list of letters, that both lists scroll, pull to reload, keep the "+" button and the sheet working, and mark the inbox as seen as before

## 3. Questions and the receiver's wishlist

- [x] 3.1 In `gift/SantaQuestionsScreen.kt` and `gift/ReceiverScreen.kt` rebuild `SantaQuestionsSection` and `ReceiverQuestionsSection` to own a `LazyPullPage` (with a `bottomSpace` parameter), the cards as keyed items, and update their call sites in `groups/GroupPages.kt` to drop the `PullPage` around them; verify the build succeeds and the question view-model tests still pass (`./kotlin test`)
- [x] 3.2 In `wishlist/ReceiverWishlistScreen.kt` rebuild `ReceiverWishlistSection` the same way and update its call sites in `groups/GroupPages.kt`; verify the build succeeds and the receiver wishlist view-model tests still pass

## 4. Verify

- [x] 4.1 Run the full build and all tests (`./kotlin build` and `./kotlin test`) and verify they pass
- [x] 4.2 On a device, with a thread of about 200 questions, 200 letters and 200 wishlist items, check on the tabbed pages and on a wide window that each of the five lists opens quickly, scrolls, pulls to reload, scrolls from the empty space beside the page, keeps its place when its tab is left and shown again, and that the spacing and the accent bars look as before; note anything that looks off in the change

# Design

## Context

Today a participant's group screen (`ui/groups/GroupDetailScreen.kt`) lists every participant and offers entries for five separate screens: `WishlistScreen` (editable, a `LazyColumn` inside `LoadContent` with a toolbar, notice and `ItemSheetHost`), `ReceiverWishlistScreen`, `ReceiverScreen` (the receiver's postal details, then my questions to them, with the ask sheet), `SantaQuestionsScreen` (the Santa's questions, answering in a sheet) and `SantaScreen` (the reveal). Each has its own view model (`WishlistViewModel`, `ReceiverWishlistViewModel`, `ReceiverViewModel`, `SantaQuestionsViewModel`, `SantaRevealViewModel`) that is already keyed by group id and loads its own request. Each screen wraps its content in `LoadContent`, which also provides the pull-to-refresh. The owner's pair list (`PairList` and the sheets in `ui/admin/OversightSheets.kt`) is the model for the rows and sheets. The routes `WishlistRoute`, `ReceiverWishlistRoute`, `ReceiverRoute`, `SantaQuestionsRoute` and `SantaRoute` are mapped by `WebAddresses` and by the shell's titles and back handling. Every request the new design needs already exists. Decided with the user: the pages are pages (screens with addresses), the details are sheets, and the participants list stays, as a small scrolling list at the end of the group page.

## Goals / Non-Goals

**Goals:**
- The group page as pair rows, details sheets and two pages that reuse the existing view models and behaviors.

**Non-Goals:**
- Any server or API change, the owner's screens, changing what a participant may see.

## Decisions

### D1: Routes and addresses
```
 GroupRoute(id)        #/groups/{id}
 GifterRoute(id)       #/groups/{id}/gifter      new: I give (receiver's list + my questions)
 ReceiverRoute(id)     #/groups/{id}/receiver    kept name, new meaning: I receive (my list + Santa's questions)
 removed: WishlistRoute, ReceiverWishlistRoute, SantaQuestionsRoute, SantaRoute
```
`ReceiverRoute` keeps its class and its address but now means the Receiver page; the old address of the "Your receiver" screen is the same text, so an old bookmark opens the new page, which is harmless. The other removed addresses become unknown and show the first screen. `WebAddresses` (its tests too), the shell's `belowGroups()` (back arrow and tab highlight) and `titleOf` change; page titles are "Gifter" and "Receiver" (new strings).

### D2: The group page
```
 name, state, note, description, date, time, location
 pair rows (D3)
 Participants (4)
 +-------------------------+
 | admin (you)             |   fixed height (about 4 rows), scrolls inside itself
 | alice                   |
 +-------------------------+
```
The page stays one scrolling column. The participants list is a `Column` with `heightIn(max)` and its own `verticalScroll`; whether a scroll inside a scroll hands a drag off correctly on all three platforms is checked in the first task of the page (a spike), with a `LazyColumn` of fixed height as the fallback.

### D3: Pair rows
```
 Pending:        [ alice  (c) ]   (c) = gift icon -> Receiver page
 Active/Archived:
   [ alice  ->  bob ]      alice = own username (sheet), bob = receiver (sheet), -> = Gifter page
   [ Secret Santa -> alice ]   "Secret Santa" tappable only when Archived, -> = Receiver page
```
Styled as the owner's `PairList`. The receiver's username comes from a new small `PairViewModel` (keyed by group id) that loads `GET /groups/{id}/receiver` with the existing `Loader`; the same data feeds the receiver's details sheet, so the sheet does not ask again unless refreshed. A failed load shows "Try again" in the first row only. A Draft group shows no rows (a participant cannot see one anyway). The gift icon is drawn by hand (the core icon set has none, as for the copy icon); the arrow is `Icons.AutoMirrored.Filled.ArrowForward`, already used.

### D4: Details sheets
Three bottom sheets as part of the group page (not routes, so the address stays), in the style of the owner's `UserSheet` (whose sheet frame moved to a shared `SheetFrame`): my details (loaded with `GET /me` by a small `MyDetailsViewModel`, since the account the shell holds has no email, phone or address, read-only, using `ReadOnlyText` so the copy icons apply), the receiver's (the old "Your receiver" detail: the `Field` rows and the "has not added postal details yet" sentence) and the Santa's (the old `SantaScreen` content, with the refusal message when the group is not Archived). The sheet state (which one is open) is plain UI state of the page.

### D5: Pages are lists of sections
A page is one scrolling `Column` (`PullPage`) of sections, wrapped once in the pull-to-refresh; a list holds at most 50 items and a thread at most 30 questions, so nothing needs to be lazy. The existing screens are split into:
```
 WishlistSection(vm)            from WishlistScreen      (toolbar, notice, items, ItemSheetHost)
 ReceiverWishlistSection(vm)    from ReceiverWishlistScreen
 ReceiverQuestionsSection(vm)   from ReceiverScreen      (header, ask button, questions, ask sheet)
 SantaQuestionsSection(vm)      from SantaQuestionsScreen (questions, answer sheet)
 Receiver page = WishlistSection (+ SantaQuestionsSection when Active/Archived)
 Gifter page   = ReceiverWishlistSection + ReceiverQuestionsSection
```
Each section is a plain composable plus its sheet host, so two sections share the page's one scroll. Each section keeps its view model, its own loading and error ("Try again") item, and its own `ON_RESUME` reload. A new `LoadSection` (the loading and error part of `LoadContent` without its scrolling and pull) is shared by the sections, and the pull logic of `LoadContent` became a shared `PullRefresh`; the pull gesture is on the page and calls every section's `refresh()` (`Platform.hasPullToRefresh` decides, as now). The view models and their tests are unchanged, which is why behaviors such as adding, reordering, asking, answering and the archived read-only state carry over without new specs.

### D6: What is removed
`GroupDetailScreen`'s Wishlists and Your gift sections, `WishlistEntry`, `giftEntries`/`GiftEntry` (if nothing else uses them), the five screens' scaffolding (their view models stay), the five routes and their strings that nothing uses any more (`MY_WISHLIST`, `RECEIVER_WISHLIST`, `YOUR_RECEIVER`, `SANTA_QUESTIONS`, `YOUR_SANTA` partly stay as labels). The unit tests of the removed routes' addresses are replaced.

### D7: Owner and administrator
An administrator who takes part sees the same participant page; the owner's own screens are untouched.

### D8: Verification
View model test for the receiver row (loads, fails and retries, group gone); `WebAddressesTest` with the new addresses and the removed ones giving nothing. Then by running it in headless Chrome against a real server (with a Pending group and an Active and an Archived one): the rows in each state, the three sheets, both arrows and the gift icon, the Receiver page editing its list and answering a question, the Gifter page asking one and showing the answer, one section failing while the other shows, refresh and the address of each page on reload, the participants list with many users scrolling inside itself, and back from a page. On Android and iOS the pull-to-refresh of a page and the nested participants scroll are for the user to check.

## Risks / Trade-offs

- [A page with two sections has two requests and two error states] -> Each section shows its own, as the spec says; the pull and the resume reload both.
- [A scroll inside a scroll may fight on a phone] -> A fixed-height list that only scrolls when it has more names than fit; a spike first, with a fallback.
- [The Receiver page's questions and the wishlist share one `LazyColumn` with sheets owned by two sections] -> The sheets are separate dialogs hosted at the page level, as the current screens already do.
- [A large refactor of five screens] -> Their view models do not change and are covered by tests; the work is moving composables into sections.
- [`ReceiverRoute` changes meaning] -> Old bookmarks are not promised; the address text stays.

## Migration Plan

Nothing to migrate; the server is untouched. Rollback is the previous build.

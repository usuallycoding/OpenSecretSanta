# Design

## Context

The shell (`ui/shell/ShellScreen.kt`) owns the one `Scaffold` and its `TopAppBar`, whose title comes from `titleOf(view, destination)`, a function of the route only, so it says "Group". The group name is shown by `GroupPage` and the owner's `AdminGroupScreen` (headline text) and as a `Text(group.name, titleMedium)` on each card of `GroupsScreen` and `AdminGroupsScreen`. `MULTILINE_MAX_ROWS` (six) in `ui/form/FormComponents.kt` limits the multi-line fields and, through `ReadOnlyText`, the cut of long read-only text. "Add item" (`WishlistSection`'s `Toolbar`), "Ask a question" (`QuestionsHeader` in `ReceiverScreen.kt`) and "New group" (`AdminGroupsScreen`) are wide `Button`s inside the content. The Gifter and Receiver pages (`ui/groups/GroupPages.kt`) switch between the two tabs with an index in `rememberSaveable` and show one `PullPage` at a time. Decided with the user: lists limit the name to one row without making it tappable; read-only text is cut at three lines too.

## Goals / Non-Goals

**Goals:**
- One-row group names with the full name available on the group page; three rows; floating buttons; swiping tabs.

**Non-Goals:**
- Labelled (extended) floating buttons, other screens' titles, changing name or text limits.

## Decisions

### D1: The top bar
The top bar of the group pages keeps the word "Group" (a first version put the group's name there through a title the pages set; it was dropped on review). The one-row name with the full-name dialog (D2) is the only place the name is shortened.

### D2: One-row names
- Group page and owner's detail: the name becomes a `ReadOnlyText` variant with `maxLines = 1` and an option for no copy icon: when the text is cut it is tappable and opens the existing `FullTextDialog` (which has its own copy icon). So `ReadOnlyText` gets two parameters, `maxLines` (default `MULTILINE_MAX_ROWS`) and `copy` (default true); the "shortened with an ellipsis" logic already uses the line limit and works for one line.
- The two lists: `Text(maxLines = 1, overflow = Ellipsis)`, no click of its own, so the card's tap opens the group.

### D3: Three rows
`MULTILINE_MAX_ROWS = 3`. The one constant limits the text fields (`SheetTextField`, `MultiLineTextField`) and the read-only cut. The fields' minimum height must not exceed three rows (they use `minLines`; the first task checks and lowers a minimum above three). README and specs that said six are updated.

### D4: Floating buttons
```
 Box(fillMaxSize) {
     page content (PullPage / list)       with bottom space so the last row clears the button
     FloatingActionButton(Modifier.align(BottomEnd).padding(16.dp))  { Icon }
 }
```
A shared `AppFab(icon, description, onClick)` in `ui/form` wraps `FloatingActionButton` and its icon (`contentDescription` = the action's name, so the existing strings "Add item", "Ask a question" and "New group" become descriptions). The shell's `Scaffold` has one FAB slot for all screens, so each screen places its own in a `Box` over its content, above the bottom navigation bar (the content is already inside the Scaffold's padding). Icons: `Icons.Filled.Add` (core set) for Add item and New group; a hand-drawn "help" (question mark in a circle) for Ask a question, as the copy and gift icons were drawn. The wishlist and questions sections keep their text for the cases where the action is unavailable ("closed", "list is full", "limit reached"); the button itself is composed by the page, next to its scrolling content, and only while `editable`/`canAdd` (wishlist) or `canAsk` (questions). `PullPage` gets a `bottomSpace` parameter (about 88 dp) so the last item can scroll clear of the button; the owner's list does the same.

### D5: Swiping
`HorizontalPager(state, beyondViewportPageCount = 1)` replaces the tab-index `when` on the Gifter and Receiver pages, with a `rememberPagerState` (saved across recomposition) and the `PrimaryTabRow` driven by `pagerState.currentPage`; choosing a tab calls `animateScrollToPage`. Each pager page is the same `PullPage` as today, with its tab's section and (where it has one) its floating button, so a FAB slides away with its page. Because both pages stay composed, each section's `ON_RESUME` reload works again; showing a page (by tap or swipe) additionally calls that tab's `refresh()` from `LaunchedEffect(pagerState.settledPage)` (not for the first page at start). The pager only exists when there is more than one tab; the Pending Receiver page stays a single wishlist. A horizontal pager next to the vertical scroll of the pages is the standard combination. Found when verifying in headless Chrome: a sideways wheel or trackpad scroll swipes the pages on the web, but a synthetic mouse drag or touch drag sent through the browser's devtools did not, so a finger swipe on a phone is for the user to check (the tabs always work by tapping).

### D6: Verification
In headless Chrome against a real server: a group with a 100-character name (page title cut and opening the dialog, both lists one row, a normal name unaffected, the top bar still saying "Group"), the fields and read-only text at three rows, each floating button (opens its sheet, hidden when the list is full or closed, last item clear of the button, tab swipe moving it), swiping with a mouse drag between the two tabs on both pages, tapping a tab, one tab failing, and the unchanged flows. Swiping with a finger and the FAB position over the system gesture area on Android and iOS are for the user to check.

## Risks / Trade-offs

- [A mouse drag may not swipe a pager on the web, and text selection may fight it] -> Tabs still work by tapping; checked when verifying.
- [A floating button over a scrolling list can hide content or clash with the bottom bar] -> Bottom space in the lists; each button placed inside the content area, above the bar.
- [Three rows can cut a note the person wants to see] -> The tap-to-read dialog is unchanged.

## Migration Plan

Nothing to migrate. Rollback is the previous build.

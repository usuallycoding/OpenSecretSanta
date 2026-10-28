# Design

## Context

Both group pages (`ui/groups/GroupPage.kt` for a participant, `ui/admin/AdminGroupScreen.kt` for the owner) are one scrolling column. The participant's page already has a participants box of fixed height (144 dp, `Participants` in `GroupPage.kt`) and uses `PullPage` for its pull-to-refresh. The owner's `Detail` composable puts "Edit details" after the details, then either the participants rows (Draft/Pending) or `PairList` (Active/Archived), and only then the next-step `Button` (with the four-people hint) and the Draft's "Delete" text button. The Gifter and Receiver pages (`ui/groups/GroupPages.kt`) are a `PullPage` with two sections, each with its own heading and its own view model; a section's `ON_RESUME` reload runs only while it is composed. Decided with the user: the step buttons and "Delete" join "Edit details"; the participants list and the pair list go in a box at the end that fills the rest of the page's height; the pages get a Wishlist and a Questions tab.

## Goals / Non-Goals

**Goals:**
- Actions together at the top; one scrolling box for the people at the bottom that takes the leftover height; two tabs.

**Non-Goals:**
- The participants editor sheet, swiping between tabs, remembering the tab, any change to what the parts do.

## Decisions

### D1: "Details on top, a box that fills the rest"
```
 +------------------------------+  BoxWithConstraints(maxHeight = H)
 | details (scrolls if it must) |  header: heightIn(max = H - MIN_BOX), verticalScroll
 | buttons                      |
 +------------------------------+
 | Participants (13)  [Edit...] |  footer: weight(1f), at least MIN_BOX
 | +--------------------------+ |
 | | list, scrolls inside     | |
 | +--------------------------+ |
 +------------------------------+
```
A new `HeaderWithFooter(loading, onRefresh, header, footer)` composable (in `ui/load`, next to `PullPage`) lays this out: the header is a `Column` in its own `verticalScroll` limited to the height minus a minimum for the box (about 160 dp), and the footer takes the rest with `weight(1f)`. The whole is inside `PullRefresh`, so on Android and iOS pulling the header down refreshes the page, as `PullPage` does now. Whether the pull still starts when the header has nothing to scroll cannot be tried in the headless browser, which has no pull gesture; the code relies on the header's scroll container being the pull's nested-scroll child, as the other pages do, and the user checks it on Android and iOS (the fallback is a header that always has a small scroll range). The same composable serves the participant's page (replacing its fixed 144 dp box) and the owner's page, so both behave alike. The footer's list is a `Column` with its own `verticalScroll`; the box is an `OutlinedCard` as the participant's box is today.

### D2: The owner's action row
`FlowRow` (wraps when the buttons do not fit) with `OutlinedButton` "Edit details" (when allowed), the filled step button (when the group has a step) and, for a Draft, the "Delete" text button in the error colour. The "Draw the names needs 4 people" hint stays directly under the row. The state of each button (`busy`, `drawNeedsMorePeople`) is unchanged. The confirmations and `AdminStep` stay as they are; only the place of the buttons changes in `Detail`.

### D3: What goes in the owner's box
The footer for Draft and Pending is the "Participants (n)" title with "Edit participants" beside it (outside the scrolling part, so it stays visible) and the rows with their marks and "Read wishlist" inside the scrolling part. For Active and Archived it is the "The draw" title, `PairList` rows and the legend inside the scrolling part; `PairList` keeps its sheets and its failed-draw state ("Try again"), which are shown in the box instead of growing the page.

### D4: Tabs
```
 TopAppBar
 [ Wishlist | Questions ]      PrimaryTabRow, fixed under the bar
 +--------------------------+
 | PullPage of the tab      |   each tab: its own scroll and its own pull
 +--------------------------+
```
`PrimaryTabRow` with two tabs under the app bar, the selected index in `rememberSaveable` (not in the web address, as sheets are not). The section headings ("My wishlist", "Your receiver's wishlist", "Questions from your Santa", "Your questions") go, since the tab names say it. Each tab is a `PullPage` over its section(s): the Wishlist tab refreshes the wishlist view model, the Questions tab the questions view model. The page still collects both states so that a "group gone" answer from either shows the group-gone message, and so that a tab shows its own loading and error without affecting the other. Because only the selected tab's section is composed, its `ON_RESUME` reload would not run for the other one; choosing a tab therefore calls that tab's `refresh()`, which is the "Choosing a tab" scenario. The Receiver page of a Pending group has no questions yet, so it shows the wishlist with no tab row. The ask and answer sheets and the item sheet stay hosted by their sections.

### D5: Verification
By running it in headless Chrome against a real server: a Draft, a Pending group with 13 participants and an Active group for the owner (the action row at the top, the box filling the rest and scrolling inside, the row wrapping in a narrow window, the hint, each step and Delete still confirming), the participant's page with few and with many participants and with a long description in a short window, and the tabs on both pages (switching, one tab failing, the ask and answer sheets, reload on the page's address). Pull-to-refresh and the nested scroll of the box on Android and iOS are for the user to check.

## Risks / Trade-offs

- [A box that fills the rest needs the height of the header, and the pull gesture needs something scrollable] -> `BoxWithConstraints` and a limited, scrolling header; the spike in task 1 settles whether the pull works and the fallback is described above.
- [On a short screen the details can leave very little room] -> The box keeps a minimum height and the header scrolls above it.
- [Another reload path for tabs] -> One `refresh()` on choosing a tab, which is cheap and keeps what is shown.
- [Three buttons in one row on a phone] -> `FlowRow` wraps them.

## Migration Plan

Nothing to migrate. Rollback is the previous build.

# Proposal

## Why

On the owner's group page the next step ("Make pending", "Draw the names", "Archive") and "Delete" sit at the very bottom, below the whole participants list, so with a large group they are far from "Edit details" and have to be scrolled to. The participants list and the pair list simply grow the page. On the participant side the Gifter and Receiver pages stack the wishlist and the questions in one long scroll, so a long wishlist hides the questions. The pages should keep their actions together at the top, give the list of people a place of its own at the bottom, and separate the two kinds of content.

## What Changes

- **Actions together (owner).** "Make pending", "Draw the names", "Archive" and, for a Draft, "Delete" move up beside "Edit details" into one row of buttons that wraps on a narrow screen. The hint that drawing needs four people stays right under the row.
- **The people in a box that fills the rest of the page.** On the owner's group page the participants list (Draft and Pending, with its marks, "Edit participants" and "Read wishlist") and the pair list (Active and Archived, with its legend) are shown in a box at the end of the page that scrolls inside itself and takes all the height that is left below the details and the buttons. The participant's own group page gets the same box for its participants list, instead of the box of fixed height it has now. When there is little room (a small window, a long description) the details scroll and the box keeps a minimum height.
- **Two tabs on the Gifter and Receiver pages.** Each page shows a tab row, **Wishlist** and **Questions**, and one tab at a time, each scrolling and pulling down by itself. The Receiver page of a Pending group has no questions yet, so it shows the wishlist without tabs. The selected tab is not part of the web address.
- **Unchanged.** What each part does (editing the wishlist, asking and answering, the owner's sheets, confirmations, the four-people rule) and the web addresses.

Out of scope:
- The owner's groups list and forms, the participants editor sheet, swiping between tabs, and remembering the selected tab across visits.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-admin-groups`: where the step buttons and "Delete" are, and the people box on the group detail.
- `client-admin-oversight`: the pair list sits in that box.
- `client-groups`: the participant's group page uses a box that fills the rest of the page.
- `client-group-pages`: the Gifter and Receiver pages are two tabs.

## Impact

- Code: `AdminGroupScreen` (an actions row and the people box), `GroupPage` (the same box), a small shared layout for "details on top, a list that fills the rest" in `ui/load` or `ui/form`, `GroupPages` (tab row; the sections lose their headings), `PairList` in `OversightSheets` (scroll inside the box), strings, README. No server, API or dependency change.
- Platforms: Android, iOS and web.

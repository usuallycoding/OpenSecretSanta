# Tasks

## 1. The layout

- [x] 1.1 Add `HeaderWithFooter` (a header column limited to the height minus a minimum box height and scrolling when it must, and a footer that fills the rest, inside the pull-to-refresh) and use it on the participant's group page in place of the fixed-height participants box; spike first that the pull still works with a header that does not overflow and that the box scrolls inside the page on the web, and record the result in the design; verify `./kotlin build` and the tests

## 2. The owner's group page

- [x] 2.1 Put "Edit details", the next-step button and, for a Draft, "Delete" in one wrapping row above the people, with the four-people hint under it, and show the participants (Draft and Pending, with "Edit participants" kept above the scrolling rows) or the pair list with its legend (Active and Archived) in the footer box; verify `./kotlin build` and the tests

## 3. Tabs

- [x] 3.1 Give the Gifter and Receiver pages a tab row (Wishlist, Questions) with one tab shown at a time, each its own pull page, the selected tab saved across recomposition, the sections' headings removed, a refresh when a tab is chosen, and no tab row for the Receiver page of a Pending group; verify `./kotlin build` and the tests

## 4. Check and document

- [x] 4.1 Verify in the web app against a real server in headless Chrome, as described in the design; note what could not be tried (Android and iOS pull and nested scroll)
- [x] 4.2 Update the README (the owner's buttons and the box, the participant's box, the tabs) and run `./kotlin build` and `./kotlin test` for the whole repo

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

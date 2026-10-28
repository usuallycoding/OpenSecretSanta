# Tasks

## 1. Three rows

- [x] 1.1 Set `MULTILINE_MAX_ROWS` to 3, check that no field's minimum height exceeds three rows, and update the README and wording that say six; verify the fields and the read-only cut in the web app and `./kotlin build` and the tests

## 2. Titles

- [x] 2.1 Give `ReadOnlyText` a line limit and a no-copy option and show the group's name on the two group pages as one row that opens the full name in a dialog; limit the name to one row, not tappable, on the cards of both groups lists; verify with a 100-character name

## 3. Floating buttons

- [x] 3.1 Add `AppFab` and the question icon, make "Add item", "Ask a question" and "New group" floating buttons placed by their pages (shown only while available, the unavailable explanations kept as text), give the lists room at the bottom, and remove the old buttons; verify each opens its sheet or form, is hidden when the list is full or closed, and never covers the last row

## 4. Swiping

- [x] 4.1 Replace the tab switch on the Gifter and Receiver pages with a `HorizontalPager` kept in step with the tab row (choosing a tab animates to it, showing a page refreshes it, the Pending Receiver page stays a single list); verify swiping with a mouse drag and tapping, one tab failing, and the floating buttons moving with their pages

## 5. Check and document

- [x] 5.1 Verify the whole in headless Chrome as the design describes, update the README (three rows, floating buttons, swiping) and run `./kotlin build` and `./kotlin test` for the whole repo; note what could not be tried (finger swipes and the floating buttons on Android and iOS)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

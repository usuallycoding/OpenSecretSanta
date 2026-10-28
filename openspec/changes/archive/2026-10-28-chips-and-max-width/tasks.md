# Tasks

## 1. One maximum width

- [x] 1.1 Define `MAX_CONTENT_WIDTH` once, cap the `Scaffold` of `ShellScreen` to it (centered, the rest in the theme background), and remove the per-screen `widthIn(max = 640.dp)` of the signed-in screens, `HeaderWithFooter` and `PullPage`; verify in headless Chrome at a 1900px window that the bars, content, tab row and the three floating buttons line up, and at phone width that nothing changed

## 2. End-aligned receiver

- [x] 2.1 Align the name after the arrow to the end in `PairRows` (both rows) and in the owner's `PairList`; verify with a long and a short username

## 3. Chips for participants

- [x] 3.1 Add a chips list (`FlowRow` of non-clickable chips in `ScrollingBox`, with a highlighted variant) and use it for the participant's group page, dropping the "(you)" text and giving the own chip a spoken "you"; verify with 13 participants and with 4 on a tall window
- [x] 3.2 Show an owner's Draft group's participants with the same chips, a single `*` for a participant without an account or with an incomplete profile, and the legend "* no account or profile incomplete" only when one is marked; remove the old bracket marks

- [x] 3.3 Take the participant's chips out of the box: `GroupPage` becomes a `PullPage` with the chips inline (the owner's Draft chips stay in the box), update the README line, and verify with 13 and with 4 participants, including the pull-to-refresh of the page

## 4. Pending rows

- [x] 4.1 Make an owner's Pending participants rows: the username opens the user sheet, a gift icon ends the row and opens the wishlist sheet (replacing the "Read wishlist" text), a `*` and the legend as in 3.2, and the tap and icon disabled for a participant without an account; verify the sheets open for seeded group 1 and that a no-account participant does nothing

## 5. Check and document

- [x] 5.1 Run `./kotlin build` and `./kotlin test` for the whole repo, verify the whole in headless Chrome, update the README (width, chips, marks, gift icon, alignment) and note what could not be tried (phones and tablets)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

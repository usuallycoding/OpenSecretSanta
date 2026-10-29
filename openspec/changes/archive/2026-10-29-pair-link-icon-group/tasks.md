# Tasks

## 1. The shared pair link

- [x] 1.1 Add `PairLink(label, onClick, modifier)` in `ui/form`: a tonal pill (`secondaryContainer`) holding an auto-mirrored arrow, `GiftIcon` and an auto-mirrored arrow, one `contentDescription` on the control and none on the icons, a touch target of at least 48dp and the surface's ripple; verify `./kotlin build -m shared` succeeds with no warnings on every platform.

## 2. Use it in the three places

- [x] 2.1 Replace the two arrow buttons of `PairRows` (first row, label `TITLE_GIFTER`; second row, label `TITLE_RECEIVER`) with `PairLink` and drop the unused arrow import; verify on the phone as a participant of an Active group that both rows show the pill between the names, the first opens the Gifter page and the second the Receiver page, and that a Pending group still shows the lone gift icon.
- [x] 2.2 Replace the arrow button of every row of `PairList` (label `adminPair(santa, receiver)`) with `PairLink` and drop the unused arrow import; verify on the phone as an administrator of an Active group that each pair row shows the pill and opens the pair sheet, and that the per-participant gift icon of a Pending group is unchanged.
- [x] 2.3 Check the details on the phone: with `uiautomator dump`, that each pair link is one clickable element at least 48dp high and wide and carries the expected description (and that no separate arrow or gift element is exposed); a long username with "(you)" and `*` wraps without moving or shrinking the link; the light and the dark theme; and a forced right-to-left layout shows the arrows leading from the first name to the second.

## 3. Integration

- [x] 3.1 Run `openspec validate pair-link-icon-group --strict` and the whole test suite; verify they pass with no new compiler warnings, then have the web app checked in a browser as a participant and as an administrator (the pill, its press feedback, and that it opens the same pages).

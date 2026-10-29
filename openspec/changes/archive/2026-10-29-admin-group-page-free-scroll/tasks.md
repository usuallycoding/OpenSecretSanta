## 1. Page layout

- [x] 1.1 Rebuild `AdminGroupScreen` as one column inside `PullPage` with the header, notices, search field and people, without `HeaderWithFooter` or `ScrollingBox`
- [x] 1.2 Show the pair list, the Pending rows, the Draft chips and their messages as plain content of the column (the chips unboxed)
- [x] 1.3 Remove `HeaderWithFooter` and `ScrollingBox` if nothing else uses them

## 2. The count

- [x] 2.1 Make the "Who gives to whom" string a function of the number of participants and show the count of the whole draw, unchanged by a search
- [x] 2.2 A test for the label text

## 3. Verify

- [x] 3.1 Run the full test suites and `openspec validate admin-group-page-free-scroll --strict`
- [x] 3.2 Check on the Android device: a Pending group and an Active group with enough people to scroll, the buttons scrolling away, pull to refresh, the search field with the keyboard open
- [x] 3.3 Docker build and the person's web check

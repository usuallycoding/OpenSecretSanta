# Tasks

## 1. Shared pieces

- [x] 1.1 Check which clipboard API (`LocalClipboard` with a plain-text `ClipEntry`, or `LocalClipboardManager`) builds for Android, iOS and web, and record it in the design; then add in `ui/form` the drawn copy icon, the copy state (idle, copied, failed) as a small class with a unit test, `ReadOnlyText` with the six-line cut, the copy button and the full-text dialog, `LinkText` and `LinkDialog`, and their strings in `Strings.kt`; verify `./kotlin build` and the tests
- [x] 1.2 Use them in `ItemText` (title, note, link) and check that the own list still opens an item for editing when the tap is not on cut-off text, and in `QuestionCard` and the oversight thread (question, answer)

## 2. The rest of the screens

- [x] 2.1 Use `ReadOnlyText` in the user detail (username heading, the profile fields, the temporary password, which keeps its selection), the receiver screen, the Santa reveal and the group description on the participant and admin group screens, as listed in the design

## 3. Check and document

- [x] 3.1 Verify in the web app against a real server in headless Chrome, with long data made through the API: the six-line cut and the full-text dialog on a note, an address and a question; no dialog for short text; the copy icon changing to a check mark and the clipboard holding the whole text; the link dialog with "Open in browser" and "Cancel"; the own-list tap still editing; the oversight sheet and the user detail; and the Android emulator where the tooling allows it; note what could not be tried
- [x] 3.2 Update the README (the cut at six lines, the copy icon, the link dialog, and that copying on the web needs `https` or `localhost`) and run `./kotlin build` and `./kotlin test` for the whole repo

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

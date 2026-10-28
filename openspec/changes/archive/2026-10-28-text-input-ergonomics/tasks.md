# Tasks

## 1. Shared pieces

- [x] 1.1 Add `MULTILINE_MAX_ROWS = 6`, a shared `MultiLineTextField` (default Return is a line break) and a shared `FormColumn` (insets, scrolling and the dismiss-on-drag connection of D1) in `app/shared`; add a domain test that `Profile.of` keeps a line break inside an address and trims blank lines around it; verify `./kotlin build` and the domain test

## 2. Profile

- [x] 2.1 Use `FormColumn` and `MultiLineTextField` on the Profile tab, with the address and delivery information as multi-line fields; verify on the Android emulator: the Save button reachable with the keyboard open, a drag on the form closes the keyboard, tapping a lower field scrolls it into view and keeps the keyboard open, Return in the address starts a new line, a long address stops at six rows and scrolls inside, an address on two lines saves and the Santa's receiver page shows two lines, and a drag inside the field's own text keeps the keyboard

## 3. The other forms and sheets

- [x] 3.1 Use `FormColumn` on Change password, Sign in and the group form and `MultiLineTextField` for the group description; verify on the Android emulator that each can reach its primary button with the keyboard open and that dragging closes the keyboard, and in headless Chrome that nothing regressed
- [x] 3.2 Use `MultiLineTextField` for the wishlist note and the question and answer sheet, and the dismiss-on-drag connection on the sheets' scrolling columns; verify on the Android emulator that each stops at six rows and a drag in a sheet closes the keyboard

## 4. iOS cursor in bottom sheets

- [x] 4.1 Reproduce on the iOS simulator, then try the candidates of D4 in order, building the iOS app after each and asking the reporter to type "abcd" into every sheet field (wishlist item title, note and link, question, answer, participants); keep the first that gives "abcd" everywhere, write the cause into design.md and mark this task done only when the reporter confirms; if none works, do the narrowing and the README note of candidate 5 and say so (candidate 1, keeping the selection inside the field, fixed it; the reporter confirmed on iOS)

## 5. Integration

- [x] 5.1 Update the README for the address, the six-row rule and the keyboard behavior; verify the web in headless Chrome: Profile with Return in the address, the six-row cap, and dragging
- [x] 5.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then check end to end against a real server on the Android emulator: edit the profile with a two-line address, save with the keyboard open, and read it as the Santa; note what could not be tried on iOS

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

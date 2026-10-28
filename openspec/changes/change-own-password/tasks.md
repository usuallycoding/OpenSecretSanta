# Tasks

## 1. The sheet

- [x] 1.1 Give `ChangePasswordViewModel` a `done` state and a `reset()`, make the password field composable shared, and add the password sheet (title, a line about other devices, three fields, errors, Save and Cancel, then the confirmation with Close) with its strings; cover `done` and `reset()` in the view model tests

## 2. The menu item

- [x] 2.1 Add "Change password" to the top-bar menu (both views), open the sheet over the current screen from `ShellScreen`, reset on dismiss and wire the Android back and web Escape handling; verify in headless Chrome as a participant and as an administrator in the admin view: wrong current password, short new password, mismatched repeat, success (still signed in, screen behind unchanged, other session rejected), closing and reopening shows empty fields

## 3. Check and document

- [x] 3.1 Update the README (the menu item and what it does to other sign-ins), run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (phones)

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.

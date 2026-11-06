# Tasks

## 1. Copying from the dialog

- [x] 1.1 Add pure functions for the text a row copies (`username: password`) and the text "Copy all" copies (one such line per person, in order) with a unit test, and "Copy all" to `Strings`; verify the tests cover a row, one, several, and no accounts
- [x] 1.2 In `PasswordsDialog`, put the copy icon (copying that row as `username: password`) at the end of each row beside the selectable text, and "Copy all" in the dismiss slot when there are two or more rows, with a confirmation and failure state like the row icons; verify on a device, in light and dark, on Android, iOS and the web: each row's icon copies only its own `username: password` line, "Copy all" copies every line, "Copy all" is missing for one row and for none, a refused clipboard shows the failure, and the dialog from "Get passwords", from making a group Pending and from "Reset password" all behave this way
- [x] 1.3 Run `./kotlin test` and `openspec validate add-copy-buttons-to-passwords-dialog --strict`; verify both pass, apart from `aReleaseBuildAlwaysUsesTheRealServer`, which fails on the uncommitted `ServerAddress.kt`

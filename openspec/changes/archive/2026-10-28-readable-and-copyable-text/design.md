# Design

## Context

Read-only text is drawn with plain `Text` in many places: `ItemText` (`ui/wishlist/WishlistScreen.kt`) for a wishlist item's title, note and link, used by the person's own list, the receiver's list and the owner's oversight sheet; `QuestionCard` (`ui/gift/ReceiverScreen.kt`) and the thread in `OversightSheets.kt` for questions and answers; the private `Field` rows of `AdminUserScreen.kt` (also used by the user sheet), of the receiver screen and of the Santa reveal; and the group description on `GroupDetailScreen.kt` and `AdminGroupScreen.kt`. Only the link is limited (one line, ellipsis) and it opens the browser at once through `LocalUriHandler`. The only copyable text is the temporary password, inside a `SelectionContainer`. The project depends on `material-icons-core`, which has no copy icon; the extended set does but is very large. `MULTILINE_MAX_ROWS` (six) already limits editable fields. `AlertDialog` is already used (`PasswordsDialog`). Server limits: title 200, note 1000, link 2000 characters; questions 1000. Decided with the user: six lines everywhere, the copy icon on every data value including username headings, a dialog for links.

## Goals / Non-Goals

**Goals:**
- One component for read-only text with the six-line cut, the full-text dialog and the copy icon, used everywhere the specs name.
- One link dialog.

**Non-Goals:**
- Editable fields, sharing, a toast or snackbar for "copied", changing the data.

## Decisions

### D1: One component, `ReadOnlyText`
```
 ReadOnlyText(text, style, label = null, color = ..., modifier)
 +----------------------------------------+
 | text, maxLines = MULTILINE_MAX_ROWS  [c] |   [c] = copy icon, end of the row
 +----------------------------------------+
 cut? (onTextLayout: hasVisualOverflow)  -> the text is clickable -> FullTextDialog(label, text)
 not cut                                  -> not clickable
```
It lives in `ui/form` next to `FormComponents.kt`. The row is the text with weight 1 and the copy button, aligned to the top of the text. `label` (when the caller has one, such as "Address") is the dialog's title. `FullTextDialog` is an `AlertDialog` with the title, the whole text in a scrollable column, the same copy icon and a close button.

### D2: Copying
The copy icon puts `text` on the clipboard with `LocalClipboard.setClipEntry` (suspending, so it runs in a `rememberCoroutineScope`). Found in task 1.1: `ClipEntry` is an `expect` class with no common way to build a plain-text entry, so a one-line `expect fun plainTextClip(text)` has an actual per target: Android `ClipEntry(ClipData.newPlainText(...))`, iOS and web `ClipEntry.withPlainText(text)` (which needs an `@OptIn(ExperimentalComposeUiApi::class)`). The deprecated `LocalClipboardManager` is not used. The result is a small state: idle, copied (a check mark for about 1.5 seconds, then idle again) or failed (an error tint and a "Could not copy" description until the next tap). On the web `setClipEntry` does nothing, and does not fail, on a page that is not secure, so a second `expect fun clipboardAvailable()` (true on Android and iOS; on the web `window.isSecureContext` and `navigator.clipboard.writeText`) is checked first and counts as a failure. The shown text of a cut note is shortened by the component itself and ends with an ellipsis, because the platform adds none when the cut falls at a line break. Failure matters on the web, where `navigator.clipboard` exists only on a secure page: `https` or `localhost`. A deployment over plain `http` on a network address (see the Docker setup) cannot copy, and says so through the failed state rather than doing nothing.

### D3: Icons
A copy icon is drawn once as an `ImageVector` (two overlapping rounded rectangles, about 30 lines) in `ui/form`, and the check mark is `Icons.Filled.Check` from the core set. The extended icon set is not added: it would grow the web download for one icon. The button is 36 dp, smaller than the usual 48 dp, so that it does not make every row taller; its `contentDescription` is "Copy" and then "Copied" (for accessibility and for tests).

### D4: The link
`LinkText(url)` is a `ReadOnlyText` variant: one line with an ellipsis in the link colour, the copy icon at the end, and choosing it opens `LinkDialog` instead of the browser. `LinkDialog` is an `AlertDialog` with the whole link wrapped (and selectable), the copy icon, a confirm button "Open in browser" (which calls `LocalUriHandler.openUri` inside `runCatching` as today, then closes) and a dismiss button "Cancel". `ItemText` uses `LinkText`, so the person's list, the receiver's list and the oversight sheet all change at once.

### D5: Where it is applied
| Place | Texts |
|---|---|
| `ItemText` | title, note (and the link through `LinkText`) |
| `QuestionCard`, oversight thread | question, answer |
| User detail (Users tab, user sheet) | username heading; name, email, phone, address, delivery information; the temporary password |
| Receiver screen | username; name, address, phone, delivery information |
| Santa reveal | username, full name |
| Group detail (participant and admin) | description |
Not given a copy icon: labels, screen titles, status words and notes written by the app, dates and counts, buttons, and the group name and the participant rows of the lists, which are navigation rows. The group's date and location lines are left alone (short, entered by the owner, and not asked for).

### D6: Taps
On the person's own wishlist an item is a `Card(onClick = ...)` that opens the editor. A `ReadOnlyText` that was cut has its own `clickable`, which takes the tap first, and the copy icon is a button, so the rest of the card still edits. Text that was not cut has no click of its own, so the card's tap passes through.

### D6b: Back and dialogs
The dialogs are Compose dialogs: Android's back button, the iOS interaction and the web's Escape key close them, as they do for the other dialogs. A dialog is not a route, so the web address does not change. The full-text dialog opens over a bottom sheet in the oversight screens, which Compose supports.

### D7: Strings and tests
New strings (copy, copied, could not copy, close, open in browser) go in `Strings.kt`. The cut detection and the dialogs are UI behavior without a view model, so they are verified by running the app, as for other composables; the clipboard state machine (idle, copied, back to idle, failed) is a plain class that gets a unit test.

### D8: Verification
In the web app against a real server (headless Chrome): a wishlist item with a 1000-character note and a 2000-character link, an address and delivery information of many lines; the cut at six lines, the full-text dialog, the copy icon (reading the clipboard back with a granted permission), the link dialog with both buttons, the own-list tap that still edits, the oversight sheet and the user detail. On the Android emulator the same checks where the tooling allows it. iOS is checked by the user.

## Risks / Trade-offs

- [The common clipboard API may not build a plain-text entry on every target] -> Task 1.1 settles it first; the fallback is the older manager.
- [No clipboard on an insecure web page] -> The failed state, and a sentence in the README.
- [A copy icon on every value adds noise and width] -> Small button, no icon on labels and navigation rows; the icon is part of what was asked for.
- [`hasVisualOverflow` is only known after layout] -> The text becomes clickable one frame after it first shows, which is not noticeable.
- [Two dialogs for one item (the full note, then the link)] -> They are separate taps on separate texts, never nested.

## Migration Plan

Nothing to migrate. Rollback is the previous build.

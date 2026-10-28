# Design

## Context

Plain forms are `Column`s with `verticalScroll` and no insets handling (Profile), or no scrolling at all (Change password, Sign in); the group form and the sheets already have `imePadding()` and a scrolling column. Text fields are `OutlinedTextField` declared separately in each screen. The multi-line ones are the Profile address and delivery information (`minLines = 2`, no maximum, the address with `ImeAction.Next`), the group description, the wishlist note and the question and answer sheet (the last three already `maxLines = 6`). Field values live in the view models and come back through a `StateFlow`; sheet fields edit an editor object held in the screen's UI state. The server trims a profile value and checks its length (500) but keeps line breaks inside it, and the Santa's receiver page shows the address in a `Text`, so line breaks need no server or Santa-side change. The iOS cursor bug occurs only in bottom-sheet fields, in all of them, on the first typed character only, and has not been reproduced here. See proposal.md and specs/.

## Goals / Non-Goals

**Goals:**
- Reach the primary button with the keyboard open, dismiss the keyboard by dragging, write an address on several lines, one six-row rule for every multi-line field, and a correct cursor in iOS sheet fields.

**Non-Goals:**
- Server changes, length limits, tap-outside-to-dismiss.

## Decisions

### D1: One form container
A shared composable `FormColumn` (in `ui/form`) wraps the content with `imePadding()` and `verticalScroll` over a `ScrollState`, and installs a nested-scroll connection that closes the keyboard when the person's drag scrolled the form:
```
 pointerInput (Initial pass, consumes nothing) : finger down?
 outer nestedScroll connection (above the scroll container)
   onPreScroll : remember scrollState.value
   onPostScroll: if finger down and source == UserInput and scrollState.value changed
                 -> clear focus / hide keyboard
 verticalScroll(scrollState)
   fields ...
```
Two checks tell a drag of the form apart from the other scrolling. The scroll position must change, which a field scrolling its own text does not cause. And a finger must be down: the first version used only the source of the scroll, and running it showed that when typing makes a field grow, the framework scrolls the form to keep the cursor in view and reports that scroll as `UserInput` too, which closed the keyboard in the middle of typing. A drag that starts in a field and has more to scroll than the field can take is passed on to the form, so it does move the form and does close the keyboard, as specified.
`imePadding()` on the form is exact only if the screen does not also reserve room for the tab bar: the shell hides its tab bar while the keyboard is open (it is behind the keyboard anyway), otherwise the bar's height is counted twice and the form is clipped well above the keyboard. Profile, Change password, Sign in and the group form use the container; the sheets keep their own `ModalBottomSheet` and use the same connection on their scrolling column.

### D2: One multi-line field
A shared `MultiLineTextField` (value, label, error, minLines, `onValueChange`) fixes `maxLines` to a single constant `MULTILINE_MAX_ROWS = 6`, uses `ImeAction.Default` so Return is a line break, and replaces the separate multi-line fields in the Profile, group form, wishlist note and question and answer sheets. Single-line fields stay as they are. The Profile address changes from `ImeAction.Next` to the field's default; the person moves on by tapping or scrolling.

### D3: Line breaks need no new server rule
`Profile.of` trims the outside of a value and keeps what is inside. A domain test pins that: an address with a line break inside is kept, and one with blank lines around it is trimmed.

### D4: The iOS cursor, by investigation
Known: only sheets, every field, only after the first character, so the text field is fine on its own and something about being in a sheet resets the selection. Candidates, tried in this order on the iOS simulator, each checked by the reporter typing "abcd" into every sheet field:
1. Keep the field's text in a local `TextFieldValue` inside the sheet (remembered per opened editor) and push only its text to the view model, so the selection is owned by the field and not rebuilt from the string that comes back.
2. Use the state-based text field (`TextFieldState`) in the sheets.
3. Take `imePadding()` off the sheet's content on iOS (the sheet may already move for the keyboard, so the insets change twice around the first keystroke).
4. The iOS host's keyboard-focus behavior in `ComposeUIViewController`'s configuration, if the version in use offers one that moves the view for a focused field.
5. If none works, narrow it to a minimal sheet with one field, report it upstream with the Compose Multiplatform version, and write the limit in the README.
The first candidate that gives "abcd" in all of the wishlist item (title, note, link), question, answer and participants fields is kept, and the cause found is written into this design file.

Result: candidate 1 fixed it. The reporter confirmed on iOS that typing "abcd" now gives "abcd" in the sheet fields, so the other candidates were not needed. Candidate 1 is `SheetTextField` (`ui/form`), used by every sheet field and, through `MultiLineTextField`, by every multi-line field. The cause was not isolated further, but the fix points at the selection being rebuilt from the string that came back after the first keystroke, in the sheet only; on Android it types correctly and clears when the caller clears it (checked in the participants sheet).

### D5: Tests
Common tests: the shared constant and field defaults where they can be reached without a UI, and the domain test of D3. The container and the field are verified by running the app: on the Android emulator (keyboard covering Save, drag dismissing it, Return in the address, a long address scrolling inside its field, the Santa's page showing the lines) and in headless Chrome. iOS is verified by the reporter.

## Risks / Trade-offs

- [A drag that starts inside a focused field and scrolls only the form would also dismiss the keyboard] → That is a drag that scrolls the form, which is the specified behavior.
- [The cursor fix cannot be verified from here] → Each candidate is handed to the reporter to try on iOS; none is marked done until they confirm.
- [Address no longer moves to the next field on Return] → Intended; tapping or scrolling moves on, and Save is reachable.

## Migration Plan

Nothing to migrate. Rollback is the previous build.

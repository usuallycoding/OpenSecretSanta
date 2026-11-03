## Context

The app closes the keyboard on a drag in three separate places: `dismissKeyboardOnDrag` in `FormComponents.kt` (a form that actually scrolled), the drag watcher and nested-scroll hook inside `AppBottomSheet` (a sheet, for drags nothing used), and nothing at all on the screens with a search above a list. The signed-in app is one `ShellScreen` with a `NavHost`; the sign-in and password-change screens are siblings of it under one `Surface` in `AppContent` in `App.kt`. Sheets and dialogs are their own windows and are not under that `Surface`.

## Goals / Non-Goals

**Goals:** one definition of "a gesture that closes the keyboard", applied once at the app's root, for swipes in any direction and for taps on anything that does not handle them.

**Non-Goals:** sheets and dialogs, which keep their own rule; the Android back button; changing what a drag or tap does to the screen underneath.

## Decisions

- **One file for the pieces.** `KeyboardDismiss.kt` in `app/shared/src/ui/form/` holds a gesture tracker, a pointer watcher that uses it and a nested-scroll hook. `AppBottomSheet` uses the pieces with its existing rules, so its behaviour does not change; `FormColumn`'s hook stays, since it also scrolls the form.
- **A tracker with no UI in it.** A small class takes pointer positions and consumed flags and says whether a gesture was a tap or a drag past the touch slop and that nothing used it. It is pure, so it is tested without a device.
- **Swipes.** The pointer watcher runs in the final pass, after the children, and treats a drag past the touch slop that no child consumed as a swipe. A vertical scroll is judged by the nested-scroll hook, which for the root counts any user scroll, consumed or not, so a list that scrolls also closes the keyboard. Horizontal nested scrolling is ignored: it is how a single-line field scrolls its own text, and must not close the keyboard.
- **Taps.** The same watcher treats a release without drag that no child consumed as a tap. A text field's own tap handling consumes its tap, which is why a tap on another field moves focus and does not close the keyboard; buttons, chips and cards consume theirs and so do their own thing.
- **A touch that starts in a text field is the field's own.** Counting every vertical scroll would also close the keyboard when someone scrolls the text of a multi-line field, which `client-app` forbids. So the app's text fields mark the touches that begin on them (`touchesOfAField`, in the initial pass, with the time of the touch), and the root ignores any swipe, scroll or tap whose touch carries that mark. A drag is judged by whether anything used its moves, not its first touch, so a swipe that starts on a card still counts.
- **Not tied to the keyboard being open.** The sheets read the keyboard's insets, but on the web they are not reported, and it is where the keyboard fails to close most. The root modifier hides the keyboard and clears the focus on every such swipe or tap; with no keyboard open that clears the focus of a field, which is what tapping outside a field does in most apps.
- **Where.** One modifier on the `Surface` content in `AppContent`, so the shell, the sign-in screen and the password-change screen all get it, and the top and bottom bars of the shell are inside it.
- **Sheets are unchanged.** They are other windows, so the root modifier never sees them; their tap on the dimmed area and their swipes keep working as in `client-app`.

## Risks / Trade-offs

- A drag past the touch slop that starts on a button and moves away is unconsumed and now closes the keyboard; that is intended.
- Clearing the focus on a tap in empty space also affects a desktop browser, where the person may have kept the field focused to type on; it matches what a click outside a field does elsewhere.
- Text-selection handles and the context menu are drawn in their own windows on Android and consume their gestures; on the other platforms the check against consumed gestures is what keeps a selection drag from closing the keyboard, and is part of the device check.

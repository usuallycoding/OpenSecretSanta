## 1. The shared pieces

- [x] 1.1 Add `app/shared/src/ui/form/KeyboardDismiss.kt` with a pure gesture tracker (tap, or a drag past the touch slop, and whether anything consumed it), the pointer watcher that uses it and the nested-scroll hook, and test the tracker
- [x] 1.2 Make `AppBottomSheet` use the shared pieces with its present rules, so a sheet behaves as it does now

## 2. The app-wide rule

- [x] 2.1 Add a modifier that closes the keyboard and clears the focus on any user scroll, any unconsumed drag and any unconsumed tap, ignoring horizontal nested scrolls
- [x] 2.2 Apply it to the content of the `Surface` in `AppContent`, so the shell, sign-in and change-password screens all have it

## 3. Check

- [x] 3.1 Run `./kotlin test -m shared` and `./kotlin build`
- [x] 3.2 On Android and iOS, and on iOS Safari: with the keyboard open for the Groups, admin groups, users and participants searches, swipe a list, swipe sideways, tap empty space, tap another field, tap a card, drag inside the field and pull a list down; then the sign-in screen and a form

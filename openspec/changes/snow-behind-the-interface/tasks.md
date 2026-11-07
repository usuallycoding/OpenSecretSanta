# Tasks

## 1. Snow behind the interface

- [x] 1.1 In `App.kt` make the snow the first layer inside the `Surface` of `AppContent`, under the screens, passing it whether the snow is on, and remove the snow layer drawn over the app; update the comment that says it is drawn over every screen.
- [x] 1.2 In `ShellScreen.kt` remove the shell's own opaque background fill and make the `Scaffold`'s container colour transparent, leaving the top bar, tab bar and sidebar opaque.
- [x] 1.3 Add `appFieldColors()` to `form/FormComponents.kt` (every container colour the theme's background) and use it in `SheetTextField`, `PasswordField` and the five other outlined fields: `LoginScreen`, `LetterScreens`, `AdminGroupFormScreen`, `GroupPickerFields` and `DeliveryInfoScreen`, so no `OutlinedTextField` is left without it.
- [x] 1.4 Leave sheets and dialogs as they are: add `LocalFieldsOverSnow` (true by default), turn it off for what `AppBottomSheet` holds and around the hand-over dialog's field, and have `appFieldColors()` return the usual colours when it is off; check nothing else in a sheet or dialog is coloured.
- [x] 1.5 Build for Android, iOS and the web and run the shared tests.

## 2. Verification

- [x] 2.1 Look at the snow on the sign-in screen, the groups, a group, Settings and the admin screens, in the light and the dark theme, on a phone and on a wide window: flakes in the open space, behind cards and behind the top bar, the tab bar and the sidebar, and sheets and dialogs over them. Check that no flake shows inside any text field on a screen (sign-in, search, forms, password) and that the fill looks right in the light and dark themes, and that bottom sheets and dialogs look exactly as before.

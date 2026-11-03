## 1. Icons and strings

- [x] 1.1 Draw the visibility and visibility-off icons in `app/shared/src/ui/form/`, like `SwapIcon`
- [x] 1.2 Add `SHOW_PASSWORD` and `HIDE_PASSWORD` to `Strings.kt`

## 2. The field

- [x] 2.1 Give `PasswordField` a trailing button that shows and hides its password, with its own `rememberSaveable` state, the right icon and name, and an unchanged `Password` keyboard type
- [x] 2.2 Hide a shown password on `ON_PAUSE`
- [x] 2.3 Make the sign-in field use `PasswordField`, keeping its Done action, and check the three fields of the password-change screen and sheet

## 3. Check

- [x] 3.1 Run `./kotlin test -m shared` and `./kotlin build`
- [x] 3.2 Check on a device and on the web: each field toggles on its own, a shown password hides when the app goes to the background, and it starts hidden when a screen is opened again

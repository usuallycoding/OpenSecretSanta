## 1. App

- [x] 1.1 In `LinkText` (`app/shared/src/ui/form/ReadOnlyText.kt`) open the link with `LocalUriHandler.current.openUri` inside `runCatching` when it is chosen, directly in the click handler; remove the `open` state and `LinkDialog`, and update its KDoc
- [x] 1.2 Give the link text screen-reader semantics that read it as opening in the browser, with a new string for it in `Strings.kt`
- [x] 1.3 Remove `Strings.WISHLIST_LINK` and `Strings.OPEN_IN_BROWSER`, and any import left unused in `ReadOnlyText.kt`

## 2. Check

- [x] 2.1 Run `./kotlin build` and the `shared` tests (`./kotlin test -m shared -p android`); nothing referred to the dialog, so they should pass unchanged
- [x] 2.2 Run the app on the web target and one phone target: tapping a link opens the browser with no dialog, in the person's own list, a Santa's view and the owner's participant wishlist sheet; the copy icon still copies the whole link without opening it; on web the page opens and is not blocked as a popup

# Tasks

## 1. Strings

- [x] 1.1 Add the title and tab strings and the functions that build them from a username in `app/shared/src/ui/Strings.kt` ("Giving to <username>", "<username>'s Wishlist", "Questions for <username>", "Receiving from Secret Santa", "Your Wishlist", "Respond to Secret Santa", and the Gifter fallbacks), keeping `TITLE_GIFTER` and `TITLE_RECEIVER` for the pair links; add a unit test beside `EmailSummaryTextTest` covering each string, the fallbacks and a 32-character username, and verify it passes with `./kotlin test -m shared`

## 2. Pages

- [x] 2.1 Let a page supply the top-bar title: add the shell state in `ShellScreen.kt`, make `titleOf` use it when set, and clear it when the page leaves; verify by opening the Gifter and Receiver pages and then another screen, and that the old title returns
- [x] 2.2 Pass the two tab labels into `TabbedPages` (`GroupPages.kt`) as parameters, one line with an ellipsis and the full text as the accessibility label; verify with a long username that the row stays on one line
- [x] 2.3 Make `GifterPage` supply "Giving to <receiver username>" and its tab labels from the loaded receiver, with the fallbacks before it loads or when it fails; verify with the receiver loaded, loading and failing (the test server in `app/shared/test/ui/TestSession.kt`) and from the web address `#/groups/2/gifter`
- [x] 2.4 Make `ReceiverPage` supply "Your Wishlist" while the group is Pending or its phase is unknown, and "Receiving from Secret Santa" with the tabs "Your Wishlist" and "Respond to Secret Santa" once it is Active or Archived; verify in a Pending, an Active and an Archived group
- [x] 2.5 Update any existing test that expects the old titles or tab names and verify `./kotlin test -m shared` and `./kotlin test -p jvm` pass

## 3. Specs and integration

- [x] 3.1 Run `openspec validate rename-gifter-receiver-pages --strict` and verify it passes; confirm the pair links on the group page still read "Gifter" and "Receiver"

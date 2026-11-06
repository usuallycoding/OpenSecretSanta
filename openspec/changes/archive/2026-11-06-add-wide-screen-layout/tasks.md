# Tasks

## 1. The wide frame

- [x] 1.1 Add `WIDE_BREAKPOINT` (840dp) beside `MAX_CONTENT_WIDTH` and make `ShellScreen` choose between the bottom bar and a sidebar by the width it is given, keeping one `NavHost`; verify with a Compose test that 839dp shows the bottom bar and 840dp the sidebar.
- [x] 1.2 Move the 640dp cap to the content beside the sidebar and keep the centered column between 640 and 840dp; verify by running the app at 700dp and 1200dp on the web build.
- [x] 1.3 Build the participant sidebar with Groups and Settings and the admin sidebar with Groups (and the administrator's groups under it), Users and Settings, with selection and per-tab state restore as the tabs have; verify the selection and place-keeping with a test for each view.
- [x] 1.4 Add the sidebar's strings to `Strings.kt`; verify nothing is hard-coded in the composables.
- [x] 1.6 Center the top bar title on every screen, narrow and wide; verify by opening a screen of each view at both widths.
- [x] 1.5 Check that resizing across 840dp keeps the open page, its scroll place and typed text, on Android, iOS and the web.

## 2. Page actions in the top bar

- [x] 2.1 Widen `PageTitle` into a page chrome (title and actions) published to the shell and removed when the page leaves; verify the title still follows each page and an old page's actions are gone after navigating.
- [x] 2.2 On wide windows have `AppFab` hand its action to the host: the heading of its column on the Giving and Receiving pages, the top bar elsewhere (new group); verify on a wide window that there is no floating button and on a narrow window that the floating button is unchanged.
- [x] 2.3 Keep the not-available cases (full or closed wishlist, archived group) free of buttons; verify with tests for each case on both widths.
- [x] 2.4 Remove the bottom clearance for a floating button on wide windows; verify the last row of each list is fully visible.

## 3. Group tree in the participant sidebar

- [x] 3.1 Add the shared holder for the group list and the open group's phase, receiver and badge counts, and have the Groups screen and the group page read it; verify the screens still load the same requests once each.
- [x] 3.2 Render the groups one to a line with the open group expanded into Overview, Giving and Receiving, per `client-layout`, including the Pending shape; verify the items and the Pending cases with tests, and the expansion and collapse by running the app.
- [x] 3.3 Make Giving and Receiving each open one page (the Gifter and Receiver routes) and mark the open page's item as selected, the Inbox and Outbox pages counting as Giving and Receiving; verify the selection with tests and that the web addresses are unchanged.
- [x] 3.4 Show the Giving page's wishlist, questions and Inbox, and the Receiving page's wishlist, questions and Outbox, side by side on wide windows, each with its own scrolling and reloading and its button in the top bar, keeping the tab row on narrow windows; verify by running the app at a wide and a narrow width.
- [x] 3.5 Show the badges (unread, unanswered) on Giving and Receiving as the cards count them; verify they match the cards after an answer is sent.
- [x] 3.6 Have the Inbox and Send letter buttons of a group's Overview open the Giving and Receiving pages on wide windows, and leave the Overview itself in one centered column; verify by running the app.

## 4. Dialogs and the owner's pane

- [x] 4.1 Make `AppBottomSheet` a centered, width-limited dialog on wide windows with room above its title, the same content, close paths and keyboard handling; verify the keyboard rules of `client-app` still hold and a sheet on a narrow window is unchanged.
- [x] 4.2 Add the pane beside the owner's group page on wide windows for a participant's wishlist and for a pair, reusing the sheet's content, with its own close control and back handling; verify opening, switching person, closing and back with tests.
- [x] 4.3 Show the same wishlist as a sheet when the window is made narrower with the pane open, and the pane when it is made wider; verify both directions.

## 5. Specs and checks

- [x] 5.1 Check each platform at a narrow and a wide width against the scenarios of the delta specs, including the admin view and the Settings page; record what was checked.
- [x] 5.2 Sync the delta specs into the main specs and archive the change once the checks pass.

# Tasks

## 1. Find the cause

- [x] 1.1 Add the temporary logging as one small helper (in the shell's navigation file) and call it from the tab bar's taps (route, controller identity, back stack before and after `openTab`), from `show` and `followAddresses` in `app/webApp/src/main.kt` (address, controller identity, stack before and after) and from the shell's composition (controller identity); every line starts with `[nav]`; verify with `./kotlin build`
- [x] 1.2 Rebuild the web container, then refresh on `#/profile`, tap Groups and copy the console; record in `design.md` (replacing the open question) which of the three cases it is and the fix it calls for, and stop here for the person to confirm the plan if the fix is more than a small change to the shell or the web host

## 2. Fix

- [x] 2.1 Add the lifecycle of each back stack entry and the visible entries to the logging, and try waiting for the first screen before opening an address; the log showed the cause (in `design.md`) and the wait was removed again because the first screen is only `STARTED` until the page has focus
- [x] 2.2 Rebuild the web container, refresh on `#/settings` and tap Groups, and record the log in `design.md`
- [x] 2.3 Make `openTab` in `app/shared/src/ui/shell/ShellScreen.kt` public, add `TabScreens` (which routes are tabs, and the root of the view the app started in) in `app/shared/src/ui/navigation` with tests, and open a tab screen named by an address through `openTab` in `app/webApp/src/main.kt` (every other address with the plain navigate); verify with `./kotlin test -m shared` and `./kotlin build`
- [x] 2.4 Rebuild the web container, refresh on `#/profile`, `#/settings` and, as an administrator, `#/admin/users`, tap Groups from each and record in `design.md` that it opens, with the `[nav]` lines of one; if it still does nothing the lines say what to change next

## 3. Clean up and check

- [x] 3.1 Remove the logging and verify with a search that the helper's name and `[nav]` appear nowhere in `app`
- [x] 3.2 In the web app, refresh on `#/profile`, `#/settings` and, as an administrator, `#/admin/users`, and from each choose every tab of the view in turn: Groups shows the groups list with the address `#/groups` (`#/admin/groups` in the admin view) and the other tabs show as they do without a refresh; verify that going back after the refresh shows the Groups tab first and that a refresh on a group page still opens it
- [x] 3.3 Run `openspec validate fix-tabs-after-web-refresh --strict` and verify it passes, then run `./kotlin test -m shared`, `./kotlin test -p jvm` and `./kotlin build` and verify they are green

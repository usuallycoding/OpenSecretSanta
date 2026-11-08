# Tasks

## 1. Routes and addresses

- [x] 1.1 Add `AdminGroupPairRoute(id, santa, receiver)` and its address `#/admin/groups/{id}/pairs/{santa}/{receiver}` to `Routes.kt` and `WebAddresses.kt` (parse and print, usernames checked with `USERNAME`, other shapes name no screen), and add a helper that names the owner's pane routes (rules, pair, announcements); verify with `WebAddressesTest` cases for both addresses, a bad username and a missing receiver
- [x] 1.2 Use the helper in the route lists that name a group's screens: the below-a-group set and `isAdminScreen`, `addressOf`/`pathOf`, `titleOf`, the sidebar mapping and the spread set, and add `AdminGroupRulesRoute` and the pair route to the spread set; verify with tests that each owner pane route has an address, a title and a sidebar place, and that `./kotlin build -m shared` succeeds

## 2. The owner's screen

- [x] 2.1 Make `AdminGroupEntry` and `AdminGroupScreen` take the pane route in place of `announcementsOpen`, draw rules, pair or announcements in the pane beside the page on a wide window, remove the rules redirect in `ShellScreen.kt`, and make the narrow-window routes pages (`DrawRulesScreen`, a new pair page with the pair's title and the back action); verify the build succeeds and the pane/page decision has a test
- [x] 2.2 Open the rules (gavel and the draw-refused notice) and the pair link by navigating (`popUpTo<AdminGroupRoute>`, `launchSingleTop`), close by navigating to `AdminGroupRoute` popping the pane's route, and make the participant wishlist button and the route panes replace one another; verify with tests of the navigation decisions (what opens, what closing goes to, the replacement in both directions)
- [x] 2.3 Redirect to `AdminGroupRoute` when the phase does not allow the rules (Draft) or the pair (not drawn, or not in the draw), showing progress until known; verify with tests of the phase rule and the build

## 3. Cleanup and specs

- [x] 3.1 Remove `Rules` and `Pair` from `GroupSheet`, with `openRulesPane`, `openPairSheet`, `RulesSheet`, `PairSheet` and their pane cases, keeping `User` and `Wishlist`; verify the build succeeds and the admin screen and view model tests pass or are updated
- [x] 3.2 Run `openspec validate admin-rules-and-pair-pages --strict`, the full build and all tests (`./kotlin build` and `./kotlin test`) and verify they pass (apart from the known failure of `aReleaseBuildAlwaysUsesTheRealServer` caused by uncommitted local changes)

## 4. On a device

- [x] 4.1 On a phone and on a wide web window, with a Pending, an Active and an Archived group: open the rules and a pair, see the page or the pane and the address follow (`…/rules`, `…/pairs/alice/bob`) and close it; resize with each open and see no dialog; replace a pane with another, the Announcements and a participant's wishlist, and back; open each address by typing and refreshing, and a Draft group's rules and a pair not in the draw, and see the group page; note anything that looks off

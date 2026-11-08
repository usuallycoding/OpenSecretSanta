# Design

## Context

On the owner's screen (`AdminGroupScreen`) the rules, a pair and a participant's wishlist are `GroupSheet` values in the view model's state. A wide window draws the three in a pane (`GroupSheet.opensInPane()`, `GroupPane`); every other case falls through to `GroupSheetHost`, a bottom sheet. That fall-through is why narrowing a window with the rules open shows a dialog. `AdminGroupRulesRoute` exists, but on a wide window its composable sets the state and redirects to `AdminGroupRoute`, so the address loses `/rules`. The pair has no route and is a bottom sheet on a narrow window. The Announcements are the model: `AdminAnnouncementsRoute` is the owner's pane as a route, `AdminGroupEntry` draws it beside the group page on a wide window and `AnnouncementsPage` is its page on a narrow one. See proposal.md for why.

## Goals / Non-Goals

**Goals:**
- The rules and a pair are routes that are a page or a pane by width alone, with the addresses of the spec, so a resize never shows a dialog.
- Rules, pairs, Announcements and the participant wishlist replace one another in the one pane.

**Non-Goals:**
- The user sheet and the participant wishlist button keep their state-driven behavior (pane on wide, sheet on narrow, no address).
- The participant side, the server, notifications and links.

## Decisions

**Two routes: the rules' and a new pair's.** `AdminGroupRulesRoute(id)` stays and loses its redirect; `AdminGroupPairRoute(id, santa, receiver)` is new, with the address `#/admin/groups/{id}/pairs/{santa}/{receiver}` in `WebAddresses` (both usernames checked with the existing `USERNAME` rule, any other shape names no screen). Like `AdminAnnouncementsRoute`, each composable shows `AdminGroupEntry` with the pane when the window is wide, and the page when it is narrow: `DrawRulesScreen` for the rules, a new pair page (the pair's content with a back action, titled with the pair) for the pair.

**The pane the route names, and the pane the state names.** `AdminGroupEntry` gets the pane route as a parameter (rules, pair or announcements, replacing `announcementsOpen`) and `AdminGroupScreen` draws it. The state pane (the participant wishlist, `GroupSheet.Wishlist`) stays; opening it from the screen first navigates back to the group's own route (popping a pane route, `launchSingleTop`) and then sets the state, and opening a route pane clears the state (`dismissSheet()`), so only one pane is ever shown and each replaces the other. The route is what the address follows; the state pane keeps the group's address as today.

**`GroupSheet` loses `Rules` and `Pair`.** `GroupSheet` keeps `User` and `Wishlist`, `opensInPane()` is true for `Wishlist` only and `openPairSheet`/`openRulesPane` are replaced by navigation. The fall-through that drew a pane sheet as a dialog stays only for `Wishlist`, as the spec of the wishlist button says.

**One mapping for the route lists, as the participant side has.** The owner's pane routes are named once, as a small helper beside `isGroupPane()`, and used by the lists that kept missing a route: the screens below a group (back arrow and `isAdminScreen`), `WebAddresses.addressOf`/`pathOf`, the title (`titleOf`: the group's title on a wide window, "Draw rules" or the pair's title on a narrow one), the sidebar mapping (Admin groups stays selected), and the set of spread routes that draw the group page with a pane. Rules and pair join that set; the redirect `LaunchedEffect` is removed.

**What the phase allows, decided where the route is drawn.** Rules need a group that is not a Draft (`canOpenRules`); a pair needs the group to be drawn and the pair to be in the draw. The route's composable reads the group (`AdminGroupViewModel`, keyed per group) and the draw, shows progress until it knows, and navigates to `AdminGroupRoute`, replacing itself, when the rules or the pair are not allowed. The pair page needs the draw for its title (`pairSheetTitle`), so it waits for the draw as the sheet did.

**Back stack and closing.** Opening a route pane navigates with `popUpTo<AdminGroupRoute>` and `launchSingleTop`, so the stack is the group list, the group and one pane, whatever was open, and the same button again changes nothing. Closing navigates to `AdminGroupRoute` popping the pane's route, as the Announcements do; browser Back closes the pane the same way. The "Open rules" button of the draw-refused notice navigates to the rules' route too.

## Risks / Trade-offs

- [The group page is composed afresh when a route pane opens, so it starts at the top, which the old state pane avoided] → As for the Announcements and the participant panes, accepted; the gavel and the pair links stay reachable by scrolling back.
- [Two kinds of pane on one screen, route and state] → Each opening clears the other, with a test of the decision; the wishlist button can move to a route later.
- [A phase redirect could loop or flash] → The routes redirect only to `AdminGroupRoute`, which no phase rule redirects, and show progress until the phase is known.
- [A missed route in a list gives a missing back arrow or a blank address, as before] → One helper for the owner's pane routes and a test that each has an address, a title and a sidebar place.
- [The existing spec for the participant wishlist says a pane made narrow becomes a bottom sheet] → Unchanged on purpose; that button is out of scope.

## Migration Plan

Nothing is stored or sent differently. The address `…/rules` keeps opening the rules on both widths, and `…/pairs/{santa}/{receiver}` is new.

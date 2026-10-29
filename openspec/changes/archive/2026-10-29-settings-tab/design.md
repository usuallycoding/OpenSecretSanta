# Design

## Context

- `ShellScreen.kt` holds the top bar's `ShellMenu` (view switch for administrators, Appearance, Notifications, Change password, Sign out), the three sheets' open/closed state (`choosingAppearance`, `choosingNotifications`, `changingPassword`) with their `SheetBackHandler`s, and the sheets themselves (`AppearanceSheet`, `NotificationsSheet`, `ChangePasswordSheet`, the last two with keyed view models).
- The bottom bars are two composables, `TabBar` (Groups, Profile) and `AdminTabBar` (Groups, Users), each item opening its screen with `openTab(route, root)`, which keeps each tab's place (`saveState` / `restoreState`) relative to the view's first screen.
- The shell follows addresses and views with `NavDestination.isAdminScreen()`, a Boolean: a destination of the other view moves the shell to that view, and after a view switch the shell opens the new view's first screen when the current destination does not belong to it. A screen that belongs to neither view is not representable.
- `titleOf` picks the top bar title; its `view == AppView.Admin` branch comes before the participant routes, so a participant route shown inside the admin view would be titled "Groups".
- The web addresses are in `WebAddresses` (`addressOf` for routes and for back stack entries, `routeOf` for fragments) and tested in `WebAddressesTest`.
- The core icon set has `Settings` but no swap icon; `GiftIcon` shows how the project draws one that is missing.
- `PushRegistration.isOffered` already says whether notifications are offered on the platform (false on iOS).

## Goals / Non-Goals

**Goals:**
- A Settings tab and page in both views, with the four things the menu held.
- No change to what the sheets contain or how they behave.
- Opening Settings never changes the view.

**Non-Goals:**
- New settings; inlining the sheets' content on the page; a subtitle on the Notifications row (its value is on the server); moving Change password to Profile; changing sign-out itself.

## Decisions

### One shared `SettingsRoute`, neutral to the view
`SettingsRoute` is a `data object` like `ProfileRoute`, with one `composable<SettingsRoute>` in the shell's single `NavHost`. Both bars open it with `openTab(SettingsRoute, root)`, the root being the view's first screen (`GroupsRoute` or `AdminGroupsRoute`), so each view keeps Settings' place on its own.
- The view-following logic has to learn that Settings belongs to neither view. `isAdminScreen()` is replaced by a function that answers the admin view, the participant view or neither (a nullable `AppView`), and its two users change: an address or destination of a view moves the shell to that view, a neutral one does not; after a view switch the current destination counts as "not of the new view" when it is neutral, so the switch opens the new view's first screen, as the spec says.
- In `titleOf` the Settings check comes first, before the `view == AppView.Admin` branch.
- Why not two routes (one per view): two entries for one page, two addresses or a hidden difference between them, for nothing.

### The Settings page: rows that open the existing sheets
`SettingsScreen` (new, in `ui/settings`) is a column of rows, each a full-width pressable `Row` at least 56dp high with the name, an optional supporting line and a trailing chevron: Appearance (supporting line from `AppearanceSettings`: "Follow device", "Light" or "Dark"), Notifications (only when `PushRegistration.isOffered`), Change password, then a divider and a Sign out row.
- The three sheets, their keyed view models and the `SheetBackHandler`s move from `ShellScreen` into this screen, so that the open/closed state lives with the only place that opens them and the shell shrinks.
- Sign out calls `ShellViewModel.signOut()`, passed in as a lambda.
- Alternative: show the sheets' content inline on the page. Rejected for now: it duplicates the screens, changes behaviour the specs already describe, and the sheets work.

### The top bar loses its menu; administrators keep a switch icon
`ShellMenu` and `Strings.MENU` go. The top bar's `actions` is an `IconButton` only when `ShellViewModel.canSwitch`, calling `select` for the other view, with the content description "Switch to participant view" or "Switch to admin view" from the strings that exist. Anyone else's top bar has no actions.
- The icon is a small drawn `ImageVector` (Material's swap-horizontal path), beside `GiftIcon` in `ui/form`, because the core icon set has none.
- The icon alone is the least discoverable of the options; the label is the spoken one. A text action ("Admin view") would be clearer and wider; the icon was chosen, and can be swapped later without touching anything else.

### Bars
Both bars get a third `NavigationBarItem` (`Icons.Filled.Settings`, label from a new string), selected when the destination's hierarchy has `SettingsRoute`; the participant bar's Groups and Profile items and the admin bar's Groups and Users items stay as they are, and the admin bar's Groups item is selected when neither of the other two is, so Settings has to be excluded there as well.

### Web address
`addressOf(SettingsRoute)` gives `#/settings`, the entry-based `addressOf` handles the destination, and `routeOf` accepts `settings` with no further parts, for everyone (it is not an admin address). `WebAddressesTest` gains the cases in both directions and a check that an administrator and a participant both get the route.

## Risks / Trade-offs

- [The shell flips an administrator out of the admin view when Settings is opened] → the view-of-destination function returns "neither" for Settings, and a task checks it on the phone as an administrator in both views.
- [Settings is titled "Groups" in the admin view] → the Settings check is first in `titleOf`, and the same task looks at the title.
- [The switch icon is less obvious than the menu entry was] → it stays in the one place administrators already look, and is announced by its label; revisit if it proves hard to find.
- [Moving the sheets' state could change their behaviour] → the sheets and their view models are untouched; only the place that holds the "open" flag moves, and each is opened and closed on the phone.
- [Three bar items on a narrow screen] → the Material bar is built for three to five.

## Migration Plan

Nothing to migrate: a client-only navigation change. Old web addresses still work, and `#/settings` is new. Rolling back is restoring the menu and the two-item bars.

## Open Questions

- Which settings row icons, if any, to add later; the first version is text rows with chevrons.

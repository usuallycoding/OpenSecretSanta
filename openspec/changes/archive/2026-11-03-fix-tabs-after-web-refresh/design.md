# Design

## Context

The tab bar opens a tab with `openTab`: it pops back to the view's first screen, saves the popped state, goes to the tab with single-top and restores what the tab had. The web host opens an address with a plain `navigate(route) { launchSingleTop = true }`, from a `LaunchedEffect` in the shell that can run before the `Scaffold` has composed the `NavHost`'s first screen. The browser is kept in step by `bindToBrowserNavigation`, which writes the whole back stack into the history entry and pushes a new entry whenever it changes. An administrator starts in the admin view, so a refresh on a participant screen starts from the admin start screen and then switches the view. See proposal.md for the symptom.

## What is known

- After a refresh on a Profile, Settings or Users screen, choosing the Groups tab does nothing, in the participant and in the admin view (reported twice, with the Settings tab not yet tried).
- The address bar does not change, no request is made and the console has no error. A change of the back stack would have changed the address (the binding writes it) and loaded the groups list, so the tap does not change the back stack and does not throw.
- Reading the code, with a back stack of the start screen and the refreshed tab, a tap on Groups pops the tab and leaves the start screen, which should work; and nothing found explains the admin view, where the stack has the normal shape. So the code as read does not account for the symptom.

## What the log showed

Refreshing on `#/settings` as an administrator, then tapping Groups (one controller throughout):

```
shell controller=A stack=AdminGroupsRoute
followAddresses controller=A openedWith=#/settings
show #/settings        before=AdminGroupsRoute   after=AdminGroupsRoute > SettingsRoute
destination=null                                  <- the first destination the shell sees, after show has run
destination=SettingsRoute
openTab AdminGroupsRoute   before=AdminGroupsRoute > SettingsRoute
openTab done               after=AdminGroupsRoute > SettingsRoute     <- nothing changed
```

So it is case 2: the tap arrives, the controller is the one on screen, and the call changes nothing. It also shows that `show` pushes the refreshed address before the shell has observed any destination, that is, on a start screen that has not been composed or shown. Pushing marks the start entry as in transition until it is shown, and a start screen that never was shown is the likely reason a later pop to it does nothing. The exact line of Navigation that ignores the call has not been found.

A second round with more detail (lifecycle of every entry) showed the pop did start: after the tap the entries were `[AdminGroupsRoute:CREATED > SettingsRoute:STARTED]` and the visible entries held two Settings entries, the one being popped and a new `CREATED` one. That new one is Navigation restoring the screen that was just left.

## Cause

`openTab` pops with `popUpTo(root) { saveState = true }` and then navigates with `restoreState = true`. For a pop that is not inclusive, Navigation saves what it popped and files the saved state under the id of the screen popped to, the root, but only if nothing is filed under that id yet. Groups is the root, so the same call that follows then restores that state:

```
after a refresh:  [Groups, Settings]   nothing is filed under Groups
tap Groups:       pop Settings and save it, filed under Groups
                  navigate(Groups, restoreState) restores it  ->  [Groups, Settings] again
```

In normal use something is already filed under the root. Leaving Groups for another tab runs the same pop with nothing above Groups, which files an empty entry under it, and the later restore finds nothing to bring back. A refresh skips that step because the address is pushed with a plain navigate. The stack is the right one in both cases; only that bookkeeping differs.

## Confirmed

With the fix in the build the browser was running, refreshing on `#/settings` and tapping Groups opened the groups list. An earlier retry had still shown the old behaviour only because the browser was running an older copy of the app (the log still held a line the new build no longer wrote); signing out and in again loaded the new one.

## Where it can be swallowed

```
tap on "Groups"
  1. never reaches the button          something covers the tab bar after a refresh, or the item ignores the tap
  2. reaches openTab, the call is ignored   the navigation controller accepts it and changes nothing
  3. runs on another controller        the controller the bar drives is not the one the screen shows
```

## Goals / Non-Goals

**Goals:**
- Find which of the three it is, from a log of one refresh, and fix it.
- A refreshed tab screen has the stack and tab state of a tab that was chosen.

**Non-Goals:**
- No change to the addresses, to the browser binding, or to Android and iOS.
- No change to how a refresh on a non-tab screen (a group or one of its pages) opens.
- No logging left in the app.

## Decisions

- **Diagnose before fixing.** Temporary logging, written to the browser console, records: each tap on a tab, with the route it opens, the controller's identity and the routes on its back stack before and after the call; the address that was opened, with the identity of the controller `show` used and the stack before and after; and each time the shell is composed, with the identity of its controller. Case 1 shows as no line for the tap, case 2 as a tap whose stack does not change, case 3 as different identities. The logging is one small helper so that it can be removed in one step.
- **A tab screen named by an address is opened through the tab bar's function.** That call files the empty entry under the root even when nothing is popped, which is exactly the step a refresh skips. `openTab` becomes public, and the web host uses it for every screen the tab bars open (Groups, Profile, Settings, Admin groups, Users), with the first screen of the view the app started in as the root; every other address keeps the plain navigate. Which routes are tabs, and how the root is found, is one small object (`TabScreens`) with tests.
- **No wait for the first screen (tried and removed).** The first screen showed as `STARTED` for two seconds because the page had not been given focus yet, so waiting for `RESUMED` only delayed the address. It was never the cause.
- **The shape of the stack is unchanged:** a refreshed tab is on top of the view's first screen, so going back from it returns to Groups first, as the spec for the back button already says. Alternative: start the navigation on the address's screen. Rejected: it would leave nothing under the tab and make the back button leave the app from Profile.
- **The fix for the cause the log shows is decided when it is known** and written here before it is made. If it is more than a small change to the shell or the web host, work stops and the artifacts are updated first.
- **No unit test of the navigation itself.** The behaviour is Navigation, the browser's history and the first frame working together; a test would need a browser and a real `NavHost`.

## Risks / Trade-offs

- [The cause may still be missed] → The log separates the three cases, and the manual check covers every tab of both views after a refresh on each tab screen.
- [A fix that needs a log needs a round trip with the person who can reproduce it] → One rebuild and one copy of the console; the rest of the change does not wait for it.
- [Waiting for the first screen delays an opened address by a frame] → Not noticeable; there is nothing to look at before the first screen anyway.
- [Logging that is forgotten] → A task removes it and a search for the helper's name must find nothing.

## Open Questions

- None. The logging has been removed.

## Migration Plan

None: no data or API change. Rollback is reverting the app code.

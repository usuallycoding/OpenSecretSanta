# Design

## Context

After `client-foundation` the signed-in app is `App` → `AppContent` (session `when`) → `ShellScreen`, a placeholder with filter chips for the admin and participant views, driven by `ShellViewModel(account, session, scope)`. There is no navigation library and no back stack. Screens are stateless composables over view models that take a `CoroutineScope` and are created through Koin; tests use `ScriptedServer` (a `MockEngine`) with a real `SessionManager`. `ApiClient` has `login`, `account` (`GET /me`) and `changePassword`, and every answer is an `ApiResult`. The contract already has `MemberGroupDto`, `AccountDto` and `ProfileUpdateRequest`; `core/domain` has `Profile.of(...)` (trim, blank to null, email check, 500 characters). The server needs no change. See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- Real, platform-correct back navigation (Android back, iOS swipe, browser history) from the first screen that needs it.
- A small amount of reusable plumbing (load state, retry, refresh) that the next screens (wishlist, questions) will use unchanged.
- Screen logic in view models that tests can drive without Compose.

**Non-Goals:**
- Wishlist, receiver, questions, reveal and any admin screen. Notifications and live updates.
- A design system or icon set beyond what the scaffold needs.

## Decisions

### D1: The Compose Multiplatform navigation library, with typed routes
`org.jetbrains.androidx.navigation:navigation-compose` 2.10.0 (the latest stable when planned), through `libs.versions.toml`, plus `settings: kotlin: serialization: json` for `app/shared` so routes can be `@Serializable` objects. Routes are `GroupsRoute`, `GroupRoute(id: Long)` and `ProfileRoute`. The catalog's lifecycle library is 2.11.0, newer than the one navigation 2.10.0 was built against; dependency resolution takes the higher one, but this is the one real compatibility risk, so the first task adds the dependency and builds every target (Android, iOS, wasm) before anything uses it. *Alternative:* a hand-rolled back stack (no dependency, but each platform's back handling and the browser history would be ours to build).

### D2: Structure of the signed-in app
```
 App(gate, onNavigationReady)
  AppContent: session -> SignedIn(account)
   SignedInShell(account)               key(username): a new person gets a fresh back stack
    ShellViewModel                      which view (as now) + sign out
    Scaffold
      TopAppBar   title | back arrow on GroupRoute | menu: [Switch view] [Sign out]
      body        Participant: NavHost(start = GroupsRoute)
                                  GroupsRoute --tap--> GroupRoute(id)
                                  ProfileRoute
                  Admin:       placeholder (unchanged)
      BottomBar   [Groups] [Profile]   participant view, top-level screens only
```
The tabs use the standard pattern (`popUpTo(start) { saveState = true }`, `launchSingleTop`, `restoreState = true`), so each tab keeps its own place. `Groups` shows as selected on `GroupsRoute` and `GroupRoute`. Switching between the participant and admin views recreates the participant `NavHost`; the spec only asks for the choice to last for the run, not for the place within a view. The bottom bar stays visible while a group is open (with Groups selected), so a person can switch tabs from there and find the group as they left it. The filter chips are removed; the switch moves into the top-bar menu. Material 3's `Scaffold` handles the system insets on edge-to-edge Android and on iOS. *Alternative:* a navigation drawer; rejected, the bottom tabs are the conventional place for two peer destinations on a phone.

### D3: Browser history on the web
`App` takes `onNavigationReady: suspend (NavController) -> Unit = {}` next to the existing `gate`, and the participant shell calls it from a `LaunchedEffect`. The web host passes `{ window.bindToNavigation(it) }` (the library's browser-history binding, an experimental API), so the back button and the URL follow the navigation. After a refresh the session is gone (memory-only), the sign-in screen shows, and the URL still names the screen; once signed in the app opens that route, which for a group the person is no longer in simply shows "no longer available". If the binding misbehaves, the fallback is to leave it unbound and note it; it is verified in a real browser.

### D4: Calls and types
`ApiClient` gains `groups()`, `group(id)` and `updateProfile(request)` (`GET /groups`, `GET /groups/{id}`, `PUT /me`) with the existing contract types, using the same private `call` helper, so authentication, 401 handling and error mapping are identical to every other call. The profile is read with the existing `account()`. A 404 for a group arrives as `ApiError.Rejected(404, ...)`.

### D5: Load state, retry and refresh
```
 LoadState<T>(value: T?, loading: Boolean, error: ApiError?)
   first load      (null,  true,  null)    -> progress indicator
   loaded          (v,     false, null)
   failed, no data (null,  false, e)       -> message + "Try again"
   refresh         (v,     true,  null)    -> keeps showing v, thin progress bar
   refresh fails   (v,     false, e)       -> keeps v, shows a message + "Try again"
```
A small `Loader<T>(scope, fetch)` owns the flow and `refresh()` (ignored while a load is running), and one composable (`LoadContent`) renders the five cases for any screen. `GroupsViewModel`, `GroupDetailViewModel` and the profile load use it, and so will the next changes. Screens call `refresh()` on `ON_RESUME` (`LifecycleEventEffect`); the first resume after composition is ignored because the initial load is already running. Error texts come from `ApiError`: `Network` becomes the unreachable message, anything else the generic failure message; `Unauthorized` never reaches a screen because the session handles it.

### D6: Formatting and states
Pure common functions with tests: `formatWhen(date, time)` turns `2026-12-24` and `18:30` (or `18:30:00`) into `24 Dec 2026, 18:30`, English month abbreviations, no timezone logic, anything unparsable is returned unchanged rather than failing; a group state string maps to Pending, Active or Archived with its note, and an unknown value is shown capitalised with no note. The "you" mark compares usernames ignoring case.

### D7: The profile form
`ProfileViewModel` loads with `account()`, keeps the five fields as text, and compares them with the saved values to decide whether saving is available. Before sending it calls `Profile.of(...)`, the same function the server uses, and shows a `DomainException.ValidationFailed` next to the field it names (it reports the first problem). A save sends all five fields and the unchanged username with blanks as null (`PUT /me` replaces everything), and the answer becomes the new saved baseline. Server validation errors name a field and land on it; a network failure keeps the edits. A reload on `ON_RESUME` only replaces the saved baseline and never the person's edits. The hint about name and address looks at what is typed right now, so it disappears as the person types. The view model lives with the tab's back stack entry, so switching tabs keeps edits.

### D8: Icons and strings
Icons are optional: labels carry every action. If `material-icons-core` resolves on all targets it supplies the back arrow and the menu glyph; otherwise text labels ("Back", "Menu") are used. All new sentences go into the existing `Strings` object.

### D9: Tests
Common, no UI harness: `ApiClient` (the three calls, 404 and 400 mapping, the request body of a profile save) and `Loader` (first load, retry, refresh keeping data, refresh failing, a second refresh ignored while running); `GroupsViewModel`, `GroupDetailViewModel` and `ProfileViewModel` over `ScriptedServer` extended with `/groups`, `/groups/{id}` and `PUT /me`; the formatters; the Koin verify test extended. Navigation, tabs and the scaffold are verified by running the app (Android emulator, iOS, web), because they are layout and platform behavior.

## Risks / Trade-offs

- [Navigation 2.10.0 against the catalog's lifecycle 2.11.0, and its wasm target] → The first task adds it and builds all three targets before use; if it fails, pin the lifecycle version or take the previous navigation release.
- [The browser-history binding is experimental] → Isolated behind one callback (D3), verified in a real browser, and removable without touching the screens.
- [Typed routes need the serialization plugin in `app/shared`] → Added in the same first task; the contract module already uses it, so the setup is known to work with Amper.
- [One reported validation problem at a time] → The server rule set is small; acceptable, and the form can later collect all problems.
- [Recreating the participant `NavHost` when switching views loses the place within it] → Matches the spec (only the choice lasts); revisit if administrators complain.
- [No live updates] → Refresh on resume and a refresh action are enough for now; notifications are a separate change.

## Migration Plan

Nothing to migrate: new screens and one new dependency, no server change. Rollback is the previous build.

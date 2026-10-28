# Design

## Context

The web entry point calls `window.bindToNavigation(navController)` (the browser-history binding of the navigation library, in its default mode). In that mode the library writes each route as a URL fragment (the route's serialized name with its arguments, which is where the class names come from), pushes a history entry for each navigation, restores the back stack from the entry's saved state on back and forward, and, because no custom address function is given, also reads the fragment on load and when it changes by hand and navigates to it. The library documents that once a custom address function is given it no longer knows how to read addresses, so opening, refreshing or typing an address has to be done by the app. The routes are typed objects (`GroupsRoute`, `GroupRoute(id)`, ..., fourteen of them in `ui/navigation/Routes.kt`). The shell builds the `NavHost` once a person is signed in and then calls `onNavigationReady(navController)`; sign-in and the session that survives a refresh are already in place. See proposal.md and specs/.

## Goals / Non-Goals

**Goals:**
- A readable address per screen, followed by the address bar; refresh, open and typed addresses show the named screen.

**Non-Goals:**
- Addresses without the `#`, addresses for sheets, the old class-name addresses.

## Decisions

### D1: The addresses
```
 route                       address
 GroupsRoute                 #/groups
 GroupRoute(id)              #/groups/{id}
 WishlistRoute(g)            #/groups/{g}/wishlist
 ReceiverWishlistRoute(g)    #/groups/{g}/receiver/wishlist
 ReceiverRoute(g)            #/groups/{g}/receiver
 SantaQuestionsRoute(g)      #/groups/{g}/questions
 SantaRoute(g)               #/groups/{g}/santa
 ProfileRoute                #/profile
 AdminGroupsRoute            #/admin/groups
 AdminGroupNewRoute          #/admin/groups/new
 AdminGroupRoute(id)         #/admin/groups/{id}
 AdminGroupEditRoute(id)     #/admin/groups/{id}/edit
 AdminUsersRoute             #/admin/users
 AdminUserRoute(username)    #/admin/users/{username}
```
Usernames are lowercase letters, digits, `.`, `_` and `-`, which need no escaping in a fragment. A trailing slash is tolerated. The hash form needs nothing from the host, unlike real paths, which would need the host to serve the app for every path.

### D2: One mapping, in common code
`WebAddresses` (in `ui/navigation`, common) has two pure functions: `addressOf(entry route)` giving the fragment for a screen and `routeOf(fragment, isAdmin)` giving the typed route for an address, or null when it names no screen or an admin screen to a non-administrator. It lives in common code so a test can run it without a browser, including a round trip over every route.

### D3: Binding and reading
The web entry point binds the browser history with the address function (so addresses are written as in D1) and adds what the library no longer does by itself:
- on start, after the controller is ready, read the fragment and `navigate` to `routeOf(...)` if it names a screen;
- listen for `popstate` and `hashchange` and, when the entry has no saved navigation state (an address typed or pasted), do the same.
Back and forward keep working through the library's saved state, which does not depend on how the address looks. `onNavigationReady` gains `isAdmin` so the guard of D2 can decide; the shell passes it from the signed-in account. An address that names no screen is ignored, so the start screen stays.

### D4: Signed out
The address bar is left alone while the sign-in screen is shown, so after signing in (or after a refresh with a stored session) the same read of the fragment shows the screen named there.

### D5: Tests
Common tests of `WebAddresses`: every route round-trips through its address, each address of D1 is exactly the one given, a trailing slash and an unknown address, an admin address for a non-administrator, and the old class-name form giving nothing. The behavior is verified by running the web app in headless Chrome against a real server: the address of each kind of screen, refresh on several screens, typing an address with the hash, back and forward, an unknown address, an admin address as a participant, and opening an address while signed out then signing in.

## Risks / Trade-offs

- [The library's own address reading is off once a custom function is given] → The entry point does it, with the same two triggers (start, typed address) and a test of the mapping.
- [Old bookmarks stop working] → They open the first screen; the app has few people and the addresses were never meant to be kept.
- [An address for a screen that no longer exists (a deleted group)] → The screen says the group is no longer available, as it does when reached by clicking.

## Migration Plan

Nothing to migrate. Rollback is the previous build.

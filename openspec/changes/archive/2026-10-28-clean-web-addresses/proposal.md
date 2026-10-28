# Proposal

## Why

Every screen already has an address in the web app's address bar, but it is the Kotlin class name of the route, for example `#com.usuallycoding.opensecretsanta.ui.navigation.AdminGroupRoute/2`. That is unreadable, impossible to type or share by hand, and it leaks the code's structure into something people see and bookmark. With the browser's refresh now being the web's refresh (the two previous changes), the address is also what brings a person back to the same screen, so it deserves to be a deliberate, stable part of the app.

## What Changes

- **Readable addresses.** Each screen has a short, readable address after the `#`, for example `#/groups/2`, `#/groups/2/wishlist`, `#/profile`, `#/admin/groups/2` and `#/admin/users/alice`. The address bar follows the screens as the person moves between them.
- **The app reads its own addresses.** Opening or refreshing an address, or typing one into the address bar, shows that screen once the person is signed in. The navigation library only does this for its own class-name addresses, so the app now does it itself for the new ones.
- **A sensible fallback.** An address the app does not recognise shows the first screen. An address of an admin screen opened by someone who is not an administrator also shows the first screen, instead of a screen that can only fail.
- **Unchanged.** Back and forward in the browser, the session that survives a refresh, and every screen's content. Bottom sheets are still not part of the address.

Out of scope:
- Addresses without the `#` (they would need the web host to serve the app for every path), addresses for sheets, and keeping the old class-name addresses working (a bookmarked old address opens the first screen).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-app`: the web address of each screen is readable, followed by the address bar and understood when opened, refreshed or typed.

## Impact

- Code: a mapping between the app's routes and addresses in `app/shared` (common, so it can be tested), the web entry point (`app/webApp`) that binds the browser history with it and navigates to the address on load and when it is typed, the shell passing whether the person is an administrator, README.
- APIs: none. Server: none. Dependencies: none.
- Platforms: web only; Android and iOS are unaffected.

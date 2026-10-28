# Tasks

## 1. The mapping

- [x] 1.1 Add `WebAddresses` (`addressOf` and `routeOf(fragment, isAdmin)`, covering the fourteen routes of D1) in common code with tests: every route round-trips, each address is exactly the one in the design, a trailing slash, unknown and old-form addresses give nothing, an admin address for a non-administrator gives nothing; verify `./kotlin build` and the tests

## 2. The web entry point

- [x] 2.1 Bind the browser history with the address function, read the address on start and on a typed address (`popstate` and `hashchange` without saved state), and pass `isAdmin` through `onNavigationReady`; verify in headless Chrome against a real server: the address of each kind of screen has the form in the design, refresh on a group, a wishlist and an admin screen restores them, typing `#/profile` shows Profile without a reload, back and forward follow the screens, an unknown address and an old-form address show the first screen, an admin address as a participant shows the first screen, and opening `#/groups/2` while signed out then signing in shows that group; note what could not be tried

## 3. Words and the rest

- [x] 3.1 Update the README (the address sentence and a table of the addresses) and run `./kotlin build` and `./kotlin test` for the whole repo

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.

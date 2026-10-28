# Design

## Context

The admin view is one `NavHost` destination set under the shared top bar: `AdminGroupsRoute`, the group form and detail routes. The participant view has a bottom `TabBar` (Groups, Profile) and the admin view has none yet (`bottomBar = if (participant) TabBar(...)`). Switching views clears the back stack and opens the other view's first screen. Screens load through `Loader` and `LoadContent`, reload on `ON_RESUME`, and view models are created by Koin and tested over `ScriptedServer` with a real `ApiClient`; the 2a change added the admin group fake there and the temporary passwords dialog. The contract has `ManagedUserDto(user: AccountDto, temporaryPassword)` and `ResetPasswordResponse(username, temporaryPassword)`, and the routes exist. On the server, promotion is the use case `PromoteUser` (`core/domain/.../ManagedUsers.kt`), its route in `AdminRoutes.kt`, a DI binding, the contract route constant and tests in `core/domain/test/ManagedUsersTest.kt` and `server/test/AdminRoutesTest.kt`. `UserRepository.markAdmin` exists only for it (the first administrator is created from configuration through another path), and one authentication test uses it to change stored state. See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- Look after the accounts the administrator is responsible for: see who still has a temporary password and what it is, and reset one.
- No way left to make an administrator, anywhere.

**Non-Goals:**
- Creating, deleting, editing or demoting users, and owner oversight (see proposal.md).

## Decisions

### D1: The admin tab bar and routes
```
 admin view:  [ Groups | Users ]
   Groups tab -> AdminGroupsRoute -> AdminGroupRoute(id) ...
   Users tab  -> AdminUsersRoute  -> AdminUserRoute(username)
```
The shell shows an admin `TabBar` when the view is Admin, built like the participant one (a tab keeps its own place through `saveState`/`restoreState`) and selected by route: the Groups tab covers the group routes, the Users tab the two new ones. The top bar shows a back arrow below the Users list and the title of each screen. The two typed routes join the same `NavController`, so the address bar and the back button follow them on every platform.

### D2: Calls
`ApiClient` gains `adminUsers()`, `adminUser(username)` and `resetPassword(username)` over the existing routes and contract types. Errors arrive as `Rejected(404, ...)` (a user outside the administrator's scope answers as if they did not exist) and `Rejected(409/400, ...)` are not expected. The username goes into the path unchanged: usernames are restricted to lowercase letters, digits, `.`, `_` and `-`, none of which need escaping.

### D3: View models
```
 AdminUsersViewModel   Loader<List<ManagedUser>>
 AdminUserViewModel(username)   Loader<ManagedUser> + confirming + created password + notice
```
`ManagedUser` holds the username, the optional profile fields (blank values left out), `mustChangePassword`, and `temporaryPassword` from the server. The detail model exposes `canReset` (false for the administrator's own username, known from the signed-in account). `askReset()` sets a confirmation, `confirmReset()` sends the reset and puts the answer's password in `created` (shown in the dialog) and reloads the user, so the detail then shows them as having to change it and shows the new temporary password the same way the server returns it. `dismissCreated()` clears it. A 404 reloads and the screen shows the user as gone; a network failure is a notice and changes nothing. The password lives in the loaded user only because the server returns it for as long as it is temporary; nothing writes it anywhere else.

### D4: The dialog is shared
The 2a passwords dialog becomes a small shared composable taking a list of (username, password) and a body sentence, used by the group detail and by the reset. Its wording changes to say a password can be looked up under Users until the person changes it.

### D5: Removing promotion
Delete `PromoteUser`, its binding and route, `ApiRoutes.ADMIN_USER_PROMOTE` and `adminUserPromote`, `UserRepository.markAdmin` with its SQLite implementation (nothing else can grant admin rights, which is the point), and the promotion tests. The authentication test that used `markAdmin` goes: the stored-state rule is still covered by the existing test that rejects an older token after a password change. A request to the old path then falls through to the server's usual 404. The specs lose the promote requirement and the scenarios that exist only for it (the authentication requirement and the client-app view requirement are re-added under new names with their remaining scenarios, because a MODIFIED requirement must keep every scenario it had).

### D6: Tests
Common tests over `ScriptedServer`, extended with a fake of the admin user routes (a managed set that includes the administrator, profile fields, temporary passwords that appear and disappear, reset, scoping, scripted failures):
- `ApiClient`: the three calls, a 404.
- Users list: order, only managed users, marks, empty profile, unreachable then retry.
- User detail: fields left out when unset, temporary password shown and hidden, reset with confirmation and cancel, own account offers no reset, 404 as gone, a network failure.
- Server: the old promote path answers 404 for an administrator, the remaining admin routes still pass, and an older token is still rejected after a password reset.

Screens are verified by running the app: the Android emulator against a real server as an administrator and as the reset user, and headless Chrome for the web.

## Risks / Trade-offs

- [Removing a route is a breaking change for any script that used it] → The app never called it; the proposal marks it breaking and a 404 is the answer.
- [The temporary password is visible to the administrator for as long as it is temporary] → That is the server's existing rule (user-administration spec); the app shows it only on that screen and keeps it nowhere else.
- [A reset signs the person out] → The confirmation says so.

## Migration Plan

Nothing to migrate: the promote route had no data of its own. Rollback is the previous build.

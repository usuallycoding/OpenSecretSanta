# Tasks

## 1. Domain

- [x] 1.1 Add `handOverAdmin(from, to): Boolean` to `UserRepository`, documented as one all-or-nothing step that returns whether it was applied.
- [x] 1.2 Add the `HandOverAdmin` use case: administrator only; unknown form or unmanaged target is `NotFound`; self, an existing administrator and a target with a temporary password are `InvalidState`; the password is checked against the stored hash, failures counted in the sign-in `LoginAttemptTracker` (`TooManyAttempts` once locked, cleared on success); a `false` from the repository is `InvalidState`.
- [x] 1.3 Add the in-memory fake behaviour for the new repository method and domain tests for every refusal, the password and attempt limit, the success path, and that groups, created accounts and both creator records move as specified.

## 2. Server

- [x] 2.1 Implement `handOverAdmin` in `SqliteUserRepository` in one transaction with the six statements of the design, each repeating its own condition, rolling back when the role of either person is not as expected.
- [x] 2.2 Add `HandOverRequest(password)` and the route `POST /admin/users/{username}/hand-over-admin` (204) to the contract and the admin user routes, and wire the use case in `ServerModules`.
- [x] 2.3 Repository tests: role swap, all four kinds of group move, created accounts, the target's own creator cleared, the old administrator re-parented, rollback when the target changed password state or is already an administrator, and two handovers at once succeeding at most once.
- [x] 2.4 Route tests: success then 403 for the old token and success for the new one, wrong password (400, nothing moved), the attempt limit (429 with `Retry-After`), non-admin 403, unmanaged 404, self and temporary-password and already-administrator 409, the old administrator still a participant, no change to passwords or memberships, and that a restart does not bring the old administrator back.

## 3. App: data and session

- [x] 3.1 Add the API call, `handOverAdmin(username, password)`, and map the answers (400 `password`, 404, 409, 429, network) in the client.
- [x] 3.2 Add `SessionManager.refreshAccount()` that re-reads `/me` and moves `SignedIn` to the new account (keeping the token), and key the shell on username and role so the participant shell replaces the admin shell.
- [x] 3.3 In `AdminUserViewModel` add `canHandOver`, `handOverBlocked`, the dialog state with the typed password and error, and `confirmHandOver()`; on success refresh the account and record the one-time "now the administrator" message.
- [x] 3.4 Show that message once on the Groups tab after the switch, and clear it when dismissed.
- [x] 3.5 View model and session tests: the action absent on one's own page, disabled for a temporary password, nothing sent when cancelled or empty, each refusal, and the account moving to a participant on success.

## 4. App: screens

- [x] 4.1 Add "Hand over administration" to `UserDetail` (so the Users tab and the group sheet both have it), with the line that says why it is disabled for someone on a temporary password.
- [x] 4.2 Add the confirmation dialog with the text of the spec, a password field, a disabled confirm button until a password is typed, and the refusal messages; add the strings.
- [x] 4.3 Make sure leaving the admin screens after the switch leaves no admin back stack and no way into the admin view.

## 5. Docs and verification

- [x] 5.1 Update the README: replace "There is no way to make anyone an administrator" with the handover, and add the route to the table.
- [x] 5.2 Run the domain, server and shared tests, build the Android app and run `openspec validate --specs`.
- [x] 5.3 Try it by hand: hand over to a person who changed their password, see the old administrator return to the Groups tab with the message, open the app as the new administrator and find the groups and accounts, and check the refusals (temporary password, wrong password).

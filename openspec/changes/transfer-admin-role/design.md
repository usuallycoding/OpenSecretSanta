# Design

## Context

Exactly one administrator exists in practice: the first one is created from `ADMIN_USERNAME` and `ADMIN_PASSWORD` on an empty database, and no route grants the role (see the earlier decision recorded in the README and in `client-admin-users`). Authentication loads the user on every request, so `is_admin` is read from the database each time and a change of role takes effect at once. Two tables point at the administrator: `groups.owner_username` (a plain foreign key) and `users.created_by` (`ON DELETE SET NULL`), which decides who manages a person who is in no group (`AccessPolicy`). The session state is `SignedIn(Account(username, isAdmin))`, and the shell is built once per username with that account.

## Goals / Non-Goals

**Goals:**
- A handover that is one atomic step, never leaves zero or two administrators, and cannot be used as a back door.
- The old administrator keeps using the app as a participant without signing in again.

**Non-Goals:**
- Any way to add an administrator, a second administrator, or to take the role back (only the new administrator can hand it over again).
- Notifying the new administrator, pushing the role to an app that is already open, or an audit log.
- Changing how the first administrator is created.

## Decisions

**One repository operation does the whole move.** `UserRepository.handOverAdmin(from, to): Boolean` runs in one transaction and returns whether it was applied, in this order, each statement repeating its own condition so that a concurrent or stale call changes nothing:
1. `UPDATE users SET is_admin = 0 WHERE username = from AND is_admin = 1` (must change one row, else nothing else runs),
2. `UPDATE users SET is_admin = 1 WHERE username = to AND is_admin = 0 AND must_change_password = 0` (must change one row, else the transaction rolls back),
3. `UPDATE groups SET owner_username = to WHERE owner_username = from`,
4. `UPDATE users SET created_by = to WHERE created_by = from`,
5. `UPDATE users SET created_by = NULL WHERE username = to`,
6. `UPDATE users SET created_by = to WHERE username = from`.
Alternative: several repository calls from the use case; rejected because a failure in between would leave the role, the groups and the accounts out of step. No migration is needed: no table or column changes. Order matters: step 4 would otherwise leave the target as their own creator, which 5 fixes, and 6 runs last so that 4 does not move the old administrator's own record twice.

**The use case checks first and the repository checks again.** `HandOverAdmin(users, access, hasher, attempts, clock)`: require the caller to be an administrator; parse the target (unknown form is 404); require `access.canManage(caller, target)` (else 404, as for every other user route); refuse self and an existing administrator (409 `invalid_state`); refuse a target with a temporary password (409); check the password against the stored hash, counting a failure in the same `LoginAttemptTracker` that sign-in uses and answering `TooManyAttempts` once the lock is set, clearing it on success; then call the repository, which re-checks role and temporary password inside the transaction and reports `false` (mapped to 409) if the world changed. The password check comes after the cheap refusals but before anything is written, and a wrong password never reveals whether the target was eligible beyond what the 404/409 above already say to an administrator.
- Using the sign-in tracker means failed handover attempts and failed sign-ins share one budget for that username. That is deliberate (one lock, no second brute-force path) and means a lock from either blocks both.

**Route.** `POST /admin/users/{username}/hand-over-admin` with `HandOverRequest(password)`, 204 on success. It joins the existing admin user routes next to `reset-password`. The name says what moves, and no route is named promote.

**No token handling.** `is_admin` is read per request, so the old administrator's token is still valid and now gets 403 on `/admin/...`, and the new administrator's token works at once. Bumping `token_version` would sign both out and defeat "without signing in again".

**Client: where the action lives.** `UserDetail` is shared by the Users tab and the group sheet, so the action appears in both. `AdminUserViewModel` gets `canHandOver` (not self), `handOverBlocked` (the person still has a temporary password, read from the loaded `ManagedUser`), the dialog state (`handingOver`, typed password, busy, error message) and `confirmHandOver()`. The dialog is an `AlertDialog` with a password field, using the existing password field style. Alternative: a full page; rejected as heavy for one action.

**Client: after success.** `SessionManager.refreshAccount()` re-reads `/me` and updates `SessionState.SignedIn` with the new `Account`. The shell is currently keyed on the username only, so it is keyed on username and role as well, which rebuilds it with the participant account, drops the admin back stack and lands on Groups. The success message is shown once on the Groups tab: a small `HandoverNotice` held by the session or shell view model (not stored anywhere). Alternative: sign the person out with a reason; rejected as it asks for a sign-in the person just proved they can do.

**Client: the new administrator.** Nothing pushes the role to an open app. The next `/me` read (a restart or reload) returns `isAdmin = true` and the existing code shows the admin view. This is a stated limitation, not a task.

**Docs.** The README sentence "There is no way to make anyone an administrator" and the route table change, and `client-admin-users` replaces its "No way to make an administrator" requirement.

## Risks / Trade-offs

- [A person on a temporary password cannot take over] → this is the point (the old administrator can read it); the action is disabled with a reason, and resetting is the administrator's own tool to get the person onto their own password first.
- [The handover cannot be undone by the old administrator] → heavy confirmation with the password; the new administrator can hand it back.
- [An app open as the new administrator does not know yet] → the next account read fixes it; their server requests already work.
- [The old administrator's own account has no group and no owner] → re-parented under the new administrator, who can manage and delete it.
- [A wrong password locks sign-in as well] → one shared limit by design; the lock is short and says when to try again.
- [Several administrators in an old database] → the handover still works for whoever calls it; the target must not be an administrator, so two never become one by accident. The "exactly one" statement in the specs describes the count staying the same, not a guarantee of one.

## Migration Plan

No schema change. Deploy server and app together; an older app simply has no handover button. Rolling back leaves any completed handover in place (the data is ordinary).

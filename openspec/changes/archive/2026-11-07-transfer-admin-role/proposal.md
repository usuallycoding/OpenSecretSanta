# Proposal

## Why

The administrator account is tied to one person, with no way to pass the role on: someone who leaves, changes role or loses an account can only be replaced by editing the database. Promoting a second administrator stays unwanted (the rule that no admin makes another admin), so the role is **handed over** instead: it moves to someone else and leaves the old administrator in the same step, so there is always exactly one.

## What Changes

- On the **Users** tab, an administrator can open a user's page and hand the administrator role over to that person.
- The handover is one step: the person becomes the administrator and the old administrator becomes an ordinary user. The number of administrators never changes.
- Everything linked to the old administrator moves to the new one: every group they own (in any state, Draft included) and the accounts they created. The new administrator's own record and the old administrator's account are re-parented so the old administrator is managed by the new one, like any account the administrator created.
- Only a person the administrator manages can be chosen, never themself, and never someone who still has a temporary password (the old administrator can read that password, so would keep access to the new administrator's account).
- The handover is guarded: the administrator must enter their own password again, after a confirmation that says what moves and that it cannot be undone by them.
- After a handover the old administrator's app returns to the participant view without asking them to sign in again. The new administrator's role takes effect on their next request, and their app shows the administrator view the next time it loads the account.
- **BREAKING (decided):** the earlier rule "there is no way to make anyone an administrator" is replaced by "the only way to give the role is to hand over one's own". The README and the specs that said otherwise are updated. A route that adds an administrator without removing one is still not offered.

## Capabilities

### New Capabilities

- `admin-handover`: the server's handover of the administrator role: who may do it and to whom, what is checked, what moves, the all-or-nothing guarantee and what it does not change.
- `client-admin-handover`: the app's side: the action on a user's page, its confirmation with the password, what happens after, and the refusals it shows.

### Modified Capabilities

- `client-admin-users`: the requirement "No way to make an administrator" is replaced by one that says the only such action is the handover.
- `user-accounts`: how administrators come into existence now also includes the handover.

## Impact

- `core/domain`: a `HandOverAdmin` use case, a port on the user repository for the atomic handover; `AccessPolicy` unchanged.
- `core/contract`: a request with the password and a route under `/admin/users/{username}`.
- `server`: the SQLite repository method (role swap, group ownership and creator re-parenting in one transaction), the route, tests. No migration: no table or column changes.
- `app/shared`: the action, confirmation and password field on the user's page, the view model, `SessionManager.refreshAccount()`, the shell reacting to a change of role, strings, tests.
- `README.md`: the sentence about no way to make an administrator, and the route table.
- The first administrator still comes from `ADMIN_USERNAME` and `ADMIN_PASSWORD` only on an empty database; after a handover those settings stay ignored, so a restart never brings the old administrator back.

# Proposal

## Why

Nothing in Open Secret Santa can be protected yet: the server has no notion of a user. Every later feature (groups, assignment, Q&A, wishlist) needs to know who is calling, whether they are an admin, and which users an admin may manage. The distribution model (admins hand out temporary credentials by hand, users must replace them on first login) also has to exist before groups can create users. This slice delivers that on the server, contract and domain only; client screens follow in a separate change.

## What Changes

- Domain: `User` entity, username/password rules, repository and service interfaces (password hashing, token issuing, temp-password encryption), and use cases for login, change password, update profile, promote, reset password, list/view users, and create-users-from-usernames (the entry point the groups slice will call).
- Contract: DTOs and routes for login, current user, password change, profile edit, and admin user management.
- Server:
  - `users` table with encrypted, retrievable temporary passwords and a `tokenVersion`; minimal `groups` / `group_members` tables that exist only to scope admin access.
  - bcrypt password hashing, JWT issuing/validation, and an auth layer that re-reads the user on every request.
  - Failed-login lockout and per-IP rate limiting on login.
  - A default admin seeded from environment on first boot.
- Server configuration gains `DATA_ENCRYPTION_KEY` (required) plus the admin seed variables.
- **BREAKING**: server startup now requires `DATA_ENCRYPTION_KEY`, and an empty database additionally requires `ADMIN_PASSWORD`; existing deployments and tests must supply them.

Out of scope: client login/password/profile screens, group fields and lifecycle (only the membership skeleton), assignment, Q&A, wishlist, admin demotion, refresh tokens.

## Capabilities

### New Capabilities
- `authentication`: login with username and password, JWT access tokens bound to a per-user token version, the forced password change gate, account lockout and rate limiting on login.
- `user-accounts`: the user record and its fields, immutable normalized usernames, self-service profile edit and password change, retrievable-while-temporary credentials, and the seeded default admin.
- `user-administration`: admin-only user management scoped by group membership: viewing users including temporary passwords, promoting to admin, resetting passwords, and creating users from a list of usernames.

### Modified Capabilities
- `server-runtime`: the environment configuration and fail-fast requirements gain `DATA_ENCRYPTION_KEY` and the admin seed variables.

## Impact

- Code: new `core/domain` and `core/contract` packages; `server` gains `auth`, `user` and `group` data/presentation packages; `ServerConfig` and `Application.module()` change; existing config and application tests need the new variables.
- APIs: new `/auth/login`, `/me`, `/me/password`, `/admin/users...`; `/health` stays public.
- Database: migrations `V002` (users) and `V003` (groups and membership skeleton).
- Dependencies: a bcrypt library, Ktor auth-jwt and rate-limit plugins.
- Systems: operators must set `DATA_ENCRYPTION_KEY` (base64, 32 bytes) and, on first boot, `ADMIN_PASSWORD`; README is updated accordingly.

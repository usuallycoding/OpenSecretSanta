# Tasks

## 1. Domain rules

- [x] 1.1 Add `Username` (normalize to lowercase, `[a-z0-9._-]`, 3-32 chars, validation error naming the problem) and `PasswordPolicy` (min 8, must differ from current) to `core/domain`; verify common tests cover valid, too short, too long, forbidden characters, mixed case and duplicate-ignoring-case equality
- [x] 1.2 Add the `User` entity (nullable profile fields, admin flag, must-change flag, token version), profile validation (valid email when present, each text field at most 500 chars, blank becomes empty) and the `AccessPolicy` rule "admin manages self or members of owned groups"; verify tests for invalid email, over-long field, blank-to-null, and scope for own group, other admin's group, shared user and self
- [x] 1.3 Declare the ports in `core/domain` (`UserRepository`, `GroupMembershipRepository`, `PasswordHasher`, `TokenService`, `SecretBox`, `TempPasswordGenerator`, `LoginAttemptTracker`, `Clock`) and a sealed domain-error hierarchy matching design D8; verify `./kotlin build -m core/domain` succeeds on all platforms

## 2. Use cases (pure Kotlin, in-memory fakes)

- [x] 2.1 Implement `Login` with lockout (5 failures lock 15 min, unknown usernames identical, success resets, locked attempts skip the password check); verify unit tests for each authentication lockout scenario using a fake clock
- [x] 2.2 Implement `ChangePassword` (current password required, policy, token version bump, temp password discarded, must-change cleared) and `UpdateProfile` (rejects username change); verify tests for success, wrong current password, weak/unchanged password and username change attempt
- [x] 2.3 Implement `ListManagedUsers`, `GetManagedUser` (temp password only while must-change), `ResetPassword` (new temp password, must-change, version bump, lock cleared, 12 chars without look-alikes) and `PromoteUser` (any existing user, idempotent, 404 if unknown); verify tests for scope, visibility, reset effects and promotion cases
- [x] 2.4 Implement `CreateMissingUsers` (validate all first, de-duplicate case-insensitively, create only missing, all-or-nothing) and `SeedDefaultAdmin` (only on empty user store, requires password); verify tests for mixed list, invalid entry creating nothing, duplicates, seeding on empty and no-op when users exist

## 3. Contract

- [x] 3.1 Add serializable DTOs and route constants to `core/contract` (login, account, profile update, password change, managed user with optional temp password, admin actions) with no password hash fields anywhere; verify a serialization round-trip test for each DTO and that `./kotlin build -m core/contract` succeeds on all platforms

## 4. Server configuration and dependencies

- [x] 4.1 Add bcrypt, Ktor auth-jwt and rate-limit to `libs.versions.toml` and `server/module.yaml`; verify `./kotlin build -m server` is green
- [x] 4.2 Extend `ServerConfig` with `DATA_ENCRYPTION_KEY` (valid base64 of 32 bytes), `ADMIN_USERNAME` (default `admin`, validated with `Username`) and optional `ADMIN_PASSWORD`; update `ServerConfigTest`, `ServerModulesTest` and `ApplicationTest` fixtures; verify tests for missing, malformed and wrong-length key, invalid admin username, defaults, and that all server tests pass
- [x] 4.3 Document the new variables, a key-generation command (`openssl rand -base64 32`) and the first-boot admin flow in `README.md`; verify the documented server command starts with the new variables

## 5. Persistence

- [x] 5.1 Add migrations `V002__users.sql` and `V003__groups_membership.sql` (plus `login_attempts`) and register them in `index.txt`; verify the migration tests show schema version 3 and that `/health` reports 3
- [x] 5.2 Implement the SQLite `UserRepository`, `GroupMembershipRepository` (including `managedUsernames`) and `LoginAttemptTracker`; verify repository tests on a temp database for CRUD, case-insensitive uniqueness, multi-group membership, cascade delete, lock persistence across a new repository instance, and all-or-nothing batch insert

## 6. Security adapters

- [x] 6.1 Implement bcrypt `PasswordHasher` and AES-256-GCM `SecretBox` (random nonce, tamper detection); verify tests for hash/verify, wrong password, round-trip, distinct ciphertext for equal plaintext, and failure on a modified ciphertext or wrong key
- [x] 6.2 Implement `SecureRandom`-based `TempPasswordGenerator` and the JWT `TokenService` (HS256, 7-day expiry, `sub` and `tv` claims); verify tests for 12-char format without `0 O 1 l I`, token acceptance, expiry, tampering and wrong secret

## 7. Authentication layer

- [x] 7.1 Wire Ktor authentication: validate tokens against the freshly loaded user and token version, answer 401 for missing/invalid tokens, and apply the `password_change_required` gate to all authenticated routes except `GET /me` and `PUT /me/password`; extend `StatusPages` with the domain-error mapping from design D8; verify HTTP tests for each authentication token scenario, the gate, and that promotion applies to an old token
- [x] 7.2 Implement `POST /auth/login` with lockout (429 plus `Retry-After`) and the per-address rate limit of 20/min; verify HTTP tests for success, case-insensitive username, identical wrong-password/unknown-user responses, lock after five failures, lock expiry with a fake clock, and the 21st request being limited

## 8. Account and admin routes

- [x] 8.1 Implement `GET /me`, `PUT /me` and `PUT /me/password` (returns a new token and invalidates the old one); verify HTTP tests for profile edit, clearing a field, invalid email, username change rejection and the password-change scenarios
- [x] 8.2 Implement `/admin/users`, `/admin/users/{username}`, `.../reset-password` and `.../promote` with the 403 for non-admins and 404 for out-of-scope users; verify HTTP tests using group rows inserted directly for scope, shared user, temp password visibility and hiding, reset effects, and promotion of a user outside the admin's groups

## 9. Startup wiring and end-to-end check

- [x] 9.1 Run `SeedDefaultAdmin` after migrations in `Application.module()` and register all new components in Koin; verify tests for first-boot seeding, restart with different `ADMIN_PASSWORD` changing nothing, startup failing with `ADMIN_PASSWORD` named on an empty database, and a Koin `verify()` of the server graph
- [x] 9.2 Run `./kotlin build` and `./kotlin test` for the whole repo, then start the server with the new variables and verify with `curl`: admin login returns `mustChangePassword: true`, other routes return `password_change_required`, changing the password unblocks them, and a user created by a test fixture shows a temp password to its group's owner only

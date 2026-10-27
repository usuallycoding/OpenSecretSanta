# Design

## Context

The foundation change left `core/domain` and `core/contract` empty, a JDBC `Database` with a numbered migration runner (next version is `V002`), Koin modules on the server (`configModule`, `databaseModule`, `presentationModule`), and `JWT_SECRET` validated but unused. See proposal.md for motivation and specs/ for required behavior. Constraints that shape the approach: `core` modules compile for JVM, Android, iOS and wasm, so crypto, JDBC and JWT live in `server`; SQLite is single-writer and the user base is small, so per-request lookups are cheap.

## Goals / Non-Goals

**Goals:**
- Domain rules (username format, password rules, access scope, state transitions) expressed in pure Kotlin and tested without HTTP or a database.
- A single place that decides "may this admin manage that user?", reusable by the groups slice.
- Behavior that is safe by default: no plaintext temp passwords at rest, no user-existence leaks on login.

**Non-Goals:**
- Client-side token storage and screens.
- Group fields and lifecycle (this slice only creates ownership and membership tables).
- Refresh tokens, admin demotion, email delivery of credentials, audit logging.

## Decisions

### D1: Layering of the new code
```
core/domain     User, Username, PasswordPolicy, TempPassword rules,
                UserRepository, GroupMembershipRepository,
                ports: PasswordHasher, TokenService, SecretBox, Clock,
                       TempPasswordGenerator, LoginAttemptTracker
                use cases: Login, ChangePassword, UpdateProfile,
                           GetOwnAccount, ListManagedUsers, GetManagedUser,
                           ResetPassword, PromoteUser, CreateMissingUsers,
                           SeedDefaultAdmin
core/contract   LoginRequest/Response, AccountDto, ProfileUpdateRequest,
                ChangePasswordRequest/Response, ManagedUserDto, ApiRoutes
server          bcrypt/AES-GCM/JWT adapters, SQLite repositories, Ktor
                auth + routes, Koin wiring
```
Use cases depend only on the ports, so they run in plain unit tests with in-memory fakes; the server tests cover adapters and HTTP. *Alternative:* put the logic in route handlers (less code, but untestable without Ktor and not reusable by the groups slice).

### D2: Users table and credentials
`V002` creates `users`:
`username` (PK, lowercase), `full_name`, `email`, `phone`, `address`, `delivery_info` (all nullable), `password_hash`, `temp_password_enc` (nullable BLOB), `must_change_password` (0/1), `is_admin` (0/1), `token_version` (int, starts 1), `created_at`. Username uniqueness-ignoring-case is achieved by always storing the normalized form as the key. Passwords are hashed with bcrypt (cost 12) through `at.favre.lib:bcrypt`; *alternatives:* Argon2 (needs a native library, awkward to package), PBKDF2 from the JDK (no dependency, but weaker against GPUs and bcrypt is already adequate here).

### D3: Retrievable temporary passwords
The temp password is encrypted with AES-256-GCM (random 12-byte nonce prepended to the ciphertext) using `DATA_ENCRYPTION_KEY`, in `temp_password_enc`; the hash in `password_hash` is what login checks, so the encrypted copy is only for admin display. Setting a password or the seed admin clears/never sets the encrypted copy. The key is separate from `JWT_SECRET` so that rotating one does not break the other. *Alternative:* plaintext column (simpler, but a leaked DB file exposes live credentials). Losing the key makes stored temp passwords unreadable; an admin reset issues a new one, so no recovery tooling is needed.

### D4: Access tokens and the auth layer
HS256 JWT signed with `JWT_SECRET`, 7-day expiry, claims `sub` (username) and `tv` (token version). Ktor's `authentication { jwt }` validate block loads the user by `sub` and succeeds only if the user exists and `token_version == tv`; the resulting principal carries the *freshly loaded* user, so admin status and `mustChange` are never read from the token (spec: "Current user state on every request"). Changing or resetting a password increments `token_version`. The `password_change_required` gate is a route-level interceptor applied to every authenticated route group except `GET /me` and `PUT /me/password`. *Alternatives:* claims-based roles (stale for up to 7 days after promotion), server-side session table (more state for no gain at this size).

### D5: Lockout and rate limiting
- Lockout state is a small table `login_attempts(username PRIMARY KEY, failures, locked_until)`, keyed by the normalized username whether or not that user exists, so unknown usernames behave identically (spec). It lives in the DB so a restart does not clear locks. A failure increments `failures` and, at 5, sets `locked_until = now + 15 min`; success or an admin reset deletes the row. While locked, the password is not even checked and the response is 429 with `Retry-After`.
- Per-address limiting uses Ktor's rate-limit plugin on `/auth/login` only (20/min per remote host), in memory. Behind a reverse proxy the address must come from `X-Forwarded-For`; this is documented but not configured yet.
- Residual risk: an attacker can deliberately lock a known username for 15 minutes. Accepted for a small private app; the per-IP limit bounds the effort and an admin reset unlocks.

### D6: Admin scope as one policy
`V003` creates `groups(id INTEGER PK, owner_username NOT NULL REFERENCES users)` and `group_members(group_id REFERENCES groups ON DELETE CASCADE, username REFERENCES users, PRIMARY KEY(group_id, username))`. The groups slice extends `groups` (name, state, etc.). A single repository query, `managedUsernames(admin)`, returns the admin themself plus members of groups they own; every `/admin/users/{username}` route authorizes through it and answers 404 when the target is outside it (not 403, so existence is not leaked). Promotion deliberately bypasses scope (spec) and looks up the user directly. Tests insert group rows directly, since no group API exists yet.

### D7: Bulk user creation and seeding
`CreateMissingUsers(usernames)` normalizes and validates every entry first, de-duplicates, then inserts the missing ones with encrypted temp passwords in one SQL transaction, so a bad entry creates nothing. `SeedDefaultAdmin` runs in `Application.module()` after migrations: if the `users` table is empty it creates the admin with `must_change_password = 1` and no encrypted copy; otherwise it does nothing. The "empty DB needs `ADMIN_PASSWORD`" rule is enforced there (a `ConfigurationException` naming the variable), because it depends on database state that `ServerConfig.fromEnv` cannot see.

### D8: Errors and response shape
Domain failures are a small sealed hierarchy mapped in `StatusPages` to `ErrorResponse(code, message)`: `invalid_credentials` (401), `unauthorized` (401), `password_change_required` (403), `forbidden` (403), `not_found` (404), `validation_failed` (400), `too_many_attempts` (429). Validation errors name the failing field. This extends the foundation's generic 500 handler without changing it.

## Risks / Trade-offs

- [bcrypt at cost 12 makes login ~250 ms of CPU, and the rate limit allows 20/min per address] → Acceptable for this scale; run hashing on `Dispatchers.IO`.
- [Per-request user lookup adds a DB read to every call] → SQLite point lookup on the primary key; revisit only if measured as a problem.
- [Encrypted temp passwords are only as safe as `DATA_ENCRYPTION_KEY`] → Key is env-only, never logged or stored with the DB; documented in the README.
- [Minimal group tables could drift from the groups slice's needs] → Only ids, owner and membership are fixed here; the groups slice adds columns with a new migration.
- [Breaking startup requirements (`DATA_ENCRYPTION_KEY`, `ADMIN_PASSWORD`) break existing local setups and tests] → Update README and test fixtures in the same change; fail-fast messages name the variable.
- [In-memory per-IP limiter resets on restart and does not span multiple instances] → Single instance is the deployment model today.

## Migration Plan

Deploy by setting `DATA_ENCRYPTION_KEY` (generate with `openssl rand -base64 32`) and, on the first boot, `ADMIN_USERNAME` / `ADMIN_PASSWORD`. Migrations `V002` and `V003` apply automatically. Existing foundation databases have only `schema_version`, so they simply gain the new tables and an empty-database seed. Rollback is restoring the previous build and the pre-upgrade database file; the new tables are additive.

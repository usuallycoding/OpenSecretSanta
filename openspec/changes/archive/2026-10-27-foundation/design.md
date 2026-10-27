# Design

## Context

The repo is a fresh Kotlin Toolchain (`module.yaml`) multi-module scaffold: `core` (kmp/lib for jvm, android, iOS, wasmJs), `server` (jvm/app, Ktor/Netty), `app/shared` (kmp/lib with Compose Multiplatform) and three thin hosts (`androidApp`, `iosApp`, `webApp`). Modules are listed in `project.yaml`. See proposal.md - Why for motivation. Because `core` compiles for wasmJs and iOS, it can contain only common Kotlin; SQLite, hashing and JWT signing are JVM-only and belong in `server`.

## Goals / Non-Goals

**Goals:**
- A layering that the build enforces: domain cannot see serialization, Ktor, or UI.
- One Koin setup per runtime (server, client) that later slices extend by adding modules.
- A server that boots reproducibly from env vars and an empty directory.

**Non-Goals:**
- Any user, auth, group or wishlist behavior.
- Production deployment packaging (Docker, CI).
- A design system or navigation structure for the UI.

## Decisions

### D1: Module layout (core split in two)
```
core/domain     kmp/lib  no deps except kotlin stdlib (+ coroutines later)
core/contract   kmp/lib  -> core/domain, kotlinx-serialization
server          jvm/app  -> core/domain, core/contract
app/shared      kmp/lib  -> core/domain, core/contract, Compose, Koin
```
`server` and `app/shared` stay single modules with layered packages (`data`, `presentation`/`ui`, `di`). *Alternatives:* packages-only in `core` (nothing enforces the boundary); fully split data/ui modules (too much ceremony for this project size). The existing `//core` dependency in both module files is replaced with the two new modules, and `GreetingUtil` is deleted with the hello route.

### D2: SQLite via plain JDBC with a hand-rolled migration runner
`org.xerial:sqlite-jdbc` behind repository interfaces defined in `core/domain`. Migrations are numbered SQL files in server resources (`db/migration/V001__*.sql`), applied in order, each inside a transaction, with the version stored in a `schema_version` table. The runner reads the migration list from a classpath index file rather than scanning, because classpath scanning is unreliable in packaged jars. *Alternatives:* Exposed (ORM/DSL overhead for roughly six tables), SQLDelight (strong in KMP, but the DB is server-only so its multiplatform benefit is unused), Flyway (heavy for SQLite and one file). Revisit if the schema grows significantly.

### D3: Configuration as a validated value object
`ServerConfig` is built from an env-var map by a pure function (`fromEnv(map)`) so it is unit-testable without mutating the process environment. Validation failures throw a single `ConfigurationException` naming the variable; `main` lets it terminate the process before binding a port. `JWT_SECRET` is required now though unused until slice 2, so that deployments are configured correctly from the start.

### D4: Serialization lives only in `core/contract`
DTOs are annotated `@Serializable`; domain entities are plain Kotlin and mapped to DTOs at the edges (server routes, client repositories). This costs mapper code but keeps domain free of format concerns. *Alternative:* annotate entities directly (less code, couples domain to JSON).

### D5: Koin wiring
- Server: Koin Ktor plugin installed in `Application.module()`; modules `configModule`, `databaseModule`, `routesModule`. `module()` accepts overrides so tests can supply a temp database path.
- Client: `app/shared` exposes `appModules()` (common) and `initKoin(platformModule, baseUrl)`. Each host calls it once; Android guards against re-initialization via `GlobalContext.getOrNull()` and calls it from `Application.onCreate`, not from the Activity. The base URL is passed in by the host (Android emulator needs `10.0.2.2`, web uses same origin), satisfying the app-bootstrap spec.
- Verification: Koin's `verify()` test on both module sets.

### D6: Health reports schema version
`GET /health` runs `SELECT MAX(version) FROM schema_version`, so a 200 proves both the server and the database are working, and the version aids diagnosing a stale deployment. A failure returns 503, not 500, to signal a dependency problem to monitors.

### D7: Error handling
Ktor `StatusPages` maps `Throwable` to a generic 500 JSON body and logs the detail server-side. Slice 2+ adds specific mappings (401, 403, 404, 409) to the same plugin.

## Risks / Trade-offs

- [Hand-rolled migrations lack tooling like checksum validation] → Keep the runner small and tested; migrations are append-only by convention; adopt Flyway later if needed.
- [Kotlin Toolchain (Amper-style) is newer; Koin/Compose artifact aliases may need exact coordinates] → First task is a build spike adding dependencies and getting `./kotlin build` green before writing code.
- [Wasm target limits library choice (e.g., Ktor client engine, Koin features)] → Use only common Koin APIs on the client; pick the Ktor client engine per platform via the platform module.
- [Mapper boilerplate between domain and contract] → Accepted cost of D4; kept to small extension functions per feature.
- [Android `10.0.2.2` / iOS localhost differences cause confusion in dev] → Document per-platform default base URLs in the README.

# Proposal

## Why

Open Secret Santa is a server plus Android, iOS and web clients that must share as much code as possible and follow clean architecture with Koin for dependency injection. The repository is currently an untouched Kotlin Multiplatform scaffold (hello-world route, no DI, no persistence, no serialization). Every later feature (auth, groups, assignment, Q&A, wishlist) needs the same layered skeleton, wiring and runtime plumbing, so it is built once, first, before any domain logic.

## What Changes

- Split `core` into `core/domain` (pure Kotlin: entities, use cases, repository interfaces) and `core/contract` (kotlinx-serialization DTOs and route constants, depends on `core/domain`). Domain has no serialization, Ktor or platform dependencies.
- Server: add Koin (Ktor integration), JSON content negotiation, centralized error handling, an environment-based `ServerConfig` that fails fast on missing required values, a SQLite connection with a versioned migration runner, and a `GET /health` endpoint.
- **BREAKING**: remove the scaffold `GET /` hello route, its test, and `GreetingUtil` from core.
- App (`app/shared`): add Koin with a common `appModules()`, a layered package skeleton (`data`, `domain`-consuming `ui`), and a `startKoin` entry point called from the Android, iOS and Wasm hosts.
- Tests: server health test, migration runner test, Koin module graph verification on server and client.
- Extend the version catalog with Koin, kotlinx-serialization, Ktor content negotiation and `sqlite-jdbc`.

Out of scope: users, auth/JWT, admin seeding, groups, assignment, Q&A, wishlist, any real UI screens.

## Capabilities

### New Capabilities
- `server-runtime`: server startup behavior: environment configuration with fail-fast validation, SQLite database with versioned migrations applied at boot, and the `/health` endpoint.
- `app-bootstrap`: client startup behavior: Koin initialized on Android, iOS and web before the first screen composes, with shared modules resolving the same way on every platform.

### Modified Capabilities
<!-- none: no specs exist yet -->

## Impact

- Code: `project.yaml`, `libs.versions.toml`, new `core/domain` and `core/contract` modules, `core/src/GreetingUtil.kt` removed, `server/src/Application.kt` rewritten, `app/shared` plus the three host modules gain DI startup.
- APIs: introduces `GET /health`; removes `GET /`.
- Dependencies: Koin (core, ktor, compose), kotlinx-serialization-json, ktor content-negotiation/status-pages, `org.xerial:sqlite-jdbc`.
- Systems: server needs `JWT_SECRET` (reserved for slice 2 but validated now), `DB_PATH` and `PORT` environment variables, with defaults for the latter two.

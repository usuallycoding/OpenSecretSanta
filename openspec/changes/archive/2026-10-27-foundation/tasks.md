# Tasks

## 1. Module layout and dependencies

- [x] 1.1 Create `core/domain` and `core/contract` modules (kmp/lib, same platforms as today; contract depends on domain), register them in `project.yaml`, and verify `./kotlin build -m core/contract` succeeds
- [x] 1.2 Add Koin, kotlinx-serialization-json, Ktor content-negotiation/status-pages, Koin-Ktor and `sqlite-jdbc` entries to `libs.versions.toml` and wire them to the right modules; verify `./kotlin build` is green for all modules
- [x] 1.3 Point `server` and `app/shared` at `//core/domain` and `//core/contract`, delete `core/src/GreetingUtil.kt` and the `core` module if now empty, and verify no references to `sayHello` remain (`grep`) and the build passes

## 2. Server configuration

- [x] 2.1 Implement `ServerConfig.fromEnv(map)` with defaults and validation (`JWT_SECRET` required, `PORT` 1..65535) and a `ConfigurationException` naming the variable; verify unit tests cover defaults, overrides, missing secret, blank secret, non-numeric and out-of-range port
- [x] 2.2 Load config in `main` before binding the port and verify running the server without `JWT_SECRET` exits with an error naming it and with nothing listening on the port

## 3. Database and migrations

- [x] 3.1 Implement the SQLite connection provider and the migration runner (`schema_version` table, ordered numbered SQL resources, per-migration transaction); verify tests for fresh database, rerun (idempotent, data kept) and failing migration (rolled back, version not recorded, startup aborted)
- [x] 3.2 Add the first migration `V001` creating only `schema_version` bookkeeping if not already created by the runner, and verify the schema version reads back as 1 after boot

## 4. Server HTTP surface

- [x] 4.1 Replace the scaffold `module()` with Koin install (`configModule`, `databaseModule`, `routesModule`), JSON content negotiation, and `StatusPages` returning a generic JSON 500; verify a test with a throwing route returns 500 with no stack trace in the body
- [x] 4.2 Add `GET /health` (200 `{status:"ok", schemaVersion}`, 503 `{status:"error"}` when the DB query fails) with `HealthResponse` DTO in `core/contract`; verify server tests for both cases using a temp database path
- [x] 4.3 Remove the old `GET /` route and rewrite `ApplicationTest` for `/health`; verify `./kotlin test -m server` passes
- [x] 4.4 Add a Koin `verify()` test for the server module set and verify it passes

## 5. Client bootstrap

- [x] 5.1 Add `appModules()` and `initKoin(platformModule, baseUrl)` in `app/shared` with a base-URL binding, plus a package skeleton (`data`, `ui`, `di`); verify a common test resolving the base URL from the graph passes
- [x] 5.2 Add a Koin `verify()` test for the client module set and verify it passes on the available test targets
- [x] 5.3 Call `initKoin` from the Android `Application.onCreate` (guarded against re-initialization), the iOS entry point and the Wasm `main`; verify each host builds (`./kotlin build -m androidApp`, `iosApp`, `webApp`) and the Android app launches without a Koin error
- [x] 5.4 Document run commands, required server env vars and per-platform default base URLs (Android emulator `10.0.2.2`, iOS simulator `localhost`, web same origin) in `README.md`; verify the documented server command runs as written

## 6. Integration check

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for the whole repo, start the server with `JWT_SECRET` set, and verify `curl localhost:8080/health` returns `{"status":"ok","schemaVersion":1}`

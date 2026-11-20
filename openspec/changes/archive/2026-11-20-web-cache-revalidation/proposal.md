# Proposal

## Why

When a new API version is deployed, clicking "Reload" on the web app's update-required screen does not load the new version and forces users to manually clear browser cache and cookies. Amper emits unhashed bundle filenames (`webApp.mjs`, `webApp.wasm`, `skiko.wasm`, `styles.css`), and `nginx.conf` currently sets no `Cache-Control` header on them, so browsers serve the stale `.wasm`/`.mjs` bundle from heuristic cache across reloads.

## What Changes

- Configure `app/webApp/docker/nginx.conf` to send `Cache-Control: no-cache` on unhashed web application assets (`.mjs`, `.js`, `.wasm`, `.css`), so browsers keep files cached locally via ETag/`304 Not Modified` when unchanged and immediately fetch the new bundle on reload after a deployment.
- Configure `GET /health` in `server/src/presentation/HealthRoutes.kt` to send `Cache-Control: no-store`, so browsers and reverse proxies never serve a stale `apiVersion`.
- Add test coverage verifying `GET /health` returns `Cache-Control: no-store`.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `docker-deployment`: The web container serves unhashed web application bundles (`.mjs`, `.js`, `.wasm`, `.css`) with `Cache-Control: no-cache` so reloading picks up new deployments without clearing browser cache.
- `server-runtime`: `GET /health` responses include `Cache-Control: no-store` so browsers and proxies do not cache `apiVersion` or health status.

## Impact

- **Web container (`app/webApp/docker/nginx.conf`)**: Adds `Cache-Control: no-cache` to `.mjs` and `.js`/`.wasm`/`.css` locations.
- **Server (`server/src/presentation/HealthRoutes.kt`, `server/test/ApplicationTest.kt`)**: Sets `Cache-Control: no-store` on `GET /health` and verifies it in tests.
- **No API contract or database schema changes.**

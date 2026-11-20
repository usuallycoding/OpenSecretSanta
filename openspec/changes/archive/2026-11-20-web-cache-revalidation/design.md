# Design

## Context

See `proposal.md` for motivation. Amper's WasmJS release build produces unhashed static filenames (`webApp.mjs`, `webApp.wasm`, `webApp.import-object.mjs`, `webApp.js-builtins.mjs`, `import-map-loader.js`, `skiko.mjs`, `skiko.wasm`, `styles.css`). Currently `app/webApp/docker/nginx.conf` sets `Cache-Control: no-cache` only on `/index.html`, `/config.json`, `/push.js`, `/firebase-messaging-sw.js`, and `/.well-known/assetlinks.json`, leaving all `.mjs`/`.js`/`.wasm`/`.css` assets subject to browser heuristic caching.

## Goals / Non-Goals

**Goals:**
- Ensure a normal browser reload (`window.location.reload()`) immediately picks up newly deployed web bundles without clearing cache or cookies.
- Preserve fast repeat visits via standard HTTP conditional caching (`ETag` / `Last-Modified` -> `304 Not Modified`).
- Prevent browsers and reverse proxies from caching `GET /health` (`apiVersion`).

**Non-Goals:**
- Rewriting Amper's generated ES module / WASM import paths to add content hashes to filenames.
- Changing session storage or cookie behavior.

## Decisions

### 1. `Cache-Control: no-cache` on `.mjs`, `.js`, `.wasm`, and `.css` in `nginx.conf`
- Add `add_header Cache-Control "no-cache";` inside the existing `location ~ \.mjs$` block.
- Add `location ~ \.(js|wasm|css)$ { add_header Cache-Control "no-cache"; }`.
- Keep existing exact-match locations (`= /index.html`, `= /config.json`, `= /push.js`, `= /firebase-messaging-sw.js`, `= /.well-known/assetlinks.json`) which take precedence in nginx over regex locations.

**Why**: `Cache-Control: no-cache` instructs browsers and proxies to revalidate via `If-None-Match` (nginx emits `ETag` by default) before reusing a cached copy. Unchanged assets return a 0-byte `304 Not Modified`; updated deployments return `200 OK` immediately.
**Alternative considered**: Post-processing `webApp.mjs` to content-hash filenames for `immutable` caching. Rejected because rewriting Amper-generated internal module/WASM references is brittle across toolchain upgrades.

### 2. `Cache-Control: no-store` on `GET /health`
Set `HttpHeaders.CacheControl` to `"no-store"` in `server/src/presentation/HealthRoutes.kt` on both 200 and 503 responses.

**Why**: Guarantees neither browsers nor intermediate proxies ever return a stale `apiVersion` or health status.

## Risks / Trade-offs

- **One-time transition for already-cached browsers** → Browsers that cached `webApp.wasm` *before* this nginx change was deployed will not have the `no-cache` directive until their prior heuristic cache expires or is cleared once; every subsequent deployment will reload cleanly.
- **Extra `304` revalidation checks on page load** → Minimal latency (~a few tiny conditional requests on initial load) and 0 bytes transferred when unchanged.

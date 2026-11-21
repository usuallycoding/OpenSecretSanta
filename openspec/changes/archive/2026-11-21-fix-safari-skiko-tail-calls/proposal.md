# Proposal

## Why

On macOS Safari (e.g., Safari 27.2) and iOS browsers (all WebKit-based), the deployed web app renders a blank white Compose `<canvas>` over the page background—leaving only the native HTML DOM login form visible (against a plain white canvas instead of the themed surface and snow) and showing a completely white screen after sign-in. This is caused by WebKit bug 329421 / JetBrains `CMP-10986`, where Safari's strict-mode Proper Tail Calls corrupt numeric/float arguments passed through Skiko's 996 `export let name = (...a) => (name = loadedWasm._["name"])(...a)` wrapper functions in `skiko.mjs` into `skiko.wasm`.

## What Changes

- Add a standalone, POSIX-compatible build post-processing script (`app/webApp/docker/patch-skiko-safari.sh`) that rewrites Skiko's tail-call export wrappers in `skiko.mjs` so each wrapper invokes `loadedWasm._[...]` inside a `try { return ... } catch (e) { throw e }` block, preventing WebKit from applying tail-call optimization.
- Run the patch script during the web Docker image build (`app/webApp/Dockerfile`) right after `./kotlin build -m webApp -p wasmJs -v release`, failing the build if `skiko.mjs` is missing or if no tail-call wrappers were patched (unless already patched idempotently).
- Add an automated test verifying the patch script rewrites the Skiko wrapper pattern cleanly, preserves non-matching lines, fails on missing/unmatched input, and is idempotent.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `docker-deployment`: Require the built web container image to patch Skiko's `skiko.mjs` export wrappers against WebKit tail-call argument corruption so Safari and iOS browsers render the web app canvas.

## Impact

- `app/webApp/docker/patch-skiko-safari.sh`: New post-processing script for `skiko.mjs`.
- `app/webApp/Dockerfile`: Runs the patch script on the built release `skiko.mjs` before copying assets into the nginx stage.
- Automated verification script/test covering the patch transformation.
- No Kotlin/Wasm source, API, or server changes; Chrome and Firefox behavior is unchanged.

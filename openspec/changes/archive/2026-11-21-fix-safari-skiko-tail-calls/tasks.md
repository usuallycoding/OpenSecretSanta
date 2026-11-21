# Tasks

## 1. Skiko Safari Tail-Call Patch Script

- [x] 1.1 Create `app/webApp/docker/patch-skiko-safari.sh` (POSIX `sh` + `awk`) to rewrite `export let <name> = (...a) => (<name> = loadedWasm._["<name>"])(...a)` lines in a given `skiko.mjs` into `export let <name> = (...a) => { try { return (<name> = loadedWasm._["<name>"])(...a); } catch (e) { throw e; } }`, updating the file atomically, succeeding idempotently if already patched, and exiting non-zero if the file is missing or contains neither unpatched nor already-patched wrappers; verify by running it against a temporary copy of `build/tasks/_webApp_buildWasmJsAppWasmJsRelease/skiko.mjs` and confirming all 996 wrappers are rewritten with zero unpatched tail calls remaining.
- [x] 1.2 Add a shell test (`app/webApp/docker/test-patch-skiko-safari.sh`) covering normal rewriting, preserving surrounding JS lines, idempotent re-run, missing file failure, and zero-match failure, and verify the test script exits 0 on macOS.

## 2. Docker Build Integration

- [x] 2.1 Update `app/webApp/Dockerfile` to run `sh app/webApp/docker/patch-skiko-safari.sh build/tasks/_webApp_buildWasmJsAppWasmJsRelease/skiko.mjs` immediately after `./kotlin build -m webApp -p wasmJs -v release` in the `build` stage; verify by inspecting the Dockerfile diff and confirming the script path and build output path match.
- [x] 2.2 Run `sh app/webApp/docker/test-patch-skiko-safari.sh` and validate the patched output syntax (e.g., `node --check` if available or verifying no syntax issues on a temporary copy of release `skiko.mjs`) to confirm the end-to-end build artifact is valid JavaScript ready for Docker deployment.

# Design

## Context

See `proposal.md` for motivation. The web app is built with Amper (`./kotlin build -m webApp -p wasmJs -v release`) inside the `eclipse-temurin:21-jdk` build stage of `app/webApp/Dockerfile`, and the resulting static directory (`build/tasks/_webApp_buildWasmJsAppWasmJsRelease/`) is copied directly into `nginx:1.29-alpine`. Unlike Gradle-based Kotlin/Wasm setups, Amper does not use Webpack, so a Webpack loader (`skiko-no-tail-call-loader`) cannot be plugged in.

In the current Compose Multiplatform `1.12.2` build, `skiko.mjs` contains 996 lines matching:
```js
export let <name> = (...a) => (<name> = loadedWasm._["<name>"])(...a)
```
Every line is a strict-mode ES module arrow function whose sole body expression is a direct call into `loadedWasm._[...]`, placing it in ES6 tail position.

## Goals / Non-Goals

**Goals:**
- Rewrite every Skiko tail-call export wrapper in the built release `skiko.mjs` so WebKit cannot apply Proper Tail Call optimization (preventing WebKit bug 329421 / `CMP-10986`).
- Keep the patch step zero-dependency (POSIX `sh` + `awk` available in `eclipse-temurin:21-jdk` and macOS) so no extra runtime (like Python or Node) is required in the Docker build stage.
- Fail the Docker build if `skiko.mjs` is missing or if the expected wrapper pattern disappears (unless the file is already patched), so future Skiko upgrades cannot silently regress or leave dead workaround code unnoticed.
- Keep the script standalone and directly testable on macOS/Linux without requiring Docker.

**Non-Goals:**
- Upgrading Compose Multiplatform / Amper versions in this change (can be revisited once upstream ships a Skiko fix).
- Changing local `./kotlin run -m webApp` dev server behavior (developers can still run the script manually against a debug build if testing locally in Safari).
- Modifying any Kotlin/Wasm application code or `AuthFormHosts.wasmJs.kt`.

## Decisions

### Decision 1: Rewrite using POSIX `awk` in `app/webApp/docker/patch-skiko-safari.sh`
- **Choice**: Use a small POSIX `sh` script wrapping `awk` to transform each matching line:
  ```js
  export let NAME = (...a) => (NAME = loadedWasm._["NAME"])(...a)
  ```
  into:
  ```js
  export let NAME = (...a) => { try { return (NAME = loadedWasm._["NAME"])(...a); } catch (e) { throw e; } }
  ```
  and write atomically via a temporary file in the same directory before replacing the target.
- **Why**: Per the ES2015+ specification (`HasCallInTailPosition`), a `return` inside a `try` block is never in tail position. Using `try { return ... } catch (e) { throw e }` is the standard workaround for WebKit bug 329421 and avoids any extra runtime dependencies in `eclipse-temurin:21-jdk` (unlike `python3` or `node`, `awk` is guaranteed present).
- **Alternatives considered**:
  - `sed -i`: GNU vs BSD `sed -i` flags differ between Linux containers and macOS (breaking local tests), and `sed` cannot easily count replacements and fail on zero matches.
  - `python3` / `node`: Not guaranteed to be installed in `eclipse-temurin:21-jdk` without adding `apt-get` packages.
  - Upgrading Compose Multiplatform: `1.12.2` is currently pinned with Amper and `compose-ui-backhandler`; patching `skiko.mjs` is immediate and low-risk.

### Decision 2: Fail loudly on 0 matches unless already patched
- **Choice**: Track both `patched` (unpatched lines rewritten) and `already_patched` (lines already in the `try { return ... } catch (e) { throw e }` form). Exit non-zero if `patched == 0 && already_patched == 0`; succeed idempotently if `already_patched > 0 && patched == 0`.
- **Why**: Makes the script safe to re-run and ensures that if a future Skiko release changes its export codegen format, the Docker build alerts us instead of silently shipping an unpatched bundle (or reminds us that the workaround can be retired once Skiko fixes it upstream).

### Decision 3: Run the patch inside the `build` stage of `app/webApp/Dockerfile`
- **Choice**: Invoke `sh app/webApp/docker/patch-skiko-safari.sh build/tasks/_webApp_buildWasmJsAppWasmJsRelease/skiko.mjs` right after `./kotlin build -m webApp -p wasmJs -v release` in the `build` stage.
- **Why**: Patches the artifact before copying into `nginx:1.29-alpine`, keeping the final runtime image untouched and minimal.

## Risks / Trade-offs

- [Skiko changes its export wrapper syntax in a future Compose upgrade] → The script fails loudly with a clear message so we can inspect the new `skiko.mjs` and either update the pattern or remove the workaround if upstream fixed `CMP-10986`.
- [Slight increase in `skiko.mjs` size (~40 KB uncompressed for 996 wrappers)] → `nginx.conf` already enables `gzip on` for `text/javascript` / `application/javascript`, and the repeated `try { return ... } catch (e) { throw e }` text compresses to nearly zero extra bytes over the wire.
- [First-call overhead of `try/catch` per Skia export] → Each wrapper self-overwrites `name = loadedWasm._["name"]` on first invocation, so the `try/catch` runs at most once per exported function for the lifetime of the page.

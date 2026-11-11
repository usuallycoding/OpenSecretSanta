## 1. Contract and server

- [x] 1.1 Add `ApiVersion` with `const val CURRENT = 1` (KDoc: raised by hand only for a change an older app would misbehave against) and `apiVersion: Int? = null` to `HealthResponse` in `core/contract`; extend `HealthResponseTest` (round trip with and without it, an older answer that lacks it, the constant is 1)
- [x] 1.2 Make `HealthService` set `apiVersion = ApiVersion.CURRENT` in the OK answer only; update the health tests (OK answer carries it and equals the constant, the 503 answer has none) and the tests that compare the whole `HealthResponse`

## 2. App

- [x] 2.1 Add `updateWay` (`Reload` on the web, `Download` on Android, `None` on iOS) and `reload()` (`window.location.reload()` on the web through the existing JS interop, a no-op elsewhere) to `Platform` and its three actuals; extend the three platform tests
- [x] 2.2 Add `AppCompatibility` with `Checking`, `Ok` and `UpdateRequired(androidDownload)`, and `check()` using `ApiClient.health()`: higher `apiVersion` → `UpdateRequired`, lower, equal or missing → `Ok`, failure → `Ok` while `Checking` and otherwise unchanged; register it in `AppModules`, keep the module-graph test passing, and test it with the scripted server (newer, equal, older, missing, unreachable, 503, a failed check while stopped and while working, a rollback after a stop, and the android flag taken from the stopping answer)
- [x] 2.3 Add the update screen: the text, a "Reload" button on the web, a "Download update" button on Android only when the flag is true (opening `ApiClient.androidDownloadUrl()`), the text only on iOS; add its strings to `Strings.kt`
- [x] 2.4 Wire it in `AppContent`: run the check and `session.restore()` at the same time, show the loading indicator until the compatibility state has left `Checking` and the session has left `Loading`, show the update screen for `UpdateRequired` in place of everything else, and check again on `ON_RESUME`; test the decision of which screen shows from the two states as a pure function

## 3. Docs

- [x] 3.1 Add a README section on the API version: what `ApiVersion.CURRENT` is, when to raise it and when not, what the apps do on a higher number (stop, with Reload on the web, Download update on Android, text on iOS), that apps before this release cannot be stopped, and to upload an APK to `downloads/` and deploy the web app with a server that raises it; add `apiVersion` to the `/health` example

## 4. Check

- [x] 4.1 Run `./kotlin build` and the tests of `core`, `server` and `shared` (`./kotlin test -m shared -p android`)
- [x] 4.2 Manually, with the server's `ApiVersion.CURRENT` temporarily set higher than the app's and never committed: the web app, a signed-in tab and a fresh one, show the update screen, and "Reload" works; with the value restored the app returns; check a tab left in the background while the server changes (whether regaining focus re-checks)
- [x] 4.3 Manually on Android with the same temporary change: the update screen shows, "Download update" appears with an APK in `downloads/` and is absent without one, and returning to the app after restoring the value brings it back

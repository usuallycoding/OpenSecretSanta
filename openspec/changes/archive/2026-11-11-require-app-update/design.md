## Context

See proposal.md for motivation. `/health` is the one route that needs no sign-in and that every platform's `ApiClient` can reach; `android-download` already added `androidDownload` to it and `ApiClient.androidDownloadUrl()`. The server and the apps compile the same `core/contract` module, so a constant there is the same number on both sides of any one build. `App.kt` has an `AppContent` that restores the session once (`SessionManager.restore()` does nothing when the state is no longer `Loading`) and then shows the shell, the sign-in screen or the password change from the `SessionState`; an Android host can wrap it in a `gate`. Other screens already refresh on `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)`. `Platform` already tells the web from the phones, and the web has JavaScript interop (`js(...)`) in `src@wasmJs`.

## Goals / Non-Goals

**Goals:**
- An app that cannot understand the server stops, says why, and offers the way to update that its platform has.
- A check that never gets in the way when it cannot tell: offline, an error answer and an older server leave the app working as today.

**Non-Goals:**
- Apps built before this change (they have no check), an app newer than its server, a minimum version set on the server, a header on every response, forced updates for bug fixes.

## Decisions

### One integer in the contract, raised by hand
`object ApiVersion { const val CURRENT = 1 }` in `core/contract`, with a KDoc that says when to raise it (see `server-runtime`: only for a change an older app would misbehave against). The server reports it; the app compares the server's number with the one it was built with. Because both come from the same constant in one build, "the app's supported version" needs no separate setting that could drift.

Alternatives: a minimum app version in the server's environment (flexible, can force a bug-fix update, but needs a version comparison and one more thing to remember at each deploy), or the app's semver against the server's. Rejected for now: the integer states the actual reason for stopping (the API changed) and is checked by the compiler's constant, not by an operator.

The cost is discipline: forgetting to raise it for a breaking change means no app stops. The README says when to raise it, and a deliberate raise is the one release step that must be paired with a way to get the new app.

### Compatibility is a three-state holder
A small holder, `AppCompatibility(api)`, exposes `StateFlow<Compatibility>` with `Checking` (nothing heard yet), `Ok` and `UpdateRequired(androidDownload: Boolean)`. Its `check()` calls `ApiClient.health()`:
- a success with `apiVersion` above `ApiVersion.CURRENT` → `UpdateRequired(androidDownload = answer.androidDownload == true)`;
- a success with a lower or equal version, or none → `Ok`;
- a failure of any kind → `Ok` if still `Checking` (cannot tell, carry on), otherwise unchanged (a flaky connection neither unblocks a stopped app nor stops a working one).

Keeping `Checking` as its own state is what lets the screen wait without showing the wrong thing.

### Launch: check and restore at the same time
`AppContent` starts both in one `LaunchedEffect`, concurrently, and shows the loading indicator until the compatibility state has left `Checking` and the session has left `Loading`. Sequential (check, then restore) would double the wait on a server that cannot be reached, because each call has its own 10-second connect timeout; concurrent costs nothing there. The restore's `GET /me` against a newer server is harmless to send and its answer is not shown.

The screen decision, in order: `Checking` → loading indicator; `UpdateRequired` → the update screen; otherwise the session's screens as before. Because it is decided above the session `when`, the shell, sign-in and password change are all replaced, and the session itself is untouched, so the person is still signed in after updating.

### Foreground: check again
`LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { check() }` in `AppContent`. A check that finds the API no longer newer returns the app to its session screens. Only the check is repeated: the session is restored at launch whatever the server said (the restore is harmless against a newer server), so the session is never left waiting, and a repeated restore would only start a second one while the first is still running. Whether a browser tab regaining focus counts as a resume for the lifecycle on the web is checked by hand; the launch check and the Reload button work regardless.

### What the screen offers is a platform capability
`Platform` gets `updateWay` (`Reload` on the web, `Download` on Android, `None` on iOS) and `reload()`, which only the web implements, with `window.location.reload()` through the same `js(...)` interop the other web code uses (a no-op elsewhere). The update screen reads `updateWay`:
- `Reload`: text that says to reload to get the new version, and a "Reload" button calling `reload()`.
- `Download` and `androidDownload` true: a "Download update" button that opens `ApiClient.androidDownloadUrl()` with the same `LocalUriHandler` the links use.
- otherwise, and always on iOS: the text only.

The `androidDownload` flag is taken from the very answer that stopped the app, so no second request is needed and the button never leads to a 404.

### Server: one field
`HealthService` sets `apiVersion = ApiVersion.CURRENT` in the OK answer only, as it does `schemaVersion`; the 503 answer carries neither. It is nullable in `HealthResponse` so older servers (no field) and the error answer both read as "unknown" in the app.

## Risks / Trade-offs

- [Apps already out there, including 1.1.5, have no check] → They cannot be stopped; their only protection is that breaking changes are avoided (as the 10-links change did with `url`) until enough people are on a release that has the check. State this when raising the version for the first time.
- [The version is raised by hand and could be forgotten, or raised without need] → The README states the rule; raising it is rare and deliberate. A wrongly raised number stops every app older than the build, so it must be paired with a published APK and a deployed web app.
- [Everyone is locked out until they update] → That is the point. On the web a reload is enough; on Android the download button works only when an APK is in the downloads folder, so upload one before deploying a server that raises the version. On iOS there is only the text.
- [A stopped app after an unlucky deploy order: server first, web second] → The web container and server are redeployed together by compose; an open tab sees the stop on its next resume and reloads into the new bundle.
- [The health call at launch adds a request] → It runs at the same time as the session restore, so the launch is not slower; a server that is down costs nothing extra.
- [A flaky connection] → A failed check never changes the state, so neither a working app is stopped nor a stopped app released by it.

## Migration Plan

1. Ship the server and the apps with `ApiVersion.CURRENT = 1`: nothing stops, since the server's number equals the app's. Apps from before this change keep working as they do.
2. The first time the API must change in a breaking way: build that change, raise `ApiVersion.CURRENT` to 2, upload the new APK to `downloads/`, deploy the server and the web app together, release the iOS and Android builds, and tell people. Apps built with 1 that have this change stop and offer the update.
3. Rollback of the server returns the number to 1 and the apps resume on their next check.

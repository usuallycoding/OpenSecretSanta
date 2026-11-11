# Proposal

## Why

When the server changes its API in a way an older app cannot handle, the app keeps running against it and misbehaves: screens fail to load, saves are refused, data is shown wrongly. Nothing tells the person why or what to do. With a stable download address now in place (`android-download`), the app can stop and say that it needs updating.

## What Changes

- The API gets a version number, `ApiVersion.CURRENT`, kept in the contract that the server and the apps share. It starts at 1 and is raised by hand only for a breaking change, one an older app would misbehave against.
- `GET /health` gains `apiVersion`, the server's `ApiVersion.CURRENT`; it is absent on the error answer.
- The app compares the server's `apiVersion` with its own. Only a clear answer that is higher than the app's stops it. A server that cannot be reached, an error answer, and an older server that gives no `apiVersion` never stop it.
- The app asks at launch, together with restoring the session, and again each time it returns to the foreground.
- When stopped, a full-screen "update required" screen replaces everything, so nothing else is usable: the shell, sign-in and the password change. It says that this version is too old for the server and that the app must be updated.
- What the screen offers depends on the platform: on the web, a "Reload" button that fetches the new version; on Android, a "Download update" button that opens `/download_latest`, only when `/health` says an app is there; on iOS, the text only.
- The README says when to raise the version and what the apps do.
- Apps released before this change, 1.1.5 included, have no check and cannot be stopped; protection starts with the first release that has it.
- Out of scope: an app newer than a self-hosted server, a minimum app version set on the server, a version header on every response, forcing updates for bug fixes, and any change to `distribute-android.sh`.

## Capabilities

### New Capabilities

- `client-update-required`: when the apps stop, what they check and when, what never stops them, and what the update screen offers on each platform.

### Modified Capabilities

- `server-runtime`: `/health` reports `apiVersion`, and the API's version is defined, with when it is raised.

## Impact

- **Contract** (`core/contract`): a new `ApiVersion` object; `HealthResponse` gets `apiVersion: Int? = null`.
- **Server**: `HealthService` fills `apiVersion`; no route or database change.
- **App** (`app/shared`): a holder that checks compatibility, an update screen, the launch and foreground wiring in `App.kt`, two `Platform` additions (how an update is done and a reload on the web), new strings.
- **Docs**: README, a short section on the API version.
- A server deployed with a higher `ApiVersion.CURRENT` stops every app that has this change and is older than it, until the app is updated; the version must be raised deliberately, and only together with a way for people to get the new app.

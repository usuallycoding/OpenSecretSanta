# Proposal

## Why

The Android app is distributed to testers through Firebase App Distribution, and there is no address anyone can be sent to for the latest APK. People who use the web app have no way to find the Android app, and an app that needs updating (a follow-up change) has nowhere stable to point. The server already runs on the Raspberry Pi, so it can hand out an APK that is copied there by hand.

## What Changes

- The server answers `GET /download_latest`, with no sign-in, by sending the newest APK from a downloads folder, as a file download. Without an APK it answers 404 with a short message.
- "Newest" is the highest version code in a file name of the form `opensecretsanta-<version>-<code>.apk`, which is what `scripts/distribute-android.sh` already writes; a name of any other form counts by its modification time and comes after every file named that way. Files that are not `.apk` files, and upload leftovers, are ignored.
- The server reads the folder from `DOWNLOADS_DIR`; both compose files mount `./downloads` (or the folder `DOWNLOADS_HOST_DIR` names) read-only at `/downloads` and set `DOWNLOADS_DIR` to it.
- `GET /health` gains a nullable `androidDownload` flag, true when an APK is available. A missing or unreadable folder only makes it false; health still answers 200.
- On the web only, the sign-in screen shows a "Download Android app" button and Settings shows a "Download Android app" row, both only while `/health` says `androidDownload` is true. Both open `${server address}/download_latest`.
- The README describes copying an APK to the Pi: upload under a temporary name, then rename, so nobody can fetch a half file.
- Out of scope: the update-required screen (a follow-up change, `require-app-update`, which will reuse this address), uploading from `distribute-android.sh`, sign-in or rate limits on the download, and any change to the Android or iOS apps.

## Capabilities

### New Capabilities

- `app-download`: the public `/download_latest` endpoint, how the newest APK is chosen, how it is sent, and what happens when there is none.

### Modified Capabilities

- `server-runtime`: the health body carries `androidDownload`, and the server reads `DOWNLOADS_DIR`.
- `docker-deployment`: both compose files mount `./downloads` read-only into the server and pass `DOWNLOADS_DIR`; the README explains uploading an APK.
- `android-app-distribution`: an APK the script built can be published to the Pi by hand, without a half-uploaded file ever being served.
- `client-app`: the web sign-in screen offers "Download Android app".
- `client-settings`: the web Settings page lists a "Download Android app" row.

## Impact

- **Contract** (`core/contract`): `HealthResponse` gets `androidDownload: Boolean? = null`; `ApiRoutes` gets the download route.
- **Server**: a download route and a small service that finds the newest APK, `DOWNLOADS_DIR` in `ServerConfig`, `HealthService` filling the flag; `docker-compose.yml`, `docker-compose.images.yml`, `.env.example`, README.
- **App** (`app/shared`): a `Platform` flag true only on the web, a health call at the sign-in screen and Settings, the two entries and their strings.
- The download is public: anyone with the address can fetch the APK. It holds no secrets (the release key stays on the maintainer's machine), and testers already receive the same file.
- No database change.

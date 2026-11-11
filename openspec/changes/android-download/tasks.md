## 1. Contract and server

- [x] 1.1 Add `ApiRoutes.DOWNLOAD_LATEST = "/download_latest"` and `androidDownload: Boolean? = null` to `HealthResponse` in `core/contract`; update `HealthResponse` and route tests in the contract module
- [x] 1.2 Add `downloadsDir` to `ServerConfig`, read from `DOWNLOADS_DIR` (default `downloads`, blank is the default), without checking that it exists; update its `toString`, and add config tests (default, set, blank, folder missing)
- [x] 1.3 Add `ApkDownloads(directory)` with `latest(): Path?` in the server's data layer: non-recursive, regular readable `.apk` files only, `opensecretsanta-<version>-<code>.apk` ordered by numeric code, other names after them by modification time, any I/O failure is `null`; test it with a temporary folder (highest code, numeric not text order, other names last, only other names, `.part` and non-APK files and folders ignored, empty, missing folder)
- [x] 1.4 Add the download route, outside `authenticate`, wired in `Application.kt`: the chosen file as `application/vnd.android.package-archive` with an attachment `Content-Disposition` under its own name, 404 with the usual error body when there is none; test it through the application (no sign-in needed, headers, whole file, 404 on empty and missing folder, `/download_latest/` and sub-paths give no file or listing, a `.part` file is never served)
- [x] 1.5 Make `HealthService` set `androidDownload` outside the database check (true with an APK, false with none or an unreadable folder, 200 either way, absent on the 503 answer); update the health tests and the other tests that build `HealthService`

## 2. Deployment and docs

- [x] 2.1 In `docker-compose.yml` and `docker-compose.images.yml` mount `${DOWNLOADS_HOST_DIR:-./downloads}:/downloads:ro` into the server and set `DOWNLOADS_DIR: /downloads`; add `downloads/` to `.gitignore` and `DOWNLOADS_HOST_DIR` (commented) to `.env.example`
- [x] 2.2 Add a README section on publishing an APK: build with `scripts/distribute-android.sh --no-upload`, create `downloads` on the Pi, copy under a `.part` name with a world-readable mode, rename, check `/download_latest` through the public address, and that a proxy or tunnel must allow the file's size; mention `DOWNLOADS_DIR` and `DOWNLOADS_HOST_DIR` where the server's settings are listed

## 3. Web app

- [x] 3.1 Add `offersAndroidDownload` to `Platform` (true only in the `wasmJs` actual) and extend the platform tests
- [x] 3.2 Add `ApiClient.health()` (no sign-in) and `AndroidDownloadOffer`, a holder exposing whether to offer the download that fetches only when the platform flag is set and treats any failure as false; register it in `AppModules`, keep the module-graph test passing, and test the holder (true, false, failure, phones never fetch)
- [x] 3.3 Show a "Download Android app" button on the sign-in screen, below the sign-in action and above the version, and a "Download Android app" row in Settings after "Change password" and before the separation, both only while the holder says so; each opens the contract route on the server address with the link opener; add the strings to `Strings.kt`

## 4. Check

- [x] 4.1 Run `./kotlin build` and the tests of `core`, `server` and `shared` (`./kotlin test -m shared -p android`)
- [x] 4.2 With the stack running (`docker compose up --build`), copy an APK into `downloads` under a `.part` name and check `/download_latest` and the web app show nothing, rename it and check `/download_latest` downloads it, the sign-in screen and Settings now offer it on the web, and choosing either downloads the file without leaving the page; also check the entries are absent in the Android app
- [x] 4.3 On the Pi, through `santa-api.plm360.org`, download the real APK once from a phone and install it over the existing app

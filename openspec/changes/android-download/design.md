## Context

See proposal.md for motivation. The server is a Ktor application whose routes are plain functions on `Route` (`HealthRoutes.kt`, `WishlistRoutes.kt`, ...), wired in `Application.kt`; its settings come from environment variables through `ServerConfig.fromEnv`. `GET /health` is answered by `HealthService`, which reports `status` and the database's `schemaVersion` and answers 503 when the database cannot be queried. The server image runs as an unprivileged user (`santa`, uid 10001) with a `/data` volume; both compose files already mount `./secrets` read-only for the push key. The apps share `core/contract` with the server (`HealthResponse`, `ApiRoutes`), and the app's `Platform` interface already tells the web from the phones (`canChooseToStaySignedIn` is true only on `wasmJs`). The app's `ApiClient` has no health call yet.

## Goals / Non-Goals

**Goals:**
- A file copied to a folder on the Pi is downloadable at `/download_latest` with no restart and no sign-in.
- The web app offers the download only when there is something to download.

**Non-Goals:**
- Resumable downloads, authentication, rate limits, a page that lists versions, or uploading from the script.
- Anything in the Android and iOS apps; the update-required screen is a separate change that reuses the address.

## Decisions

### A small finder in the server's data layer, a thin route on top
`ApkDownloads(directory: Path)` has one function, `latest(): Path?`, which lists the folder (not recursively), keeps regular, readable files whose name ends in `.apk`, and picks the newest. The route and `HealthService` both call it, so "is there an APK" and "which APK" can never disagree. Any I/O failure (folder missing, not a directory, unreadable) is `null`, never an exception, which is what makes the 404 and the health flag fall out without special cases.

Newest is chosen with a name parser: `opensecretsanta-<version>-<code>.apk` with a numeric `<code>` is compared by that number as a `Long`, so `10000` beats `900`; names of any other form come after all of those, ordered by last-modified time, newest first. The file name is always taken from the directory listing, never from the request, so nothing a client sends can name a file or reach outside the folder.

Alternative: a fixed file name (`latest.apk`) that the maintainer overwrites. Rejected: it loses the version and makes a half-copied file the live one; with names that carry the code, old APKs can stay for a rollback and the newest still wins.

### Half-copied files are not served by being invisible
Only names ending in `.apk` count, so copying to `opensecretsanta-1.1.6-9100.apk.part` and renaming when done (a rename on one filesystem is atomic) never exposes a partial file. No locking or size check is needed. The README tells the maintainer to do this.

### The route sends one file, with attachment headers
`GET /download_latest` is registered outside `authenticate`, next to the health route. It responds with the chosen file as a `LocalFileContent` of type `application/vnd.android.package-archive` and `Content-Disposition: attachment; filename="<name>"`, or 404 with the project's usual error response when `latest()` is `null`. The route is exactly that path; there is no wildcard, so nothing under it is reachable and the folder cannot be listed.

### Health says whether there is an APK
`HealthResponse` gets `androidDownload: Boolean? = null` (nullable so older readers and the error answer need not set it). `HealthService.check()` sets it from `ApkDownloads.latest() != null` in the success answer, outside the database `try`, so a folder problem can only make it false and a database problem still gives the 503 with no flag. Older apps ignore the unknown key (`ignoreUnknownKeys` is on), and the Docker healthcheck only reads the status line. The folder is listed on every call (the container's check every 10 seconds, and each app start); a handful of directory entries costs nothing, so there is no cache to go stale.

Alternative: the web app tries `HEAD /download_latest` instead. Rejected: it needs the route to accept HEAD, and the follow-up update screen already needs `/health`.

### The configuration is one optional setting
`ServerConfig.downloadsDir` is read from `DOWNLOADS_DIR` (default `downloads`, blank means the default). It is not validated at start: the folder may not exist yet, and a server that refuses to start for want of an APK folder would be wrong. The compose files set it to `/downloads` and mount the host folder there read-only; the host folder is `./downloads`, or `DOWNLOADS_HOST_DIR` as a full path for Portainer, which runs from a folder of its own, the same way `SECRETS_DIR` works for the push key. `downloads/` is added to `.gitignore`.

### Web side: one holder, one platform flag
`Platform` gets `offersAndroidDownload`, true only in the `wasmJs` actual. `ApiClient` gets `health()`, an unauthenticated call returning `HealthResponse`. A small Koin-provided holder, `AndroidDownloadOffer`, exposes a `StateFlow<Boolean>` that is false until `/health` answers `androidDownload == true`; it fetches only when the platform flag is set, so the phones never make the call, and any failure leaves it false. The sign-in screen and the Settings page read it and show their entry when it is true; each triggers a refresh when it appears, so a newly uploaded APK shows up on the next visit.

The entry opens `baseUrl.trimEnd('/') + ApiRoutes.DOWNLOAD_LATEST` with the same `LocalUriHandler.openUri` the links use (`window.open` on the web). Because the response is an attachment, the browser downloads it instead of showing a page. The route constant lives in `core/contract` so the server and the app cannot drift apart.

## Risks / Trade-offs

- [Anyone with the address can fetch the APK] → It holds no secrets (the signing key stays on the maintainer's machine; the build embeds only public values such as the server and web addresses), testers already receive the same file, and the follow-up update screen needs a public address. No sign-in is added.
- [A large file through the public address] → How `santa-api.plm360.org` reaches the Pi (a proxy or tunnel) may limit size or time. The README says to test a download after the first upload. Bandwidth abuse is accepted for now; rate limits are out of scope.
- [No resumable downloads] → A phone on a weak connection restarts from zero. Ktor's `PartialContent` plugin would fix it for one line of wiring but widens what must be tested; left for a later change if it matters.
- [The listing is read on every `/health` call] → A few entries, no cache; if the folder were on a slow network mount it would be the first thing to cache.
- [`window.open` for a download on the web] → The browser may flash a blank tab or ask before downloading, and a popup blocker could stop it if the call were not from a click; it is made straight from the click, and this is checked by hand on the web target.
- [A bind mount of a missing host folder is created by Docker as root's] → The README says to create `downloads` first; it is mounted read-only, and a file copied in must be readable by others (mode 644) because the server runs as uid 10001.

## Migration Plan

1. Deploy the new server image and compose files; create `./downloads` on the Pi; nothing breaks if it stays empty (`/download_latest` is 404 and the web app shows no entry).
2. Build with `scripts/distribute-android.sh --no-upload`, copy the APK to `downloads/` under a `.part` name, rename it, then request `/download_latest` through the public address.
3. Ship the web app. Rollback is the previous images; the folder and its files are untouched either way.

# Proposal

## Why

Running the app today means installing the Kotlin toolchain, exporting a handful of environment variables by hand and starting the server and the web dev server in two terminals. There is no way to put it on a home server or a small VPS with one command, and the secrets (`JWT_SECRET`, `DATA_ENCRYPTION_KEY`, the first administrator's password) have no agreed place to live. A Docker Compose setup, fed by an uncommitted `.env` file, makes the whole app start with `docker compose up --build` and keeps the secrets out of the repository.

## What Changes

- **One command.** `docker compose up --build` builds the server and the web app inside Docker (no Kotlin, JDK or Node on the host) and starts both: the server (the JVM, with its SQLite database in a named volume) and the web app (static files behind nginx).
- **`.env` fills the compose file.** The compose file reads every setting from variables in a `.env` file next to it. `.env` is not committed (it already is git-ignored); a committed `.env.example` lists every variable with placeholders and how to generate the keys, and `.gitignore` gains an exception so the example is not hidden by the `.env.*` rule.
- **The web app finds the server.** When the web container starts it writes `config.json` from `PUBLIC_SERVER_URL`, so the one built image works for any address; the server is told `PUBLIC_WEB_ORIGIN` as its allowed CORS origin.
- **Data survives.** The database lives in a named volume, so rebuilding or recreating the containers keeps the users and groups.
- **Safer defaults.** A missing required variable stops compose with a message naming it; the server container does not run as root; the server has a healthcheck on `/health` and the web container waits for it.
- **README.** A "Run with Docker" section: first boot, the variables, backing up the volume and what is out of scope.

Out of scope:
- TLS and a public reverse proxy (browsers refuse an `https` page that calls an `http` API, so a public deployment puts TLS in front; this is said in the README, not built), a single-origin setup where nginx proxies the API, publishing images to a registry, building the Android and iOS apps, and a development compose file with hot reload.

## Capabilities

### New Capabilities
- `docker-deployment`: starting the server and the web app with Docker Compose from a `.env` file, and what persists and what is checked at start.

### Modified Capabilities
None. The server's own behavior (`server-runtime`) and the app's are unchanged.

## Impact

- Code: none in the app or the server. New files: `docker-compose.yml`, `.env.example`, `.dockerignore`, a `Dockerfile` for the server and one for the web app, a small nginx configuration and start script for the web image; `.gitignore` and README updated.
- APIs: none. Dependencies: none added to the app; the images use a JDK image to build, a JRE image and an nginx image to run.
- Platforms: the images are built for the machine they are built on (arm64 or amd64); Android and iOS are unaffected.

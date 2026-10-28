# Tasks

## 1. Images

- [x] 1.1 Find out what Amper produces in a container and write the server image: `.dockerignore`, `server/Dockerfile` (JDK build stage with cached dependency layers, JRE run stage, non-root user owning `/data`, `VOLUME /data`, `HEALTHCHECK` on `/health`); record in the design how the jar is packaged and which tool the healthcheck uses; verify the image builds from a clean cache and a container started by hand with the variables answers `/health`, runs as non-root, and exits naming the variable when `JWT_SECRET` is missing
- [x] 1.2 Write the web image: `app/webApp/Dockerfile` (JDK build stage producing the wasm app, nginx run stage), the nginx configuration (cache headers, wasm type, compression) and the `/docker-entrypoint.d` script that writes `config.json` from `PUBLIC_SERVER_URL` and refuses an empty or unsafe value; record in the design which build output is used; verify the image builds, the app loads in headless Chrome and `config.json` names the address given at start

## 2. Compose and settings

- [x] 2.1 Write `docker-compose.yml` (services `server` and `web`, named volume `data`, `environment:` with `${VAR:?...}` for the required variables and defaults for the ports, `depends_on` the server's health, `restart: unless-stopped`), `.env.example` with a placeholder for every variable and the key hint, and the `.gitignore` exception `!.env.example`; verify `git check-ignore` ignores `.env` and not `.env.example`, and `docker compose config` fails naming a missing required variable
- [x] 2.2 Verify the whole stack from a clean state with a test `.env`: `docker compose up --build`, `/health`, sign in as the first administrator from the web app in headless Chrome, a changed `PUBLIC_SERVER_URL` taking effect with a restart and no rebuild, removing and recreating the containers keeping the accounts, an invalid `DATA_ENCRYPTION_KEY` failing with its name in the log; note what could not be tried

## 3. Words

- [x] 3.1 Add a "Run with Docker" section to the README (first boot, the variables table, defaults, ports, the volume and how to back it up or import an existing database, TLS and the rate-limiter note, what is out of scope), and run the existing `./kotlin build` and `./kotlin test` to confirm nothing else changed

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.

# Design

## Context

The server is a JVM Ktor app configured only by environment variables (`JWT_SECRET`, `DATA_ENCRYPTION_KEY`, `DB_PATH`, `PORT`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`), with a `GET /health` and an SQLite file. The web app is a Compose Multiplatform wasm-js build whose `config.json` (`{"baseUrl": ...}`) is read at every start, so one build serves any server. The build tool is Amper (`./kotlin`), which downloads its own distribution and JDK, so the build stage needs network access. `.env` and `.env.*` are already git-ignored. Observed: a build of the server produces `build/tasks/_server_jarJvm/server-jvm.jar`, and a build of the web app produces a folder with `index.html`, `webApp.wasm`, `skiko.wasm`, `config.json` and the scripts (`build/tasks/_webApp_buildWasmJsAppWasmJsDebug`). Decided with the user: build inside Docker, two published ports (web and server, with CORS between them). See proposal.md and specs/.

## Goals / Non-Goals

**Goals:**
- `docker compose up --build` from a fresh checkout with only Docker installed and a `.env`, giving a working server and web app.
- Secrets and addresses only in `.env`; nothing real in the repository.

**Non-Goals:**
- TLS, a single-origin proxy, a registry, mobile builds, a hot-reload development setup.

## Decisions

### D1: Two services, two Dockerfiles, one context
```
 docker-compose.yml
   server : build server/Dockerfile      -> JRE image, :8080 inside
   web    : build app/webApp/Dockerfile  -> nginx image, :80 inside
   volume : data -> /data (SQLite)
 both Dockerfiles use the repository root as build context (the modules depend on each other)
 .dockerignore at the root: build output, .kotlin/, .git/, .idea/, openspec/, *.db, .env*
```
Each Dockerfile is a multi-stage build: a JDK stage runs Amper (`./kotlin ...`) and a slim stage receives only the result. Build caching: the Amper download and dependency resolution come before the source copy, so a source change does not download everything again.

### D2: What is built
- Server (found in task 1.1): `./kotlin package -m server` gives `build/tasks/_server_executableJarJvm/server-jvm-executable.jar`, a single runnable jar. Amper compiles for Java 25 by default (class file version 69), so the run stage is `eclipse-temurin:25-jre`; a Java 21 image fails with `UnsupportedClassVersionError`. The JRE image has no `curl` or `wget`, so the healthcheck uses bash's `/dev/tcp`.
- Web (found in task 1.1): `package` has nothing for wasm-js, but `./kotlin build -m webApp -p wasmJs -v release` writes a release folder, `build/tasks/_webApp_buildWasmJsAppWasmJsRelease` (29 MB against 41 MB for debug), which is copied into the nginx web root. In a container (task 1.2) the build stage needs `libatomic1`, because the `pnpm` binary the wasm build downloads links to it; nginx serves `.mjs` files as `application/octet-stream`, which browsers refuse for module scripts, so the nginx configuration gives them `text/javascript`. A clean web image build took about a minute here.
- The base images are multi-architecture (Eclipse Temurin and nginx), so the images are built for the machine's own architecture. A clean server image build took about 50 seconds here (arm64, 10 CPUs).
- `.dockerignore` keeps the `module.yaml` files of the Android and iOS modules, because `project.yaml` lists every module and Amper loads them all.

### D3: The variables
```
 .env                      reaches
 SERVER_PORT (8081)        published server port
 WEB_PORT (8080)           published web port
 PUBLIC_SERVER_URL         web: config.json baseUrl      (http://localhost:8081)
 PUBLIC_WEB_ORIGIN         server: CORS_ALLOWED_ORIGINS  (http://localhost:8080)
 JWT_SECRET *              server
 DATA_ENCRYPTION_KEY *     server
 ADMIN_USERNAME (admin)    server
 ADMIN_PASSWORD            server (only used on an empty database; the server itself refuses to start without it then)
 * required: ${VAR:?message} in the compose file
```
The compose file lists each variable under `environment:` instead of using `env_file:`, so the containers get exactly these and not the whole `.env`. The server's `PORT` and `DB_PATH` are fixed inside the container (`8080`, `/data/opensecretsanta.db`); only the published ports change. The defaults of the ports mirror the README's development setup. `ADMIN_PASSWORD` is not required by compose, because after the first start it can be removed from `.env`; on an empty volume the server refuses to start without it and says so. The public addresses default to `http://localhost:` plus the matching port. `.env.example` has a placeholder for each and the `openssl rand -base64 32` hint; `.gitignore` gets `!.env.example` after the `.env.*` rule.

### D4: The web container writes config.json
The nginx image runs executable scripts from `/docker-entrypoint.d` before it starts, so one script there writes `config.json` from `PUBLIC_SERVER_URL` and fails with a clear message when the variable is empty or contains a quote or backslash. nginx serves `config.json` and `index.html` with `Cache-Control: no-cache` (the app already adds a timestamp to its `config.json` request), the wasm files with their long-lived caching and the right `application/wasm` type, and compresses text, scripts and wasm. No fallback route is needed because the app's addresses are after the `#`.

### D5: Server container
The run stage creates a non-root user that owns `/data`, declares `VOLUME /data`, exposes 8080 and has a `HEALTHCHECK` that calls `/health` with bash's `/dev/tcp` and is healthy only on a 200 answer. The compose file mounts the named volume `data` at `/data`, restarts the service `unless-stopped`, and the web service has `depends_on: server: condition: service_healthy`.

### D6: Verification
By running it here (arm64): build both images from a clean cache, start with a test `.env`, `GET /health`, load the web app in headless Chrome and sign in as the first administrator, read `config.json` from the web port, change `PUBLIC_SERVER_URL` and restart without rebuilding, remove and recreate the containers and sign in again, start with `JWT_SECRET` missing and with an invalid `DATA_ENCRYPTION_KEY`, and check `docker compose exec server id`. `git check-ignore` shows `.env` ignored and `.env.example` tracked.

## Risks / Trade-offs

- [The Amper build in a container is unproven: it downloads a distribution and a JDK, may need more memory, and a wasm build may need extra tooling] -> Task 1.1 builds each image first and records what it needed; if the wasm build cannot run in a container the design is revisited before going on.
- [The first build is slow and large] -> Layer order for caching; `.dockerignore`; the README says the first build takes minutes.
- [An `https` page cannot call an `http` API, and the mobile apps only allow `http` for development hosts] -> The README says a public deployment needs TLS in front of both services and the two public addresses set to `https`; not built here.
- [The login rate limiter keys on the client address, which on Docker Desktop is the gateway address for everyone] -> Harmless for a small group; said in the README next to the existing note about proxies.
- [Losing `DATA_ENCRYPTION_KEY` or the volume] -> The README says to keep the key with the other secrets and how to copy the volume's database for a backup.

## Migration Plan

Nothing to migrate; running locally without Docker keeps working. An existing `opensecretsanta.db` can be copied into the volume, and the README says how. Rollback is deleting the new files.

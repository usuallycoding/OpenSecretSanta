## Context

Both Dockerfiles are multi-stage: a `eclipse-temurin:21-jdk` build stage runs the Kotlin toolchain (`./kotlin package -m server`, `./kotlin build -m webApp -p wasmJs -v release`) and a small final stage holds the result (`eclipse-temurin:25-jre` for the server, `nginx:1.29-alpine` for the web). `docker-compose.yml` only has `build:`. The repository is public on GitHub and `main` has been pushed. The Pi runs a 64-bit OS (`aarch64`).

## Goals / Non-Goals

**Goals:** images built in GitHub Actions for amd64 and arm64, pulled by the Pi; no secrets in images; one compose file for both uses.

**Non-Goals:** running tests in the workflow, image signing, auto-update on the Pi (a Watchtower-style updater), TLS, 32-bit Pi support.

## Decisions

- **Registry: GHCR.** It is free, needs no extra account and the workflow logs in with the built-in `GITHUB_TOKEN` (`packages: write`). New packages start private even for a public repository, so the first run is followed by making each package public once in GitHub's settings (the workflow cannot do that); then the Pi needs no login.
- **Cross-build the cheap way.** The build stage of each Dockerfile becomes `FROM --platform=$BUILDPLATFORM ...`, so it runs natively on the runner's amd64 and produces files that do not depend on the CPU: the server's executable jar (its SQLite driver bundles the native libraries of every platform, arm64 included, which was checked on the built jar) and the web app's static files. Only the final stages are made per target platform. QEMU is still set up because the server's final stage has a `RUN` (creating the unprivileged user), which executes as arm64; it is a few shell commands, not a build.
- **Workflow steps:** checkout, QEMU, Buildx, login to GHCR, `docker/metadata-action` for the tags, `docker/build-push-action` for each image with `platforms: linux/amd64,linux/arm64` and the GitHub Actions cache (the Kotlin toolchain download is repeated on a cache miss, which is acceptable). The two images are built as two jobs of a matrix. Pushes to branches other than `main` and pull requests do not run it.
- **Tags** come from the metadata action: `latest` and the short commit on `main`, `X.Y.Z` and `X.Y` on `vX.Y.Z`. `latest` is set only for `main`.
- **Compose:** `image: ghcr.io/usuallycoding/opensecretsanta-server:${IMAGE_TAG:-latest}` (and `-web`) beside each `build:`. With both keys, `docker compose pull` pulls and `docker compose up --build` builds and tags locally under the same name. `IMAGE_TAG` is listed in `.env.example`.
- **The Pi only needs three things:** the compose file, `.env` and `secrets/`. The README says so, and how to update (`docker compose pull && docker compose up -d`) and roll back (`IMAGE_TAG=<older>`).
- **Check locally what can be checked:** this machine is arm64 like the Pi, so the arm64 images can be built here and the server image run and its `/health` called; the amd64 build and the push itself can only be seen on GitHub.

## Risks / Trade-offs

- The workflow cannot be run from here: its first run on GitHub is the real test, and may need a small fix.
- A build for a platform other than the runner's relies on the final stages being platform-neutral apart from the base image, which they are.
- Anyone can pull the public images; they hold no secrets, but they are the app as built from `main`.
- The Firebase web key in the web image's default `config.json` is public by design and is replaced at start.

## Open Questions

None.

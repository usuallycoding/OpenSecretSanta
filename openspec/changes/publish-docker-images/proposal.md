## Why

The Raspberry Pi that runs the app should not need the images carried over on a USB stick, nor the Kotlin toolchain to build them. GitHub Actions can build the server and web images on every push to `main`, publish them to GitHub's container registry for both the Pi's `linux/arm64` and ordinary `linux/amd64`, and the Pi then only has to `docker compose pull` and `up -d`.

## What Changes

- A GitHub Actions workflow builds the server and web images for `linux/amd64` and `linux/arm64` and pushes them to GHCR (`ghcr.io/usuallycoding/opensecretsanta-server` and `-web`): on every push to `main` (tags `latest` and the commit), on a version tag `vX.Y.Z` (tags `X.Y.Z` and `X.Y`), and on demand.
- The Dockerfiles run their heavy Kotlin build stage on the runner's own architecture and only the small final stage (JRE, nginx) per target architecture, so no emulated Kotlin build is needed. The jars and the static web files do not depend on the CPU.
- `docker-compose.yml` names the published image next to each `build:`, so one file serves both: `docker compose pull && docker compose up -d` on the Pi, `docker compose up --build` while developing. `IMAGE_TAG` in `.env` picks another tag, for a rollback.
- The README gets a section for the Pi: what to copy to it (`docker-compose.yml`, `.env`, `secrets/`), the two commands, how to update and roll back, and that each package is made public once in GitHub's package settings after the first run.
- The images keep holding no secrets and the web image keeps reading its settings at start, so the same published image works at any address.

## Capabilities

### New Capabilities

- `docker-image-publishing`: what the workflow builds, for which platforms, under which names and tags, and when it runs.

### Modified Capabilities

- `docker-deployment`: the compose file can run the published images without building.

## Impact

- New: `.github/workflows/docker-images.yml`. Changed: `server/Dockerfile` and `app/webApp/Dockerfile` (the build stage's platform), `docker-compose.yml` (an `image:` per service), `.env.example` (`IMAGE_TAG`), `README.md`.
- No application code change. Needs the repository on GitHub (it is, public) and, once, the packages made public in GitHub's settings, which the workflow cannot do.
- Not in scope: running the tests in the workflow, signing images, automatic updating on the Pi, TLS.

# Spec Delta

## Purpose
How the server and web Docker images are built and published by GitHub Actions, for which platforms, under which names and tags and when, so that a machine such as a Raspberry Pi can pull them instead of building them.

## ADDED Requirements

### Requirement: Images are built and published by a workflow
A GitHub Actions workflow SHALL build the server image from `server/Dockerfile` and the web image from `app/webApp/Dockerfile` and push them to GitHub's container registry as `ghcr.io/usuallycoding/opensecretsanta-server` and `ghcr.io/usuallycoding/opensecretsanta-web`. It SHALL run on every push to `main`, on every tag `vX.Y.Z` and when started by hand, and SHALL use only the repository's own token, with write access to packages and read access to the code.

#### Scenario: Push to main
- **WHEN** a commit is pushed to `main`
- **THEN** both images are built and pushed

#### Scenario: Started by hand
- **WHEN** the workflow is started from GitHub's Actions page
- **THEN** both images are built and pushed

#### Scenario: Failed build
- **WHEN** one of the builds fails
- **THEN** nothing of that run is tagged `latest`

### Requirement: Images run on the Raspberry Pi and on ordinary machines
Each image SHALL be published for `linux/amd64` and `linux/arm64` under one tag, so that Docker on either picks its own. The Kotlin build stage of each Dockerfile SHALL run on the architecture of the machine that builds, and only the final stage SHALL be built for the target architecture, so that no emulated Kotlin build is needed.

#### Scenario: Pull on a Raspberry Pi
- **WHEN** a 64-bit Raspberry Pi runs `docker compose pull`
- **THEN** it receives the `linux/arm64` images and the server starts and answers `GET /health` with 200

#### Scenario: Pull on an x86 machine
- **WHEN** an x86 machine pulls the same tag
- **THEN** it receives the `linux/amd64` images

#### Scenario: SQLite on arm64
- **WHEN** the arm64 server image opens its database
- **THEN** the SQLite driver's arm64 native library is used and the server starts

### Requirement: Tags
A push to `main` SHALL tag the images `latest` and with the commit it was built from. A tag `vX.Y.Z` SHALL tag them `X.Y.Z` and `X.Y`. A run that was not started by a push to `main` or by a version tag SHALL NOT move `latest`.

#### Scenario: Main
- **WHEN** commit `abc1234` is pushed to `main`
- **THEN** the images carry the tags `latest` and one naming `abc1234`

#### Scenario: Version tag
- **WHEN** the tag `v1.4.2` is pushed
- **THEN** the images carry the tags `1.4.2` and `1.4`

### Requirement: Images hold no secrets
The images SHALL contain no `.env`, no Firebase service-account key and no database, as the build already excludes them, and the published web image SHALL read its server address and public settings when it starts, so that one image works at any address.

#### Scenario: Nothing private inside
- **WHEN** a published image is inspected
- **THEN** it has no `.env`, no `secrets` folder and no `.db` file

#### Scenario: Any address
- **WHEN** the web image is started with another `PUBLIC_SERVER_URL`
- **THEN** its `config.json` names that address without a rebuild

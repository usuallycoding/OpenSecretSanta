# Spec Delta

## ADDED Requirements

### Requirement: Compose can run the published images
`docker-compose.yml` SHALL name the published image of each service, tagged `latest` unless `IMAGE_TAG` in `.env` says otherwise, next to its `build:`. `docker compose pull` followed by `docker compose up -d` SHALL run the stack from the published images with no build and no toolchain on the machine, and `docker compose up --build` SHALL still build from the checkout.

#### Scenario: Run on a Pi
- **WHEN** the Pi has the compose file, a filled-in `.env` and the `secrets` folder, and runs `docker compose pull` and `docker compose up -d`
- **THEN** the stack starts without building anything

#### Scenario: Update
- **WHEN** a newer image is published and the Pi runs the two commands again
- **THEN** the services restart on the new images and the data volume is kept

#### Scenario: Roll back
- **WHEN** `IMAGE_TAG` in `.env` names an earlier version and the Pi runs the two commands
- **THEN** the stack runs on that version

#### Scenario: Build from the checkout
- **WHEN** someone runs `docker compose up --build` in a checkout
- **THEN** the images are built from the source as before

### Requirement: The README describes running on a Raspberry Pi
The README SHALL describe running the stack on a Raspberry Pi from the published images: the files to put there (the compose file, `.env` and the `secrets` folder), the two commands, updating, rolling back with `IMAGE_TAG`, and making the two packages public once in GitHub's settings.

#### Scenario: Following the README
- **WHEN** someone follows the Raspberry Pi section on a 64-bit Pi
- **THEN** they end with the stack running and know how to update and roll back

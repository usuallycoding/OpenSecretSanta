# Spec Delta

## ADDED Requirements

### Requirement: Compose can run the published images
`docker-compose.yml` SHALL name the published image of each service, tagged `latest` unless `IMAGE_TAG` in `.env` says otherwise, next to its `build:`, so that `docker compose up --build` still builds from the checkout. A second file, `docker-compose.images.yml`, SHALL define the same services with only the published images and no `build:`, so that `docker compose -f docker-compose.images.yml pull` and `up -d` run the stack with no build, no toolchain and no repository on the machine.

#### Scenario: Run on a Pi
- **WHEN** the Pi has `docker-compose.images.yml`, a filled-in `.env` and the `secrets` folder, and runs `docker compose -f docker-compose.images.yml pull` and `up -d`
- **THEN** the stack starts without building anything

#### Scenario: Update
- **WHEN** a newer image is published and the Pi runs the two commands again
- **THEN** the services restart on the new images and the data volume is kept

#### Scenario: Roll back
- **WHEN** `IMAGE_TAG` in `.env` names an earlier version and the Pi runs the two commands
- **THEN** the stack runs on that version

#### Scenario: Portainer
- **WHEN** the contents of `docker-compose.images.yml` are deployed as a Portainer stack that has no repository files
- **THEN** the stack starts from the published images and nothing is built

#### Scenario: Push key folder
- **WHEN** `SECRETS_DIR` names a folder that holds the key file
- **THEN** the server container can read the key from it, and without `SECRETS_DIR` the folder `./secrets` next to the compose file is used

#### Scenario: Build from the checkout
- **WHEN** someone runs `docker compose up --build` in a checkout
- **THEN** the images are built from the source as before

### Requirement: The README describes running on a Raspberry Pi
The README SHALL describe running the stack on a Raspberry Pi from the published images: the files to put there (`docker-compose.images.yml`, `.env` and the `secrets` folder), the two commands, updating, rolling back with `IMAGE_TAG`, and making the two packages public once in GitHub's settings.

#### Scenario: Following the README
- **WHEN** someone follows the Raspberry Pi section on a 64-bit Pi
- **THEN** they end with the stack running and know how to update and roll back

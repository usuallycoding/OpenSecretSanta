# docker-deployment Specification

## Purpose
How the server and the web app are built and run with one Docker Compose command: settings from an uncommitted .env, the web app pointed at the server at start, persistent data and a checked, unprivileged server container.

## Requirements

### Requirement: One command runs server and web
The repository SHALL contain a Docker Compose setup that, with `docker compose up --build` and no toolchain installed on the host other than Docker, builds the server and the web app and runs them as two services: the server and the web app served as static files.

#### Scenario: First start
- **WHEN** someone with Docker and a filled-in `.env` runs `docker compose up --build` in a fresh checkout
- **THEN** the server answers `GET /health` with 200 on its published port and the web app loads on its published port

#### Scenario: Web waits for the server
- **WHEN** the stack starts
- **THEN** the web service is started only once the server's healthcheck passes

### Requirement: Settings come from an uncommitted .env
Every setting of the compose file (the ports, the two public addresses, `JWT_SECRET`, `DATA_ENCRYPTION_KEY`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` and the optional `RESEND_API_KEY`, `EMAIL_FROM` and `APP_URL`) SHALL be read from variables in a `.env` file, which SHALL NOT be committed. A committed `.env.example` SHALL list every variable with a placeholder and no real secret. Only the variables the services need SHALL reach the containers.

#### Scenario: .env is not committed
- **WHEN** a `.env` file exists in the checkout
- **THEN** git ignores it, while `.env.example` is tracked

#### Scenario: Missing secret
- **WHEN** a required variable such as `JWT_SECRET` is not set in `.env`
- **THEN** compose refuses to start and the message names that variable

#### Scenario: Defaults
- **WHEN** the ports are not set in `.env`
- **THEN** documented default ports are used

#### Scenario: Email is optional
- **WHEN** the email variables are not set in `.env`
- **THEN** compose starts as before and the server runs with email off

#### Scenario: Email variables reach only the server
- **WHEN** the email variables are set
- **THEN** the server container receives them and the web container does not

### Requirement: The web app is pointed at the server when it starts
The web container SHALL write the web app's `config.json` from `PUBLIC_SERVER_URL` each time it starts, and the server SHALL be given `PUBLIC_WEB_ORIGIN` as its only allowed web origin, so that the same built image works for any address without a rebuild.

#### Scenario: Another server address
- **WHEN** `PUBLIC_SERVER_URL` is changed in `.env` and the stack is started again without rebuilding
- **THEN** `config.json` served by the web container names the new address

#### Scenario: Sign in from the browser
- **WHEN** a person opens the web app on its published address and signs in as the first administrator
- **THEN** the sign-in succeeds, with the server allowing the request from the web origin

### Requirement: Data survives containers
The server's SQLite database SHALL live in a named Docker volume, so that stopping, removing or rebuilding the containers keeps the users, groups and wishlists.

#### Scenario: Recreate
- **WHEN** the containers are removed and created again with the same volume
- **THEN** the accounts created earlier still sign in, and `ADMIN_PASSWORD` is not needed again

### Requirement: The server container is checked and unprivileged
The server container SHALL run as a user other than root and SHALL have a healthcheck that succeeds only while `GET /health` answers 200.

#### Scenario: Not root
- **WHEN** the server container is running
- **THEN** its process does not run as root and can write to the data volume

#### Scenario: Unhealthy
- **WHEN** the server cannot start, for example because `DATA_ENCRYPTION_KEY` is not valid base64
- **THEN** the container exits or reports unhealthy and its log names the offending variable

### Requirement: Push settings are optional and stay out of git
The compose setup SHALL pass the optional Firebase settings from `.env`: the path of the service-account key file to the server, which SHALL receive the file read-only, and the public Firebase web settings to the web container only. The key file SHALL live in a folder git ignores, SHALL NOT be copied into any image and SHALL NOT be served by the web container. `.env.example` SHALL list the variables with placeholders. Without them compose SHALL start as before with push off.

#### Scenario: Push off
- **WHEN** the Firebase variables are not set in `.env`
- **THEN** compose starts, the server runs with push off and the web app works

#### Scenario: Key reaches only the server
- **WHEN** the key file is configured
- **THEN** the server container can read it and the web container and the web app's files do not contain it

#### Scenario: Key not committed
- **WHEN** the key file sits in the ignored folder
- **THEN** git ignores it and `.env.example` is tracked

#### Scenario: Another address
- **WHEN** the web address in `.env` is changed and the stack is started again without rebuilding
- **THEN** the web app's served `config.json` carries the public Firebase settings and the new server address

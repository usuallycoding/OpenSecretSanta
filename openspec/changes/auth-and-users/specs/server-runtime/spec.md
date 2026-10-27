# Spec Delta

## MODIFIED Requirements

### Requirement: Environment configuration
The server SHALL read its configuration from environment variables: `JWT_SECRET` (required), `DATA_ENCRYPTION_KEY` (required, a base64-encoded 32-byte key), `DB_PATH` (default `opensecretsanta.db`), `PORT` (default `8080`), `ADMIN_USERNAME` (default `admin`) and `ADMIN_PASSWORD` (required only when the database has no users).

#### Scenario: Defaults applied
- **WHEN** the server starts with only `JWT_SECRET` and `DATA_ENCRYPTION_KEY` set, against a database that already has users
- **THEN** it listens on port 8080 and uses `opensecretsanta.db` as its database file

#### Scenario: Overrides honored
- **WHEN** the server starts with `PORT=9000` and `DB_PATH=/data/santa.db`
- **THEN** it listens on port 9000 and uses `/data/santa.db`

#### Scenario: Admin seed variables
- **WHEN** the server starts against an empty database with `ADMIN_PASSWORD` set and `ADMIN_USERNAME` unset
- **THEN** the default administrator is created with the username `admin`

### Requirement: Fail fast on invalid configuration
The server SHALL refuse to start, with an error naming the offending variable, when a required variable is missing or blank, when `PORT` is not an integer between 1 and 65535, when `DATA_ENCRYPTION_KEY` is not valid base64 for exactly 32 bytes, when `ADMIN_USERNAME` is not a valid username, or when the database has no users and `ADMIN_PASSWORD` is missing or blank.

#### Scenario: Missing secret
- **WHEN** the server starts without `JWT_SECRET`
- **THEN** startup fails before any port is bound and the error message names `JWT_SECRET`

#### Scenario: Invalid port
- **WHEN** the server starts with `PORT=abc`
- **THEN** startup fails and the error message names `PORT`

#### Scenario: Missing or malformed encryption key
- **WHEN** the server starts without `DATA_ENCRYPTION_KEY`, or with a value that does not decode to 32 bytes
- **THEN** startup fails and the error message names `DATA_ENCRYPTION_KEY`

#### Scenario: Empty database without admin password
- **WHEN** the server starts against an empty database without `ADMIN_PASSWORD`
- **THEN** startup fails and the error message names `ADMIN_PASSWORD`

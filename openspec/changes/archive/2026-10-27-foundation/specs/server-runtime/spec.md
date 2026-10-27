# Spec Delta

## Purpose

Defines how the Open Secret Santa server starts: how it is configured from the environment, how its SQLite database schema is brought up to date at boot, and how operators can check that it is alive.

## ADDED Requirements

### Requirement: Environment configuration
The server SHALL read its configuration from environment variables: `JWT_SECRET` (required), `DB_PATH` (default `opensecretsanta.db`) and `PORT` (default `8080`).

#### Scenario: Defaults applied
- **WHEN** the server starts with only `JWT_SECRET` set
- **THEN** it listens on port 8080 and uses `opensecretsanta.db` as its database file

#### Scenario: Overrides honored
- **WHEN** the server starts with `PORT=9000` and `DB_PATH=/data/santa.db`
- **THEN** it listens on port 9000 and uses `/data/santa.db`

### Requirement: Fail fast on invalid configuration
The server SHALL refuse to start, with an error naming the offending variable, when a required variable is missing or blank or when `PORT` is not an integer between 1 and 65535.

#### Scenario: Missing secret
- **WHEN** the server starts without `JWT_SECRET`
- **THEN** startup fails before any port is bound and the error message names `JWT_SECRET`

#### Scenario: Invalid port
- **WHEN** the server starts with `PORT=abc`
- **THEN** startup fails and the error message names `PORT`

### Requirement: Versioned database migrations
The server SHALL apply all pending numbered schema migrations to the SQLite database in ascending order during startup, and SHALL record the highest applied version so that each migration runs at most once.

#### Scenario: Fresh database
- **WHEN** the server starts and the database file does not exist
- **THEN** the file is created and every migration is applied in order

#### Scenario: Already up to date
- **WHEN** the server restarts and every migration has already been applied
- **THEN** no migration is executed again and existing data is unchanged

#### Scenario: Failing migration
- **WHEN** a migration fails while applying
- **THEN** that migration's changes are rolled back, its version is not recorded, and the server does not start

### Requirement: Health endpoint
The server SHALL expose `GET /health`, requiring no authentication, that returns HTTP 200 with a JSON body containing a `status` of `"ok"` and the current database `schemaVersion`.

#### Scenario: Healthy server
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response is 200 with `status` equal to `"ok"` and `schemaVersion` equal to the highest applied migration version

#### Scenario: Database unavailable
- **WHEN** a client requests `GET /health` and the database cannot be queried
- **THEN** the response is 503 with `status` equal to `"error"`

### Requirement: Consistent error responses
The server SHALL return unhandled failures as HTTP 500 with a JSON error body that does not expose stack traces or internal messages.

#### Scenario: Unexpected failure
- **WHEN** a request handler throws an unexpected exception
- **THEN** the response is 500 with a JSON body containing a generic error message and no stack trace

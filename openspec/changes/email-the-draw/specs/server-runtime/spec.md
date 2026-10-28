## MODIFIED Requirements

### Requirement: Environment configuration
The server SHALL read its configuration from environment variables: `JWT_SECRET` (required), `DATA_ENCRYPTION_KEY` (required, a base64-encoded 32-byte key), `DB_PATH` (default `opensecretsanta.db`), `PORT` (default `8080`), `ADMIN_USERNAME` (default `admin`) and `ADMIN_PASSWORD` (required only when the database has no users). It SHALL also read the optional email settings `RESEND_API_KEY`, `EMAIL_FROM` and `APP_URL`; email is on only when `RESEND_API_KEY` and `EMAIL_FROM` are both set.

#### Scenario: Defaults applied
- **WHEN** the server starts with only `JWT_SECRET` and `DATA_ENCRYPTION_KEY` set, against a database that already has users
- **THEN** it listens on port 8080 and uses `opensecretsanta.db` as its database file

#### Scenario: Overrides honored
- **WHEN** the server starts with `PORT=9000` and `DB_PATH=/data/santa.db`
- **THEN** it listens on port 9000 and uses `/data/santa.db`

#### Scenario: Admin seed variables
- **WHEN** the server starts against an empty database with `ADMIN_PASSWORD` set and `ADMIN_USERNAME` unset
- **THEN** the default administrator is created with the username `admin`

#### Scenario: Email settings
- **WHEN** the server starts with `RESEND_API_KEY` and `EMAIL_FROM` set
- **THEN** draw emails are enabled, and without them the server runs with email off

### Requirement: Fail fast on invalid configuration
The server SHALL refuse to start, with an error naming the offending variable, when a required variable is missing or blank, when `PORT` is not an integer between 1 and 65535, when `DATA_ENCRYPTION_KEY` is not valid base64 for exactly 32 bytes, when `ADMIN_USERNAME` is not a valid username, when the database has no users and `ADMIN_PASSWORD` is missing or blank, when `RESEND_API_KEY` is set without `EMAIL_FROM`, when `EMAIL_FROM` is not an email address (optionally with a display name), or when `APP_URL` is not an http or https address.

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

#### Scenario: Key without a sender
- **WHEN** the server starts with `RESEND_API_KEY` set and `EMAIL_FROM` missing or not an email address
- **THEN** startup fails and the error message names `EMAIL_FROM`, without printing the key

#### Scenario: Bad app address
- **WHEN** the server starts with `APP_URL=not a url`
- **THEN** startup fails and the error message names `APP_URL`

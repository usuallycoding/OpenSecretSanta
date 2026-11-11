# server-runtime Specification

## Purpose

Defines how the Open Secret Santa server starts: how it is configured from the environment, how its SQLite database schema is brought up to date at boot, and how operators can check that it is alive.

## Requirements

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
The server SHALL refuse to start, with an error naming the offending variable, when a required variable is missing or blank, when `PORT` is not an integer between 1 and 65535, when `DATA_ENCRYPTION_KEY` is not valid base64 for exactly 32 bytes, or when `ADMIN_USERNAME` is not a valid username.

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
The server SHALL expose `GET /health`, requiring no authentication, that returns HTTP 200 with a JSON body containing a `status` of `"ok"`, the current database `schemaVersion`, the API's `apiVersion` and `androidDownload`, which is true when an APK is available at `/download_latest` and false otherwise. A downloads folder that is missing or cannot be read SHALL only make `androidDownload` false and SHALL NOT change the status of the answer.

#### Scenario: Healthy server
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response is 200 with `status` equal to `"ok"` and `schemaVersion` equal to the highest applied migration version

#### Scenario: The API's version
- **WHEN** a client requests `GET /health` on a started server
- **THEN** the response carries `apiVersion` equal to the version the server's API has

#### Scenario: An APK is available
- **WHEN** the downloads folder holds an APK and a client requests `GET /health`
- **THEN** the response is 200 and `androidDownload` is true

#### Scenario: No APK
- **WHEN** the downloads folder is empty and a client requests `GET /health`
- **THEN** the response is 200 and `androidDownload` is false

#### Scenario: Unreadable folder
- **WHEN** the downloads folder does not exist and a client requests `GET /health`
- **THEN** the response is 200 with `status` equal to `"ok"` and `androidDownload` equal to false

#### Scenario: Database unavailable
- **WHEN** a client requests `GET /health` and the database cannot be queried
- **THEN** the response is 503 with `status` equal to `"error"` and no `apiVersion`

### Requirement: Consistent error responses
The server SHALL return unhandled failures as HTTP 500 with a JSON error body that does not expose stack traces or internal messages.

#### Scenario: Unexpected failure
- **WHEN** a request handler throws an unexpected exception
- **THEN** the response is 500 with a JSON body containing a generic error message and no stack trace

### Requirement: Cross-origin access for browser clients
The server SHALL accept cross-origin requests from a browser only from the origins listed in the optional `CORS_ALLOWED_ORIGINS` variable, a comma-separated list of origins such as `https://app.example.org`. For a listed origin the server SHALL answer preflight requests and allow the `Authorization` and `Content-Type` request headers and the methods the API uses. When the variable is unset or blank the server SHALL add no cross-origin headers at all, as before. A `*` entry SHALL be refused.

#### Scenario: Listed origin
- **WHEN** a browser at `https://app.example.org`, which is listed, sends a preflight request for `PUT /me/password` with an `Authorization` header
- **THEN** the response allows that origin, method and header

#### Scenario: Unlisted origin
- **WHEN** a request carries an `Origin` that is not listed
- **THEN** the response has no cross-origin allow headers

#### Scenario: Default
- **WHEN** the server starts without `CORS_ALLOWED_ORIGINS`
- **THEN** no response carries cross-origin allow headers and the other behavior is unchanged

#### Scenario: Wildcard refused
- **WHEN** the server starts with `CORS_ALLOWED_ORIGINS=*` or an entry that is not an origin (no scheme and host, or with a path)
- **THEN** startup fails and the error message names `CORS_ALLOWED_ORIGINS`

#### Scenario: Authentication unaffected
- **WHEN** a request from a listed origin has no valid token
- **THEN** it is still answered 401, and a preflight request needs no token

### Requirement: Fail fast on invalid admin and email configuration
The server SHALL also refuse to start, with an error naming the offending variable, when the database has no users and `ADMIN_PASSWORD` is missing or blank, when `RESEND_API_KEY` is set without `EMAIL_FROM`, when `EMAIL_FROM` is not an email address (optionally with a display name), or when `APP_URL` is not an http or https address.

#### Scenario: Bad sender
- **WHEN** `RESEND_API_KEY` is set and `EMAIL_FROM` is `not an address`
- **THEN** startup fails and the error names `EMAIL_FROM`

### Requirement: The downloads folder is configurable
The server SHALL read `DOWNLOADS_DIR`, the folder `/download_latest` takes the APK from, defaulting to `downloads` when it is not set or is blank. The folder SHALL NOT have to exist when the server starts.

#### Scenario: Default
- **WHEN** the server starts without `DOWNLOADS_DIR`
- **THEN** it looks for APKs in `downloads`

#### Scenario: Set
- **WHEN** the server starts with `DOWNLOADS_DIR=/downloads`
- **THEN** it looks for APKs in `/downloads`

#### Scenario: Not there yet
- **WHEN** the server starts and the folder does not exist
- **THEN** it starts normally and `/download_latest` answers 404

### Requirement: The API has a version
The API SHALL have one version number, a whole number starting at 1 that the server and the apps are built with, and the server SHALL report it as `apiVersion` in `GET /health`. It SHALL be raised only for a change that an app built with the earlier number would misbehave against, and SHALL NOT be raised for a change such an app still copes with, such as a new optional field or a new route.

#### Scenario: A breaking change
- **WHEN** the API changes so that an app built before it would fail or show wrong data
- **THEN** the version is raised by one, and the server reports the new number

#### Scenario: A compatible change
- **WHEN** a field is added that an older app ignores
- **THEN** the version stays as it is

#### Scenario: Same build, same number
- **WHEN** the server and an app are built from the same revision
- **THEN** the number the server reports is the number the app was built with

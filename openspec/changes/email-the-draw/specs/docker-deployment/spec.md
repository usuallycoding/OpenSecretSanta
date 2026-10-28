## MODIFIED Requirements

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

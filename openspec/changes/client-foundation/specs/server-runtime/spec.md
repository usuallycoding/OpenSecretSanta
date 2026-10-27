# Spec Delta

## ADDED Requirements

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

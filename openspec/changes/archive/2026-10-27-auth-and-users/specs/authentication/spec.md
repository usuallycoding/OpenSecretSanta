# Spec Delta

## Purpose

Defines how users prove who they are to the Open Secret Santa server, how that proof is checked on every request, and how the server defends login against guessing.

## ADDED Requirements

### Requirement: Login with username and password
The server SHALL expose `POST /auth/login` that accepts a username and password and, on success, returns an access token together with whether the user must change their password. Usernames SHALL be matched case-insensitively.

#### Scenario: Successful login
- **WHEN** a client posts the correct username and password of an existing user
- **THEN** the response is 200 with an access token and `mustChangePassword` reflecting the user's state

#### Scenario: Username case ignored
- **WHEN** a client logs in as `Alice` and the stored username is `alice`
- **THEN** the login succeeds

#### Scenario: Wrong password or unknown user
- **WHEN** a client posts a wrong password, or a username that does not exist
- **THEN** the response is 401 with the same error code and message in both cases

### Requirement: Access token validity
The server SHALL issue signed access tokens valid for 7 days and SHALL reject a token that is missing, malformed, expired, wrongly signed, or issued before the user's latest credential change, or whose user no longer exists.

#### Scenario: Valid token accepted
- **WHEN** a client calls a protected endpoint with a token from a recent login
- **THEN** the request is authenticated as that user

#### Scenario: Missing or invalid token
- **WHEN** a client calls a protected endpoint with no token, a tampered token, or an expired token
- **THEN** the response is 401

#### Scenario: Token invalidated by credential change
- **WHEN** a user's password is changed or reset after a token was issued
- **THEN** that earlier token is rejected with 401

### Requirement: Current user state on every request
The server SHALL determine a caller's admin status and password-change requirement from the stored user on each authenticated request, not from claims in the token.

#### Scenario: Promotion takes effect immediately
- **WHEN** a user holding an older token is promoted to admin
- **THEN** their next request to an admin endpoint is authorized without logging in again

### Requirement: Forced password change gate
While a user's password is temporary, the server SHALL reject every authenticated request except reading their own account and changing their password, with HTTP 403 and the code `password_change_required`.

#### Scenario: Blocked until password changed
- **WHEN** a user who must change their password calls any other protected endpoint
- **THEN** the response is 403 with code `password_change_required`

#### Scenario: Allowed endpoints
- **WHEN** that user reads their own account or changes their password
- **THEN** the request is processed normally

#### Scenario: Gate lifted
- **WHEN** the user has changed their password
- **THEN** other protected endpoints are no longer blocked by this rule

### Requirement: Account lockout after repeated failures
The server SHALL lock logins for a username for 15 minutes after 5 consecutive failed attempts, rejecting further attempts, even with the correct password, with HTTP 429 and a `Retry-After` header, and SHALL treat unknown usernames identically.

#### Scenario: Lock after five failures
- **WHEN** five consecutive wrong-password attempts are made for a username
- **THEN** a sixth attempt with the correct password is rejected with 429 and a `Retry-After` header

#### Scenario: Unknown usernames locked the same way
- **WHEN** five consecutive attempts are made for a username that does not exist
- **THEN** the sixth attempt gets the same 429 response as for an existing username

#### Scenario: Lock expires
- **WHEN** 15 minutes have passed since the lock began
- **THEN** a login with the correct password succeeds

#### Scenario: Counter reset by success
- **WHEN** a successful login occurs after fewer than five failures
- **THEN** the failure count restarts from zero

#### Scenario: Reset clears the lock
- **WHEN** an admin resets a locked user's password
- **THEN** the lock is cleared immediately

### Requirement: Login rate limit per client address
The server SHALL limit `POST /auth/login` to 20 requests per minute per client address and reject excess requests with HTTP 429 and a `Retry-After` header.

#### Scenario: Excess requests rejected
- **WHEN** one client address sends a 21st login request within a minute
- **THEN** that request is rejected with 429 and a `Retry-After` header

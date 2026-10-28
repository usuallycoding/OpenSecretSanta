# Spec Delta

## REMOVED Requirements

### Requirement: Current user state on every request
**Reason**: Its only scenario, "Promotion takes effect immediately", exists because of the promote route, which is removed. The rule itself stays, under the name "Stored user state decides each request" below.
**Migration**: None.

## ADDED Requirements

### Requirement: Stored user state decides each request
The server SHALL determine a caller's admin status and password-change requirement from the stored user on each authenticated request, not from claims in the token.

#### Scenario: A password reset applies to an older token
- **WHEN** an admin resets a user's password after that user's token was issued
- **THEN** the user's next request with that token is rejected and a new sign-in with the new temporary password requires a password change

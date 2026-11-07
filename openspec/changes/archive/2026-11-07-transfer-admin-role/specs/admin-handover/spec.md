## Purpose

Defines how the administrator role is handed over to another user: who may do it and to whom, what the server checks first, what moves with the role, and the guarantee that there is never more or fewer administrators than before.

## ADDED Requirements

### Requirement: An administrator hands the role over
The server SHALL expose `POST /admin/users/{username}/hand-over-admin` with the administrator's own password in the body, usable only by an administrator. On success the named user SHALL be an administrator and the caller SHALL no longer be one, and the response SHALL be 204 with no body. No other route SHALL grant the administrator role.

#### Scenario: Handing over
- **WHEN** administrator `boss` hands the role over to `alice`, whom `boss` manages and who has set her own password
- **THEN** the response is 204, `alice` is an administrator and `boss` is not

#### Scenario: Not an administrator
- **WHEN** a user who is not an administrator calls the route
- **THEN** the response is 403 and nothing changes

#### Scenario: Always one administrator
- **WHEN** the role has been handed over
- **THEN** the number of administrators is what it was before the handover

### Requirement: Only a managed person can take over
The target of a handover SHALL be a user the caller manages (see "Managed-user scope") and who is not the caller. A user the caller does not manage SHALL be answered as if they did not exist. Handing over to oneself, or to someone who is already an administrator, SHALL be refused with 409 and code `invalid_state`.

#### Scenario: Not managed
- **WHEN** the caller hands over to `bob`, who is in no group the caller owns and was not created by the caller
- **THEN** the response is 404 and nothing changes

#### Scenario: Oneself
- **WHEN** the caller names their own username
- **THEN** the response is 409 with code `invalid_state` and the caller is still the administrator

#### Scenario: Already an administrator
- **WHEN** the target is already an administrator
- **THEN** the response is 409 with code `invalid_state` and nothing changes

### Requirement: Someone on a temporary password cannot take over
A handover SHALL be refused with 409 and code `invalid_state` when the target still has a temporary password, because the caller can read that password and would keep access to the account that now holds the role. The check SHALL be made again inside the handover, so a password reset between looking and handing over cannot slip through.

#### Scenario: Temporary password
- **WHEN** the target has not yet changed the password the administrator gave them
- **THEN** the response is 409 with code `invalid_state` and nothing changes

#### Scenario: After the person changed it
- **WHEN** the target has changed their password and the caller hands over
- **THEN** the handover is made

### Requirement: The caller proves who they are
The body SHALL hold the caller's current password, and the handover SHALL be refused with 400 naming `password` when it is missing or wrong, changing nothing. Failed attempts SHALL count towards the same limit as failed sign-ins, answered with 429 and code `too_many_attempts` and a `Retry-After` header once it is reached.

#### Scenario: Wrong password
- **WHEN** the caller sends the wrong password
- **THEN** the response is 400 naming `password` and the role has not moved

#### Scenario: Too many attempts
- **WHEN** the caller has failed more times than sign-in allows
- **THEN** the response is 429 with code `too_many_attempts` and a `Retry-After` header

#### Scenario: A right password clears the count
- **WHEN** the handover succeeds
- **THEN** the caller's failed attempts are cleared

### Requirement: What moves with the role
In the same step, every group owned by the caller, in any state, SHALL become owned by the target, and every account the caller created SHALL be recorded as created by the target. The target's own record of who created them SHALL be cleared, and the caller's account SHALL be recorded as created by the target, so that the target manages the caller like any other account they created.

#### Scenario: Groups move
- **WHEN** the caller owns a Draft, a Pending, an Active and an Archived group and hands over
- **THEN** the target owns all four and the caller owns none

#### Scenario: Created accounts move
- **WHEN** the caller created `carol` and `dave` and hands over
- **THEN** both are recorded as created by the target

#### Scenario: The target is not their own creator
- **WHEN** the target had been created by the caller
- **THEN** the target's record has no creator afterwards

#### Scenario: The old administrator is managed by the new one
- **WHEN** the caller is in no group after the handover
- **THEN** the target can see the caller among the users they manage, and can delete the account, as any account they created that is in no group

### Requirement: All or nothing, at most once
The role change, the group transfer and the account re-parenting SHALL either all happen or none, and two handovers started together by the same caller SHALL succeed at most once.

#### Scenario: A failure part-way
- **WHEN** any part of the handover cannot be stored
- **THEN** the caller is still the administrator, owns the same groups and has the same created accounts

#### Scenario: Twice
- **WHEN** the same caller sends two handovers at once, to two different people
- **THEN** one succeeds and the other is refused with 403 or 409, and exactly one person holds the role

### Requirement: What a handover does not change
A handover SHALL NOT change anyone's password, wishlist, delivery info, letters, questions or membership in any group, SHALL NOT draw or redraw anything, and SHALL NOT send any notification. The caller SHALL stay a participant of every group they take part in, and their access token SHALL stay valid but no longer admit them to `/admin/...` endpoints. The target's access token SHALL admit them to those endpoints from their next request.

#### Scenario: Still a participant
- **WHEN** the caller was a participant of a group they owned and hands over
- **THEN** they are still a participant of it and now see it as one

#### Scenario: The old role ends at once
- **WHEN** the caller calls an `/admin/...` endpoint with their old token after the handover
- **THEN** the response is 403, not 401

#### Scenario: The new role starts at once
- **WHEN** the target calls an `/admin/...` endpoint with their existing token after the handover
- **THEN** the request is processed

### Requirement: The start-up settings do not bring the old administrator back
After a handover, `ADMIN_USERNAME` and `ADMIN_PASSWORD` SHALL still be ignored while any user exists.

#### Scenario: Restart
- **WHEN** the server restarts after a handover
- **THEN** the caller is not made an administrator again and no account is created or changed

## Purpose

Defines how an administrator hands their role over to another person in the app: the action on a user's page, the confirmation with the password, what the app does afterwards and what it says when the server refuses.

## ADDED Requirements

### Requirement: Handing the administrator role over
The detail of a managed user other than the signed-in administrator SHALL offer "Hand over administration", which opens a confirmation asking for the administrator's own password. Confirming SHALL send `POST /admin/users/{username}/hand-over-admin` with that password, and the confirm button SHALL stay disabled until a password is typed.

#### Scenario: Confirmation text
- **WHEN** the administrator taps "Hand over administration" on `alice`'s page
- **THEN** a dialog names `alice`, says that she becomes the administrator and the signed-in person stops being one, that their groups and the accounts they created become hers, that only she can reverse it, and asks for the password

#### Scenario: Not enabled without a password
- **WHEN** the password field is empty
- **THEN** the confirm button is disabled

#### Scenario: Cancelled
- **WHEN** the administrator cancels the dialog
- **THEN** nothing is sent

#### Scenario: Own page
- **WHEN** the administrator opens their own detail
- **THEN** there is no "Hand over administration"

### Requirement: Someone on a temporary password cannot be chosen
For a user who still has a temporary password, "Hand over administration" SHALL be disabled and the page SHALL say that the person must first change their temporary password.

#### Scenario: Temporary password
- **WHEN** the administrator opens the page of a user who has not changed their password
- **THEN** the action is disabled and a line explains why

### Requirement: What the app does after a handover
After the server answers 204, the app SHALL reload the signed-in account, which is no longer an administrator, leave every admin screen and show the participant view on its Groups tab with no administrator switch, without asking for a new sign-in, and SHALL say once, in a message, that the named person is now the administrator.

#### Scenario: Back to a participant
- **WHEN** the handover succeeds
- **THEN** the app shows the Groups tab of a participant, there is no way left into the admin view, and a message says that `alice` is now the administrator

#### Scenario: Not signed out
- **WHEN** the handover succeeds
- **THEN** the person stays signed in and can use the app as a participant

### Requirement: The new administrator's app
When the app loads an account that is an administrator, it SHALL show the admin view as it does for any administrator, and an app that is open when the role arrives SHALL show it the next time it loads the account (when it is opened again).

#### Scenario: Next start
- **WHEN** `alice` opens the app after the handover
- **THEN** the admin view is available to her and the groups handed over are in it

### Requirement: Refusals are said plainly
When the server refuses, the dialog SHALL stay open on the password for a wrong password with the server's message, and SHALL otherwise close and say what happened: that the person is no longer available (404), that the person cannot take over now (409), that there have been too many attempts and when to try again (429), or that the server could not be reached. Nothing SHALL change in the app in these cases.

#### Scenario: Wrong password
- **WHEN** the server answers 400 naming `password`
- **THEN** the dialog stays open with the message and the password field

#### Scenario: Person left the scope
- **WHEN** the server answers 404
- **THEN** the page says the user is no longer available and offers to go back

#### Scenario: Too many attempts
- **WHEN** the server answers 429
- **THEN** the dialog says there have been too many attempts and when to try again

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached
- **THEN** the dialog says so and the role has not moved

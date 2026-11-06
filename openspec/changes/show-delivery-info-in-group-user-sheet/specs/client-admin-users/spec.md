# Spec Delta

## MODIFIED Requirements

### Requirement: User detail shows the account
Choosing a user on the Users tab SHALL open their detail, and choosing a username in a group's pair list SHALL open the same account detail in a bottom sheet. The detail is loaded with `GET /admin/users/{username}` and shows the username and, while it is temporary, the password. On the Users tab it SHALL NOT show a name, email, phone, address or delivery information; the sheet opened from a group adds the group's delivery info, as `client-admin-oversight` describes.

#### Scenario: Temporary password visible
- **WHEN** a managed user has not changed their temporary password
- **THEN** the detail shows it with a note that the person must change it at first sign-in

#### Scenario: Hidden after change
- **WHEN** a managed user has set their own password
- **THEN** the detail shows no password

#### Scenario: No personal details
- **WHEN** a managed user's detail is shown on the Users tab
- **THEN** it has no name, email, phone, address or delivery information

#### Scenario: User out of scope
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the screen or sheet says the user is no longer available and offers to go back or close

#### Scenario: Same detail in a sheet
- **WHEN** the owner chooses a username in a group's pair list
- **THEN** the sheet shows the same username, temporary password rule and "Reset password" as the Users tab detail, followed by the group's delivery info

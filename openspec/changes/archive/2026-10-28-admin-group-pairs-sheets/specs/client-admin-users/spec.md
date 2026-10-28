# Spec Delta

## MODIFIED Requirements

### Requirement: User detail
Choosing a user on the Users tab SHALL open their detail, and choosing a username in a group's pair list SHALL show the same detail in a bottom sheet. The detail is loaded with `GET /admin/users/{username}`, showing the username and each of full name, email, phone, address and delivery information that is set, leaving out the others. For a person who still has a temporary password the detail SHALL also show that password, selectable so it can be copied; for a person who has set their own password it SHALL show none.

#### Scenario: Profile fields
- **WHEN** a user has a full name and an address but no phone
- **THEN** the detail shows those two and nothing for the phone

#### Scenario: Temporary password visible
- **WHEN** a managed user has not changed their temporary password
- **THEN** the detail shows it with a note that the person must change it at first sign-in

#### Scenario: Hidden after change
- **WHEN** a managed user has set their own password
- **THEN** the detail shows no password

#### Scenario: User out of scope
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the screen or sheet says the user is no longer available and offers to go back or close

#### Scenario: Same detail in a sheet
- **WHEN** the owner chooses a username in a group's pair list
- **THEN** a sheet shows the same fields and temporary password rule as the Users tab detail

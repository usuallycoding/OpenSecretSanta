# Spec Delta

## MODIFIED Requirements

### Requirement: List and view managed users
The server SHALL expose `GET /admin/users` listing the admin's managed users and `GET /admin/users/{username}` returning one with its admin flag, `mustChangePassword` and, only while the user's password is temporary, the current temporary password.

#### Scenario: Temporary password visible
- **WHEN** an admin views a managed user who has not yet changed their temporary password
- **THEN** the response includes that temporary password in clear text

#### Scenario: Hidden after change
- **WHEN** an admin views a managed user who has set their own password
- **THEN** the response contains no temporary password

#### Scenario: List is scoped
- **WHEN** an admin lists users
- **THEN** only users within their managed scope are returned


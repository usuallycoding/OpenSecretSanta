## ADDED Requirements

### Requirement: Administrators come from start-up or a handover
The administrator role SHALL come into existence only through the start-up default administrator, or by an administrator handing their own role over to another user, which removes it from them. No operation SHALL make a user an administrator while leaving the one who gave the role an administrator.

#### Scenario: Only by handover
- **WHEN** an administrator uses any route of the server
- **THEN** none of them leaves the number of administrators higher than it was, except by the start-up default on an empty database

#### Scenario: The role moves
- **WHEN** an administrator hands the role over
- **THEN** the person who receives it is an administrator and the one who gave it is not

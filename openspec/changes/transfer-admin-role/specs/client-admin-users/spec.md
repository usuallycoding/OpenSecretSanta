## MODIFIED Requirements

### Requirement: No way to make an administrator
The app SHALL offer no action that adds an administrator. The only action that gives anyone administrator rights SHALL be the handover of the signed-in administrator's own role, from a user's page, which takes it from them (see "Handing the administrator role over").

#### Scenario: User detail
- **WHEN** the administrator opens any user's detail
- **THEN** there is no action that adds an administrator: the only one that gives the role also takes it from the administrator

#### Scenario: Own page
- **WHEN** the administrator opens their own detail
- **THEN** there is no handover action

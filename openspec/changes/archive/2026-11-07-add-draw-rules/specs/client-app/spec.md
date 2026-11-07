## ADDED Requirements

### Requirement: The rules page has a readable web address
On the web, the rules page of an administrator's group SHALL have the address `#/admin/groups/{id}/rules`, which opens it for an administrator and names no screen for anyone else. On a wide window that address SHALL show the group page with the rules in the pane beside it.

#### Scenario: Address of the page
- **WHEN** the admin is on the rules page of group 7 on a narrow window
- **THEN** the address is `#/admin/groups/7/rules`

#### Scenario: Opening the address
- **WHEN** an administrator opens `#/admin/groups/7/rules` on a narrow window
- **THEN** the rules page of group 7 opens, with the group page behind it

#### Scenario: Opening the address on a wide window
- **WHEN** an administrator opens `#/admin/groups/7/rules` on a wide window
- **THEN** the group page shows with the rules in its pane

#### Scenario: Not an administrator
- **WHEN** someone who is not an administrator opens that address
- **THEN** it names no screen, as any admin address does

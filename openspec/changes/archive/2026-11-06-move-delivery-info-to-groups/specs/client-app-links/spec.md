# Spec Delta

## MODIFIED Requirements

### Requirement: Only group addresses open from a link
A link SHALL open only the group screens named above. A link with the address of an admin screen, of the settings or of a group's delivery info, or of nothing the app knows, SHALL open the app's first screen and no other.

#### Scenario: Admin address
- **WHEN** a link names `/#/admin/users/alice`
- **THEN** the app opens its first screen, whoever the person is

#### Scenario: Unknown address
- **WHEN** a link has no address after the `#`, or one that names no screen
- **THEN** the app opens its first screen


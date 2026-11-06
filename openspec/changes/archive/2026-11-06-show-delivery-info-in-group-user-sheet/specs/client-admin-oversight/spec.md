# Spec Delta

## ADDED Requirements

### Requirement: The user sheet shows the group's delivery info
Below the account part, the user sheet SHALL show the person's delivery info for the group, loaded with `GET /admin/groups/{id}/participants/{username}/delivery-info`: name, email, phone, address and delivery information, leaving out what is not set, and saying the person has not added delivery info yet when none is set. It SHALL be read-only, and SHALL load and fail apart from the account part.

#### Scenario: Delivery info shown
- **WHEN** the owner opens the sheet of `bob`, who set a full name, an email and an address in the group
- **THEN** below his username the sheet shows his name, email and address, and no phone or delivery information

#### Scenario: Nothing set
- **WHEN** the person has set no delivery info in the group
- **THEN** the sheet says the person has not added delivery info yet

#### Scenario: Account part unaffected
- **WHEN** the sheet is shown
- **THEN** it also shows the username, the temporary password while temporary and "Reset password" as before

#### Scenario: Failure of one part
- **WHEN** the delivery info cannot be loaded but the account loaded, or the account cannot be loaded but the delivery info did
- **THEN** the part that loaded stays shown and the other says what went wrong with "Try again"

#### Scenario: Person or group not readable
- **WHEN** the server answers 404 for the delivery info, or 409 `invalid_state` because the group is no longer readable
- **THEN** that part says the person is no longer available, or that the group is not in a state where this can be read, as in "Oversight errors and refreshing"

#### Scenario: Read-only
- **WHEN** the sheet is shown, whatever the group's state
- **THEN** there is no control that changes the person's delivery info

#### Scenario: Reloaded
- **WHEN** the sheet is closed and opened again
- **THEN** both parts load again and show what the server now says

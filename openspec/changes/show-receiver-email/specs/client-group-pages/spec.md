# Spec Delta

## MODIFIED Requirements

### Requirement: User details sheets
Choosing the person's own username SHALL open a bottom sheet with their own details (username and the profile fields they have set), and choosing the receiver's username SHALL open a bottom sheet loaded with `GET /groups/{id}/receiver` showing the receiver's username and, for each of full name, email, address, phone and delivery information that they have set, its value, leaving out what is not set and saying that the receiver has not added postal details yet when none is. "Secret Santa" SHALL NOT be tappable while the group is Pending or Active and nothing on the page SHALL identify the Santa then; once the group is Archived, choosing it SHALL open a bottom sheet loaded with `GET /groups/{id}/santa` showing the Santa's username and full name when it has one. The sheets are part of the group page, not screens, so the web address stays on the page while one is open.

#### Scenario: My details
- **WHEN** the person chooses their own username
- **THEN** a sheet shows their username and the profile fields they have set

#### Scenario: Receiver's details
- **WHEN** the receiver has set a full name and an address but no email, phone or delivery information
- **THEN** the sheet shows the username, the full name and the address and nothing for the others

#### Scenario: Receiver's email
- **WHEN** the receiver has set an email address
- **THEN** the sheet shows it, labelled as the email, with the other details that are set

#### Scenario: Only an email set
- **WHEN** the receiver has set an email address and none of the other four fields
- **THEN** the sheet shows the username and the email and does not say that postal details are missing

#### Scenario: No postal details yet
- **WHEN** the receiver has set none of the five fields
- **THEN** the sheet shows the username and says the receiver has not added postal details yet

#### Scenario: Santa hidden before archiving
- **WHEN** the group is Active
- **THEN** "Secret Santa" cannot be chosen and nothing shows who the Santa is

#### Scenario: Santa revealed after archiving
- **WHEN** the group is Archived and the person chooses "Secret Santa"
- **THEN** a sheet shows the Santa's username and full name

#### Scenario: Revealed too early
- **WHEN** the server refuses the Santa request because the group is not Archived
- **THEN** the sheet says the Santa is revealed once the group is archived

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist
- **THEN** the sheet says the group is no longer available and offers to close

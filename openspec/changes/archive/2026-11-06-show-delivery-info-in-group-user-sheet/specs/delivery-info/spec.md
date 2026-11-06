# Spec Delta

## MODIFIED Requirements

### Requirement: Others read delivery info only as the draw allows
Another participant's delivery info SHALL be readable only as `gift-visibility` describes: the receiver's by their Santa, and the Santa's full name after the group is archived. The group's owner SHALL see, for each participant, only the full name and whether the delivery info is complete in the group view, as `groups` describes, and MAY read one participant's whole delivery info for the group through `admin-oversight`.

#### Scenario: A stranger's details
- **WHEN** `carol`, who is neither `bob`'s Santa nor his owner, asks for `bob`'s delivery info
- **THEN** no route gives it to her

#### Scenario: The owner's view
- **WHEN** the owner views a group
- **THEN** each participant carries a full name and a completeness flag, and no email, phone, address or delivery information

#### Scenario: The owner reads one person's details
- **WHEN** the owner reads `bob`'s delivery info for a group they own, through the oversight route
- **THEN** the response has all of `bob`'s fields for that group, and the group view still carries only a full name and a completeness flag

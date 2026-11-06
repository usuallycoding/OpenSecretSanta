# Spec Delta

## MODIFIED Requirements

### Requirement: The Delivery Info page
The "Edit Delivery Info" button SHALL open a page titled "Delivery Info" for that group, loaded with `GET /groups/{id}/delivery-info`, where the person edits their full name, email, phone, address and delivery information. The first four SHALL be marked as required and the delivery information MAY be empty. "Save" SHALL send all five with `PUT /groups/{id}/delivery-info` and be unavailable while nothing has changed or a save runs; after a save the page SHALL say so.

#### Scenario: Existing delivery info
- **WHEN** the person opens the page and has saved a name and an address for the group
- **THEN** those fields hold them and the others are empty

#### Scenario: Nothing yet
- **WHEN** the person has not filled anything in for the group
- **THEN** every field is empty

#### Scenario: Saving
- **WHEN** the person changes their address and saves
- **THEN** the new address is stored for that group and the page says "Saved."

#### Scenario: Cleared field
- **WHEN** the person empties the delivery information and saves, with the other four filled in
- **THEN** the delivery information is cleared on the server

#### Scenario: Required fields marked
- **WHEN** the page is shown
- **THEN** the full name, email, phone and address are marked as required and the delivery information is not

#### Scenario: Back
- **WHEN** the person goes back from the page
- **THEN** the group page is shown

### Requirement: Delivery info is checked before it is sent
Before sending, the page SHALL check what the server checks: the full name, email, phone and address are not blank, the email is a valid address, and each field is at most 500 characters. Every problem SHALL be shown next to its field at once and nothing SHALL be sent.

#### Scenario: Missing required fields
- **WHEN** the person leaves the phone and the address empty and saves
- **THEN** both fields say they are required and nothing is sent

#### Scenario: Delivery information left empty
- **WHEN** the person fills in the four required fields and leaves the delivery information empty
- **THEN** the save is sent

#### Scenario: Invalid email
- **WHEN** the person types an email that is not a valid address and saves
- **THEN** the email field shows that and nothing is sent

## ADDED Requirements

### Requirement: Why the four details matter
The page SHALL say that a Santa uses the person's name and address to post their gift and also sees their email address and phone, and that the email is used to send them their draw. While any of the full name, email, phone or address is empty it SHALL say more directly that all four are needed. It SHALL NOT say or imply who the person's Santa or receiver is, or whether the group has been drawn.

#### Scenario: Incomplete
- **WHEN** any of the four required fields is empty
- **THEN** the page says more directly that all four are needed for a gift to reach the person

#### Scenario: Complete
- **WHEN** all four are filled in
- **THEN** the page gives the general explanation only

## REMOVED Requirements

### Requirement: Why the name and the address matter
**Reason**: All of the full name, email, phone and address are now required, so the explanation is about the four, not the name and the address.
**Migration**: See "Why the four details matter".

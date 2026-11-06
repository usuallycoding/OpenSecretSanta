# client-delivery-info Specification

## Purpose
The page where a participant edits their delivery info for one group: its fields, what is checked, what it tells the person and how it is reached.

## Requirements

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

### Requirement: The page loads, fails and can be changed only while the group allows
While the delivery info loads the page SHALL show a progress indicator, and when it cannot be loaded it SHALL say so without technical details and offer to try again. In an Archived group the page SHALL NOT be offered, and a save that the server refuses because the group is closed SHALL say so.

#### Scenario: Loading
- **WHEN** the page is opened
- **THEN** a progress indicator is shown until the delivery info arrives

#### Scenario: Cannot be loaded
- **WHEN** the delivery info cannot be loaded
- **THEN** the page says so and offers "Try again"

#### Scenario: Group archived meanwhile
- **WHEN** the person saves and the server answers that the group no longer allows changes
- **THEN** the page says the group is closed for edits and keeps what was typed

### Requirement: The page has a web address
In the web app the page SHALL have the address `#/groups/{id}/delivery-info`. Opening or refreshing that address while signed in SHALL show the page for that group.

#### Scenario: Address follows
- **WHEN** the person opens the page for group 2
- **THEN** the address ends in `#/groups/2/delivery-info`

### Requirement: Address and delivery information take several lines
The address and delivery information fields SHALL accept line breaks: the keyboard's Return key SHALL start a new line in them and SHALL NOT move to another field. A line break typed inside the text SHALL be kept, and blank lines before and after it SHALL be dropped when it is saved.

#### Scenario: Return in the address
- **WHEN** the person presses Return in the address field after typing "1 Main Street"
- **THEN** a new line starts in that field

#### Scenario: Two lines kept
- **WHEN** the person saves the address "1 Main Street" and "Springfield" on two lines
- **THEN** the saved address has both lines

#### Scenario: Blank lines dropped
- **WHEN** the person saves an address with an empty line before and after the text
- **THEN** the saved address has neither

### Requirement: Why the four details matter
The page SHALL say that a Santa uses the person's name and address to post their gift and also sees their email address and phone, and that the email is used to send them their draw. While any of the full name, email, phone or address is empty it SHALL say more directly that all four are needed. It SHALL NOT say or imply who the person's Santa or receiver is, or whether the group has been drawn.

#### Scenario: Incomplete
- **WHEN** any of the four required fields is empty
- **THEN** the page says more directly that all four are needed for a gift to reach the person

#### Scenario: Complete
- **WHEN** all four are filled in
- **THEN** the page gives the general explanation only

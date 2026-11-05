# Spec Delta

## MODIFIED Requirements

### Requirement: User details sheets
The "Delivery Info" button of the "Your stuff" card SHALL open a bottom sheet with the person's own details (the username and the delivery info they have set for the group), and that of the "Secret Santa" card one loaded with `GET /groups/{id}/receiver` showing the receiver's username and, for each of full name, email, address, phone and delivery information that they have set, its value, leaving out what is not set and saying that the receiver has not added postal details yet when none is.

#### Scenario: My details
- **WHEN** the person chooses "Delivery Info" in the "Your stuff" card
- **THEN** a sheet shows their username and the delivery info they have set for this group

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
- **THEN** nothing offers to reveal the Santa and nothing shows who the Santa is

#### Scenario: Santa revealed after archiving
- **WHEN** the group is Archived and the person chooses "Reveal Santa"
- **THEN** a sheet shows the Santa's username and full name

#### Scenario: Revealed too early
- **WHEN** the server refuses the Santa request because the group is not Archived
- **THEN** the sheet says the Santa is revealed once the group is archived

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist
- **THEN** the sheet says the group is no longer available and offers to close

### Requirement: The Your stuff card
Below the Secret Santa card the group page SHALL show a card headed "Your stuff:", the line "Tell your santa what you want and let them know you!" in italics under it, "Edit Wishlist" and, once the group is Active or Archived, "Respond to Santa" on one row and, last, a "Delivery Info" button on its own row. In an Archived group it SHALL also have, above that, the "Reveal Santa" button of `The Santa stays hidden until the group is archived`.

#### Scenario: The encouraging line
- **WHEN** the card is shown in a Pending, an Active or an Archived group
- **THEN** the line "Tell your santa what you want and let them know you!" is under "Your stuff:", in italics

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the card has "Delivery Info" and "Edit Wishlist" and no "Respond to Santa"

#### Scenario: Active
- **WHEN** the group is Active
- **THEN** the card has "Delivery Info", "Edit Wishlist" and "Respond to Santa" and no "Reveal Santa"

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the card has "Delivery Info", "Edit Wishlist", "Respond to Santa" and "Reveal Santa"

#### Scenario: Delivery Info
- **WHEN** the person chooses "Delivery Info"
- **THEN** a sheet shows their own username and the delivery info they have set for this group

#### Scenario: Edit Wishlist
- **WHEN** the person chooses "Edit Wishlist"
- **THEN** the Receiver page opens on their wishlist tab

#### Scenario: Respond to Santa
- **WHEN** the person chooses "Respond to Santa" in an Active group
- **THEN** the Receiver page opens on its "Respond to Secret Santa" tab

#### Scenario: Group phase not known
- **WHEN** the group has not loaded
- **THEN** neither card is shown

### Requirement: Edit Delivery Info
The sheet of the "Your stuff" card's "Delivery Info" button SHALL end with an "Edit Delivery Info" button, below the details or the note that none are set, while the group is Pending or Active. Choosing it SHALL close the sheet and open the group's Delivery Info page. The receiver's sheet SHALL NOT have this button.

#### Scenario: The button
- **WHEN** the person's own Delivery Info sheet is shown with their details
- **THEN** the last thing in the sheet is an "Edit Delivery Info" button

#### Scenario: No profile yet
- **WHEN** the person has set no delivery info for the group and opens their own sheet
- **THEN** the sheet says so and still ends with the "Edit Delivery Info" button

#### Scenario: Opening the profile
- **WHEN** the person chooses "Edit Delivery Info"
- **THEN** the sheet closes and the Delivery Info page of that group is shown

#### Scenario: Not on the receiver's sheet
- **WHEN** the person opens their receiver's Delivery Info sheet
- **THEN** the sheet has no "Edit Delivery Info" button

#### Scenario: Archived group
- **WHEN** the person opens their own Delivery Info sheet in an Archived group
- **THEN** the sheet has no "Edit Delivery Info" button


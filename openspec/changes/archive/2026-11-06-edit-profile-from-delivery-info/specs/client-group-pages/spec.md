# Spec Delta

## ADDED Requirements

### Requirement: The username is centered in the Delivery Info sheets
In the two sheets opened by the "Delivery Info" buttons the username SHALL be centered horizontally in the sheet, with its copy icon at its end, so that the name itself stays in the middle whether or not the icon is there.

#### Scenario: Own sheet
- **WHEN** the person opens their own Delivery Info sheet
- **THEN** their username is in the middle of the sheet's width, with the copy icon beside it

#### Scenario: Receiver's sheet
- **WHEN** the person opens their receiver's Delivery Info sheet
- **THEN** the receiver's username is in the middle of the sheet's width, with the copy icon beside it

#### Scenario: A long username
- **WHEN** a username is longer than one row
- **THEN** it is cut as usual and choosing it opens the dialog with all of it

### Requirement: Edit Delivery Info
The sheet of the "Your stuff" card's "Delivery Info" button SHALL end with an "Edit Delivery Info" button, below the details or the note that none are set. Choosing it SHALL close the sheet and open the Profile tab. The receiver's sheet SHALL NOT have this button.

#### Scenario: The button
- **WHEN** the person's own Delivery Info sheet is shown with their details
- **THEN** the last thing in the sheet is an "Edit Delivery Info" button

#### Scenario: No profile yet
- **WHEN** the person has not filled in a profile and opens their own sheet
- **THEN** the sheet says so and still ends with the "Edit Delivery Info" button

#### Scenario: Opening the profile
- **WHEN** the person chooses "Edit Delivery Info"
- **THEN** the sheet closes and the Profile tab is shown, as when the Profile tab is chosen in the tab bar

#### Scenario: Not on the receiver's sheet
- **WHEN** the person opens their receiver's Delivery Info sheet
- **THEN** the sheet has no "Edit Delivery Info" button

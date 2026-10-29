# client-profile Specification

## Purpose

How a person keeps their own profile in the app: the Profile tab, saving name, email, phone, address and delivery information, what is checked and why the name and address matter.

## Requirements

### Requirement: Profile tab
The participant view SHALL have a Profile tab, loaded with `GET /me`, that shows the person's username, which cannot be changed, and lets them edit their full name, email, phone number, address and delivery information, each of which may be empty.

#### Scenario: Existing profile
- **WHEN** the person opens the Profile tab and has saved a name and an address
- **THEN** the username is shown as text and the five fields are filled with what is saved

#### Scenario: New account
- **WHEN** the person has not filled in a profile yet
- **THEN** the five fields are empty

#### Scenario: Username
- **WHEN** the person looks at the form
- **THEN** the username is shown but cannot be edited

### Requirement: Saving the profile
The app SHALL save the profile with `PUT /me`, sending all five fields and the unchanged username, and SHALL treat a field left blank as cleared. Saving SHALL be unavailable while nothing has changed or a save is in progress. After a successful save the app SHALL say so and treat the saved values as the new starting point.

#### Scenario: Saved
- **WHEN** the person changes their address and saves
- **THEN** the server receives all five fields, a confirmation is shown and saving is unavailable again

#### Scenario: Cleared field
- **WHEN** the person empties the phone number and saves
- **THEN** the phone number is cleared on the server

#### Scenario: Nothing changed
- **WHEN** the form still shows exactly what was loaded
- **THEN** saving is unavailable

#### Scenario: Unsaved changes while switching tabs
- **WHEN** the person edits a field and switches to the Groups tab and back
- **THEN** their edits are still there

### Requirement: Profile validation
Before sending, the app SHALL check what the server checks: an email, when given, is a valid address, and each field is at most 500 characters. A problem SHALL be shown next to its field and nothing SHALL be sent while one exists. When the server refuses a save with an error naming a field, the app SHALL show that message next to that field.

#### Scenario: Invalid email
- **WHEN** the person enters `not-an-email` and saves
- **THEN** the email field shows that the address is not valid and nothing is sent

#### Scenario: Too long
- **WHEN** a field has more than 500 characters
- **THEN** that field shows the limit and nothing is sent

#### Scenario: Server refuses
- **WHEN** the server answers a save with a validation error naming a field
- **THEN** its message is shown next to that field and the person's edits are kept

#### Scenario: Server unreachable
- **WHEN** a save cannot reach the server
- **THEN** the form says so, keeps the edits and lets the person try again

### Requirement: Why name and address matter
The Profile tab SHALL say that a Santa uses the person's name and address to post their gift, and that the person's email address is used to send them their draw when a group they are in is drawn. While the name or the address is empty it SHALL say so more directly. It SHALL NOT say or imply who the person's Santa or receiver is, or whether groups have been drawn.

#### Scenario: Incomplete profile
- **WHEN** the name or the address is empty
- **THEN** the tab asks the person to add them so a gift can reach them

#### Scenario: Complete profile
- **WHEN** both the name and the address are filled in
- **THEN** only the general note is shown

#### Scenario: What the email is for
- **WHEN** the Profile tab is shown
- **THEN** its general note says the email is used to send the person their draw

### Requirement: Profile loading and errors
While the profile loads the tab SHALL show a progress indicator. When it cannot be loaded the tab SHALL say so without technical details and offer to try again.

#### Scenario: Loading
- **WHEN** the Profile tab is opened
- **THEN** a progress indicator is shown until the profile arrives

#### Scenario: Load fails
- **WHEN** the profile cannot be loaded
- **THEN** the tab shows a message with "Try again"

### Requirement: Address and delivery information take several lines
The address and delivery information fields on the Profile tab SHALL accept line breaks: the keyboard's Return key SHALL start a new line in them and SHALL NOT move to another field. A line break typed inside the text SHALL be saved and shown unchanged wherever the Santa reads it. Text before and after the whole value is still trimmed.

#### Scenario: Return starts a new line
- **WHEN** the person presses Return in the address field after typing "1 Main Street"
- **THEN** the cursor is on a new line of the same field and no other field has focus

#### Scenario: Several lines are saved
- **WHEN** the person saves the address "1 Main Street" and "Springfield" on two lines
- **THEN** the server receives both lines with the line break between them

#### Scenario: The Santa reads the lines
- **WHEN** the Santa opens the receiver's page for a receiver whose address has two lines
- **THEN** the address is shown on two lines

#### Scenario: Outer blank lines are trimmed
- **WHEN** the person saves an address with an empty line before and after the text
- **THEN** the saved address has neither

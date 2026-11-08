# Spec Delta

## MODIFIED Requirements

### Requirement: Writing a letter
While the group is Active the Send letter page SHALL have a round "+" button that opens a bottom sheet with a title field and a description field, both required, and a "Send" button. A blank title or description, a title over 100 characters or a description over 2000 SHALL be refused next to its field before anything is sent. A successful send SHALL close the sheet and show the new letter first. In an Archived group there SHALL be no "+" button.

#### Scenario: Open the sheet
- **WHEN** the person presses "+" in an Active group
- **THEN** a bottom sheet opens with an empty title and an empty description

#### Scenario: Sending
- **WHEN** the person fills in both and chooses "Send"
- **THEN** the letter is sent, the sheet closes and the letter is at the top of the page

#### Scenario: Both required
- **WHEN** the person leaves the title or the description empty and chooses "Send"
- **THEN** that field says it is required, and nothing is sent

#### Scenario: Too long
- **WHEN** the title is over 100 characters or the description over 2000
- **THEN** that field says how long it may be, and nothing is sent

#### Scenario: Letter limit reached
- **WHEN** the server refuses because 200 letters were already sent
- **THEN** the sheet says so, keeps what was typed and sends nothing

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the page shows the letters and has no "+" button

#### Scenario: Dismissing
- **WHEN** the person closes the sheet without sending
- **THEN** nothing is sent and what was typed is discarded

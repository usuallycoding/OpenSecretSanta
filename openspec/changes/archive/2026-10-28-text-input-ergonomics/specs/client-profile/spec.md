# Spec Delta

## ADDED Requirements

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

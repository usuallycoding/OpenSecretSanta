# Spec Delta

## ADDED Requirements

### Requirement: The Your stuff card asks for missing delivery info
In a Pending or an Active group, when the person's delivery info for the group lacks the full name, email, phone or address, the "Your stuff:" card SHALL show the text "Please fill out your delivery info in order to be able to receive your secret gift!" in the theme's error colour, directly above the "Delivery Info" button. It SHALL be plain text, not a control, and SHALL NOT block any action.

#### Scenario: Incomplete in a Pending group
- **WHEN** the group is Pending and the person has not set their phone
- **THEN** the message is shown directly above the "Delivery Info" button

#### Scenario: Incomplete in an Active group
- **WHEN** the group is Active and the person has set nothing
- **THEN** the message is shown directly above the "Delivery Info" button, below "Reveal Santa" if that is shown

#### Scenario: Complete
- **WHEN** the person has set their full name, email, phone and address
- **THEN** no message is shown

#### Scenario: Only the delivery information empty
- **WHEN** the person has set the four required fields and left the delivery information empty
- **THEN** no message is shown

#### Scenario: Archived group
- **WHEN** the group is Archived and the person's delivery info is incomplete
- **THEN** no message is shown, because nothing can be changed any more

#### Scenario: Wording
- **WHEN** the message is shown
- **THEN** it reads exactly "Please fill out your delivery info in order to be able to receive your secret gift!" in the error colour, and a screen reader reads it as text

#### Scenario: Nothing is blocked
- **WHEN** the message is shown
- **THEN** every button of the card can still be used

### Requirement: The group page checks the person's delivery info quietly
While the group is Pending or Active the group page SHALL load the person's own delivery info with `GET /groups/{id}/delivery-info` and reload it whenever the page is shown again, is pulled to refresh or returns to the foreground. Until the first load has finished, or when loading fails, the page SHALL show no message about it and SHALL NOT show the missing delivery info message; a reload keeps what was shown until its answer arrives.

#### Scenario: Loading
- **WHEN** the group page is opened and the delivery info has not arrived yet
- **THEN** no message is shown

#### Scenario: Reloading
- **WHEN** the message is shown and the page reloads the delivery info
- **THEN** the message stays until the answer arrives, and goes if the answer is complete

#### Scenario: Loading fails
- **WHEN** the person's delivery info cannot be loaded, or a reload fails after an earlier load
- **THEN** no message is shown and the page shows no error about it

#### Scenario: Gone after saving
- **WHEN** the person fills in the missing fields on the Delivery Info page, saves and goes back to the group page
- **THEN** the page reloads their delivery info and the message is gone

#### Scenario: Draft group
- **WHEN** the group is a Draft
- **THEN** the person's delivery info is not requested

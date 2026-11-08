# Spec Delta

## MODIFIED Requirements

### Requirement: Opening a notification goes to its group
Every notification SHALL carry the identifier of the group it is about and its category, so that the app can open that group when the notification is tapped. A notification for an announcement the owner sent SHALL also say that it is a message from the owner, so that it can be told from the other notifications of the announcements category, which are about being added to a group and the names being drawn.

#### Scenario: Tap target
- **WHEN** a question notification for group 7 is delivered
- **THEN** it carries group 7 and the category "questions"

#### Scenario: An announcement from the owner
- **WHEN** the owner's announcement is sent as a push notification for group 7
- **THEN** it carries group 7, the category "announcements" and the mark that it is a message from the owner

#### Scenario: Added to a group
- **WHEN** a person is told they were added to group 7, or that its names were drawn
- **THEN** the notification carries group 7 and the category "announcements" and does not carry the mark

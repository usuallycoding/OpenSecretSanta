# Spec Delta

## ADDED Requirements

### Requirement: Sending actions use the Send icon
Asking a question and sending a letter SHALL show the Send icon, as the floating button of a narrow window and as the button of a wide window's heading alike. Adding an item and creating a group SHALL keep the plus icon.

#### Scenario: Ask a question
- **WHEN** the person can ask a question, on a narrow or a wide window
- **THEN** the "Ask a question" button shows the Send icon, not a question mark

#### Scenario: Send letter
- **WHEN** the person can send a letter, on a narrow window
- **THEN** the floating button shows the Send icon, not a plus

#### Scenario: Adding keeps the plus
- **WHEN** the person can add a wishlist item or the administrator can create a group
- **THEN** the button shows the plus icon

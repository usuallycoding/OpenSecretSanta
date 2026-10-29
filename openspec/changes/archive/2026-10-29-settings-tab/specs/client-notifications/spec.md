# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Notifications in the top-bar menu`
- TO: `### Requirement: Notifications in Settings`

## MODIFIED Requirements

### Requirement: Notifications in Settings
On Android and the web, the Settings page, in both the participant and the admin view, SHALL have a "Notifications" row that opens a bottom sheet. On iOS the row SHALL NOT be offered.

#### Scenario: Participant and admin
- **WHEN** a participant or an administrator opens Settings on Android or the web
- **THEN** it has a "Notifications" row that opens the sheet

#### Scenario: iOS
- **WHEN** Settings is opened on iOS
- **THEN** it has no "Notifications" row

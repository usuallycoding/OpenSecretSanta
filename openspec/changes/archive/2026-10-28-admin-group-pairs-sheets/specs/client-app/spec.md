# Spec Delta

## REMOVED Requirements

### Requirement: Oversight routes in the admin view
**Reason**: Oversight is read in bottom sheets on the group detail; there are no routes for a participant's wishlist or a group's questions.
**Migration**: None. The sheets are state of the group screen, so the address bar does not follow them.

## ADDED Requirements

### Requirement: Back closes an open sheet
Going back with the system back button on Android while a bottom sheet is open SHALL close the sheet and stay on the screen behind it. On the web the browser's back button belongs to the screens' history, which a sheet is not part of, so it SHALL leave the screen as it does without a sheet; the Escape key SHALL close an open sheet there.

#### Scenario: Back from a sheet on Android
- **WHEN** a user or pair sheet is open on a group and the person uses the system back button on Android
- **THEN** the sheet closes and the group detail is still shown

#### Scenario: Back again on Android
- **WHEN** the person goes back once more
- **THEN** the groups list is shown

#### Scenario: Browser back on the web
- **WHEN** a sheet is open on a group in the web app and the person presses the browser's back button
- **THEN** the groups list is shown, the page is not reloaded and the session is kept

#### Scenario: Escape on the web
- **WHEN** a sheet is open on a group in the web app and the person presses Escape
- **THEN** the sheet closes and the group detail is still shown

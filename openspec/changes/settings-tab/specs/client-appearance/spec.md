# Spec Delta

## MODIFIED Requirements

### Requirement: Choosing the appearance
The Settings page, in both the participant and the admin view, SHALL have an "Appearance" row that opens a bottom sheet with the choices "Follow device" (the default), "Light" and "Dark". Choosing one SHALL apply it at once, to every screen including the sign-in screen, and keep it until it is changed.

#### Scenario: Choose dark
- **WHEN** the person chooses "Dark" while the device is light
- **THEN** the app turns dark at once and stays dark

#### Scenario: Choose light
- **WHEN** the person chooses "Light" while the device is dark
- **THEN** the app turns light at once and stays light

#### Scenario: Back to the device
- **WHEN** the person chooses "Follow device"
- **THEN** the app shows whatever the device is set to

#### Scenario: Both views
- **WHEN** an administrator opens Settings in the admin view and in the participant view
- **THEN** "Appearance" is in both

#### Scenario: Current choice shown
- **WHEN** the sheet is opened
- **THEN** it shows which choice is in force

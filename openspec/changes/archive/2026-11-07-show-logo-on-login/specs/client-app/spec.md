## MODIFIED Requirements

### Requirement: Sign in
The app SHALL show a sign-in screen when there is no session, which opens with the app's logo in the place of a title, asking for a username and a password, and SHALL sign the person in through `POST /auth/login`. The logo SHALL be an image with the spoken name "Open Secret Santa", and no title text SHALL be shown above the fields. The logo SHALL come in a light and a dark version, and the screen SHALL show the dark one when the theme it is drawn in has a dark background and the light one when it has a light background, whatever chose that theme (the app's own appearance setting, the device, or dynamic colours). The username SHALL be sent as typed, since the server ignores case. The sign-in action SHALL be unavailable while either field is blank or a request is in progress.

#### Scenario: Successful sign in
- **WHEN** a person enters a correct username and password
- **THEN** the app stores the session and shows the first view for that person

#### Scenario: Blank fields
- **WHEN** the username or the password is empty
- **THEN** the sign-in action cannot be used and no request is sent

#### Scenario: Double submit
- **WHEN** a sign-in request is in progress
- **THEN** the action cannot be used again until it finishes

#### Scenario: The logo opens the screen
- **WHEN** the sign-in screen is shown on Android, iOS or the web
- **THEN** the logo is shown above the fields, with no "Open Secret Santa" title text, and a screen reader names the image "Open Secret Santa"

#### Scenario: Light theme
- **WHEN** the sign-in screen is shown on a light background
- **THEN** the light version of the logo is shown

#### Scenario: Dark theme
- **WHEN** the sign-in screen is shown on a dark background
- **THEN** the dark version of the logo is shown, with a light outline where the light version has a black one

#### Scenario: The app's own choice wins
- **WHEN** the device is in dark mode and the appearance setting of the app is Light, or the other way round
- **THEN** the version that fits the theme the app is showing is used, not the device's

#### Scenario: The logo fits the screen
- **WHEN** the sign-in screen is shown on a narrow phone, or with the keyboard open
- **THEN** the logo is no wider than the fields and the whole screen can still be scrolled to the sign-in button

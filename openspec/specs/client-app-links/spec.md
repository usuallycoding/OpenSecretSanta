# client-app-links Specification

## Purpose
How the Android app takes over the web app's group addresses, so that the link in an email opens the group inside the installed app and not in the browser.

## Requirements

### Requirement: Group links open in the Android app
On Android, an address of the web app that the release was built for (for example `https://santa.example.org`) that names a group (`/#/groups/<id>`, or the group's Giver or Receiver page with its tab) SHALL open in the installed app, and not in the browser, once the system has verified that the address belongs to the app.

#### Scenario: Link from an email
- **WHEN** a signed-in person with the app installed taps `https://santa.example.org/#/groups/7` in an email
- **THEN** the app opens on group 7's page, with the group list behind it, and the browser does not open

#### Scenario: Signed out
- **WHEN** a signed-out person taps the link
- **THEN** the sign-in screen is shown, and after signing in the group's page opens

#### Scenario: App already open
- **WHEN** the app is open on another screen and the person taps the link
- **THEN** the app shows group 7's page without starting again

#### Scenario: A group the person is not in
- **WHEN** the link names a group the person cannot read
- **THEN** the screen says the group is not available and offers the way back, as it does for a notification

#### Scenario: A Giver page
- **WHEN** a link names `/#/groups/7/giver/questions`
- **THEN** the Giver page of group 7 opens on its questions tab, with group 7's page and then the group list behind it

#### Scenario: A renamed address
- **WHEN** a link names `/#/groups/7/gifter`
- **THEN** the app opens its first screen and no other

### Requirement: Only group addresses open from a link
A link SHALL open only the group screens named above. A link with the address of an admin screen, of the settings or of a group's delivery info, or of nothing the app knows, SHALL open the app's first screen and no other.

#### Scenario: Admin address
- **WHEN** a link names `/#/admin/users/alice`
- **THEN** the app opens its first screen, whoever the person is

#### Scenario: Unknown address
- **WHEN** a link has no address after the `#`, or one that names no screen
- **THEN** the app opens its first screen

### Requirement: Without the app, or unverified, a link opens the web app
When the app is not installed, when the address could not be verified for the app, or when the app server address is not `https`, the link SHALL open in the browser, and the web app SHALL show the group as it does for a link typed there. A build that the release script did not make, such as a debug build, does not name the web app's host and is never verified.

#### Scenario: App not installed
- **WHEN** a person without the app taps the link
- **THEN** the browser opens the web app on that group

#### Scenario: Not verified
- **WHEN** the system has not verified the address for the app
- **THEN** the link opens in the browser and not in the app

#### Scenario: Debug build
- **WHEN** a debug build installed from Android Studio is on the phone and the person taps the link
- **THEN** the link opens in the browser and not in that build

### Requirement: A link opens its screen as a notification does
The app SHALL open the screen a link names with the group list behind it, as a tap on a notification does, and SHALL wait for sign-in first when the person is signed out.

#### Scenario: Back goes to the group list
- **WHEN** a signed-in person opens a link to group 7 and goes back
- **THEN** the group list is shown

#### Scenario: A page of the group
- **WHEN** a link names `/#/groups/7/receiver/questions`
- **THEN** the Receiver page of group 7 opens on its questions tab, with group 7's page and then the group list behind it

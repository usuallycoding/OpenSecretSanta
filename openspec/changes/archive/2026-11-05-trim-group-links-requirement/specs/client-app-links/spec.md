## MODIFIED Requirements

### Requirement: Group links open in the Android app
On Android, an address of the web app that the release was built for (for example `https://santa.example.org`) that names a group (`/#/groups/<id>`, or the group's Gifter or Receiver page with its tab) SHALL open in the installed app, and not in the browser, once the system has verified that the address belongs to the app.

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

## ADDED Requirements

### Requirement: A link opens its screen as a notification does
The app SHALL open the screen a link names with the group list behind it, as a tap on a notification does, and SHALL wait for sign-in first when the person is signed out.

#### Scenario: Back goes to the group list
- **WHEN** a signed-in person opens a link to group 7 and goes back
- **THEN** the group list is shown

#### Scenario: A page of the group
- **WHEN** a link names `/#/groups/7/receiver/questions`
- **THEN** the Receiver page of group 7 opens on its questions tab, with group 7's page and then the group list behind it

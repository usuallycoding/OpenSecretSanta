## MODIFIED Requirements

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

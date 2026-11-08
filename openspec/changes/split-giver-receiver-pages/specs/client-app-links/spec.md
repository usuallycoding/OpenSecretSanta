# Spec Delta

## MODIFIED Requirements

### Requirement: Group links open in the Android app
On Android, an address of the web app that the release was built for (for example `https://santa.example.org`) that names a group (`/#/groups/<id>`, the group's Giver or Receiver address, the address of one of the six pages of its gift exchange: `giver/wishlist`, `giver/questions`, `inbox`, `receiver/wishlist`, `receiver/questions` or `outbox`, or its `announcements`) SHALL open in the installed app, and not in the browser, once the system has verified that the address belongs to the app.

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
- **THEN** the "Questions for" page of group 7 opens, with group 7's page and then the group list behind it

#### Scenario: Each of the six
- **WHEN** a link names `giver/wishlist`, `inbox`, `receiver/wishlist`, `receiver/questions` or `outbox` of group 7
- **THEN** that page of group 7 opens, with group 7's page and then the group list behind it

#### Scenario: The announcements
- **WHEN** a link names `/#/groups/7/announcements`
- **THEN** the Announcements page of group 7 opens, with group 7's page and then the group list behind it

#### Scenario: A renamed address
- **WHEN** a link names `/#/groups/7/gifter`
- **THEN** the app opens its first screen and no other

### Requirement: A link opens its screen as a notification does
The app SHALL open the screen a link names with the group list behind it, as a tap on a notification does, and SHALL wait for sign-in first when the person is signed out.

#### Scenario: Back goes to the group list
- **WHEN** a signed-in person opens a link to group 7 and goes back
- **THEN** the group list is shown

#### Scenario: A page of the group
- **WHEN** a link names `/#/groups/7/receiver/questions`
- **THEN** the "Respond to Secret Santa" page of group 7 opens, with group 7's page and then the group list behind it

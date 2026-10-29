# client-settings Specification

## Purpose
Where a signed-in person reaches the app's own settings: a Settings item in the bottom bar of both views, and a Settings page that lists appearance, notifications, changing the password and signing out.

## Requirements

### Requirement: The Settings tab
The bottom bar of the participant view and of the admin view SHALL each have a Settings item with a settings icon and the label "Settings", after the view's other tabs. Choosing it SHALL open the Settings page, and the item SHALL be shown as selected while the page is open. Like the other tabs, it SHALL keep its place when the person moves to another tab and back.

#### Scenario: Participant's bar
- **WHEN** a participant is signed in
- **THEN** the bottom bar offers Groups, Profile and Settings, in that order

#### Scenario: Administrator's bar
- **WHEN** an administrator is in the admin view
- **THEN** the bottom bar offers Groups, Users and Settings, in that order

#### Scenario: Selected
- **WHEN** the Settings page is open
- **THEN** the Settings item is shown as selected and no other item is

#### Scenario: One page for both views
- **WHEN** a participant and an administrator each open Settings
- **THEN** both see the same Settings page

### Requirement: The Settings page
The Settings page SHALL be titled "Settings" and SHALL list, in this order, "Appearance", "Notifications", "Change password" and, set apart below them, "Sign out". The Appearance row SHALL show the current choice ("Follow device", "Light" or "Dark") under its name. The Notifications row SHALL NOT be offered on iOS. Every row SHALL be at least 48 density-independent pixels high and shown as something to press.

#### Scenario: Rows in order
- **WHEN** the person opens Settings on Android or the web
- **THEN** the rows are Appearance, Notifications and Change password, and below them, set apart, Sign out

#### Scenario: Appearance shows its choice
- **WHEN** the appearance is set to "Dark"
- **THEN** the Appearance row says "Dark" under its name

#### Scenario: iOS
- **WHEN** the person opens Settings on iOS
- **THEN** there is no Notifications row

### Requirement: Each row does what the menu item did
Choosing "Appearance", "Notifications" or "Change password" SHALL open the same bottom sheet over the Settings page that the top-bar menu item opened, behaving as `client-appearance`, `client-notifications` and `client-app` describe, and closing the sheet SHALL leave the Settings page as it was. Choosing "Sign out" SHALL sign the person out as `client-app` describes.

#### Scenario: A sheet over the page
- **WHEN** the person chooses "Notifications" and then closes the sheet
- **THEN** the sheet opened over Settings and Settings is still shown

#### Scenario: Sign out
- **WHEN** the person chooses "Sign out"
- **THEN** the stored token is deleted and the sign-in screen is shown

### Requirement: Settings does not change the view
Opening the Settings page SHALL NOT switch between the participant view and the admin view: it SHALL be shown in the view the person is in, with that view's bottom bar.

#### Scenario: Administrator in the admin view
- **WHEN** an administrator in the admin view chooses Settings
- **THEN** the Settings page is shown with the admin view's bottom bar, Groups, Users and Settings

#### Scenario: Administrator in the participant view
- **WHEN** an administrator who has switched to the participant view chooses Settings
- **THEN** the Settings page is shown with the participant view's bottom bar

### Requirement: The Settings address on the web
In the web app the Settings page SHALL have the address `#/settings`. Opening or refreshing that address, or typing it while signed in, SHALL show the Settings page in the view the person is in.

#### Scenario: Address follows
- **WHEN** a signed-in person opens Settings in the web app
- **THEN** the address ends in `#/settings`

#### Scenario: Typed address
- **WHEN** a signed-in administrator in the admin view types `#/settings`
- **THEN** the Settings page is shown, in the admin view

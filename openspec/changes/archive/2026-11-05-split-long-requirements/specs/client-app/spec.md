## MODIFIED Requirements

### Requirement: Session storage per platform
On Android the app SHALL keep the access token in encrypted storage backed by the platform keystore, and on iOS in the Keychain, so that the person stays signed in after closing the app. On web the app SHALL keep the token in the browser's local storage when the person chose to stay signed in, so that it survives closing the tab and the browser, and otherwise in the browser tab's session storage, so that it survives a refresh of the page and is lost when the tab is closed.

#### Scenario: Native restart
- **WHEN** a signed-in person closes and reopens the Android or iOS app
- **THEN** the app checks the stored token with the server and, if it is still accepted, shows the first view without asking for the password

#### Scenario: Web refresh
- **WHEN** a signed-in person refreshes the web page
- **THEN** the app checks the stored token with the server and, if it is still accepted, shows the screen named in the address without asking for the password

#### Scenario: Web tab closed
- **WHEN** a person who did not stay signed in closes the web tab and opens the app again in a new tab
- **THEN** the sign-in screen is shown

#### Scenario: Web token rejected after a refresh
- **WHEN** a person refreshes the web page and the server no longer accepts the stored token
- **THEN** the sign-in screen is shown and the stored token is gone

#### Scenario: Web sign out
- **WHEN** a signed-in person signs out on the web and refreshes the page
- **THEN** the sign-in screen is shown

#### Scenario: Not in longer-lived storage
- **WHEN** a person signed in on the web with "Stay signed in" unticked
- **THEN** the token is in the tab's session storage and in neither local storage nor a cookie

#### Scenario: Storage unavailable
- **WHEN** the platform storage cannot be read or written
- **THEN** the app behaves as if there were no stored session and the person can still sign in for the current run

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab, a Profile tab and a Settings tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups, a Users tab and the same Settings tab, under the same top bar.

#### Scenario: Administrator
- **WHEN** an administrator signs in
- **THEN** the admin view is shown and the switch icon in the top bar switches to the participant view and back

#### Scenario: Normal participant
- **WHEN** a non-administrator signs in
- **THEN** only the participant view is shown and there is no way to reach the admin view

#### Scenario: The admin view has two tabs
- **WHEN** an administrator is in the admin view
- **THEN** a bottom bar offers its two tabs, Groups, which shows the administrator's own groups, and Users, and then the Settings tab that both views share

#### Scenario: Switching views
- **WHEN** an administrator switches to the participant view and back
- **THEN** each view opens on its first screen

#### Scenario: The menu in both views
- **WHEN** the person looks at the top bar in the participant view or in the admin view
- **THEN** there is no menu: an administrator sees the one switch icon and anyone else sees no action

### Requirement: Readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/gifter` and `#/groups/2/receiver` for its Gifter and Receiver pages, `#/profile`, `#/settings`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them.

#### Scenario: A group
- **WHEN** a signed-in person opens a group in the web app
- **THEN** the address ends in `#/groups/` followed by the group's number

#### Scenario: A user in the admin view
- **WHEN** an administrator opens the user `alice` in the web app
- **THEN** the address ends in `#/admin/users/alice`

#### Scenario: Back and forward
- **WHEN** the person moves between screens and uses the browser's back and forward buttons
- **THEN** the screens and their addresses follow, as before

#### Scenario: Sheet
- **WHEN** a bottom sheet is open on a group's screen in the web app
- **THEN** the address is still the group's address

#### Scenario: A page on its questions tab
- **WHEN** a signed-in person opens `#/groups/2/receiver/questions` or `#/groups/2/gifter/questions`
- **THEN** that page of group 2 opens on its questions tab

#### Scenario: The tab is where the page opens
- **WHEN** the person switches tabs on a page
- **THEN** the address stays as it was, and a name for the wishlist tab such as `#/groups/2/receiver/wishlist` names no screen

### Requirement: Opening a web address
In the web app, opening or refreshing an address, or typing one into the address bar while signed in, SHALL show the screen it names. An address that names no screen SHALL show the first screen of the person's view. An address of an admin screen opened by someone who is not an administrator SHALL show the first screen.

#### Scenario: Refresh
- **WHEN** a signed-in person refreshes the page on `#/groups/2/receiver`
- **THEN** that group's Receiver page is shown

#### Scenario: Typed address
- **WHEN** a signed-in person types `#/profile` into the address bar
- **THEN** the Profile tab is shown without reloading the page

#### Scenario: Sign in first
- **WHEN** a signed-out person opens `#/groups/2` and signs in
- **THEN** the screen of group 2 is shown

#### Scenario: Unknown address
- **WHEN** a signed-in person opens an address the app does not know, such as the former `#/groups/2/wishlist`
- **THEN** the first screen of their view is shown

#### Scenario: Admin address without admin rights
- **WHEN** a participant who is not an administrator opens `#/admin/groups/2`
- **THEN** the first screen is shown and not an admin screen

#### Scenario: Old address
- **WHEN** a person opens an address in the old form that contains the names of the app's classes
- **THEN** the first screen is shown

#### Scenario: Groups tab after refreshing another tab
- **WHEN** a signed-in person refreshes the page on `#/profile` or `#/settings` and chooses the Groups tab
- **THEN** the groups list is shown and its address is `#/groups`

#### Scenario: Other tabs after a refresh
- **WHEN** a signed-in person refreshes the page on `#/profile` and chooses the Settings tab, then the Profile tab, then the Groups tab
- **THEN** each tab is shown as it is when no refresh has happened

#### Scenario: The admin view after a refresh
- **WHEN** an administrator refreshes the page on `#/admin/users` and chooses the Groups tab
- **THEN** the administrator's groups are shown and its address is `#/admin/groups`

#### Scenario: Back after a refresh
- **WHEN** a signed-in person refreshes the page on `#/profile` and goes back
- **THEN** the Groups tab is shown first, as it is without a refresh

### Requirement: Changing your own password
Choosing "Change password" on the Settings page SHALL open a bottom sheet over the current screen, which stays as it was, asking for the current password, the new password and a repeat of it, with the same checks before sending and the same field-level server errors as the password-change form, and sending `PUT /me/password`.

#### Scenario: Open from either view
- **WHEN** a participant, or an administrator in the admin view, chooses "Change password" on the Settings page
- **THEN** the sheet opens over the screen they were on

#### Scenario: Change succeeds
- **WHEN** the person enters the right current password and a valid new one twice and saves
- **THEN** the sheet says the password was changed and that other devices are signed out, the screen behind is unchanged and the person is still signed in

#### Scenario: Checks before sending
- **WHEN** the new password is shorter than 8 characters, equals the current one, or does not match the repeat
- **THEN** the sheet says what is wrong next to the field and sends nothing

#### Scenario: Wrong current password
- **WHEN** the server refuses the change because the current password is wrong
- **THEN** the message appears next to the current password field and the sheet stays open with the new entries

#### Scenario: Server unreachable
- **WHEN** the request cannot reach the server
- **THEN** the sheet says so and keeps what was typed

#### Scenario: Closing discards the entries
- **WHEN** the person closes the sheet and opens it again
- **THEN** all three fields are empty and no earlier message is shown

### Requirement: Staying signed in on the web
The web sign-in screen SHALL show a "Stay signed in" checkbox, ticked by default; the Android and iOS sign-in screens SHALL NOT show it. Signing in with it ticked SHALL keep the session across closing and reopening the tab or the browser for as long as the server accepts the token; signing in with it unticked SHALL keep it only for the life of the tab.

#### Scenario: Ticked by default
- **WHEN** the web sign-in screen is shown
- **THEN** "Stay signed in" is ticked

#### Scenario: Not shown on phones
- **WHEN** the sign-in screen is shown on Android or iOS
- **THEN** there is no "Stay signed in" checkbox

#### Scenario: Reopening the tab
- **WHEN** a person signed in on the web with "Stay signed in" ticked closes the tab and the browser and opens the app again
- **THEN** the first view is shown without asking for the password, as long as the server still accepts the token

#### Scenario: Unticked
- **WHEN** a person signs in with "Stay signed in" unticked, refreshes and then closes the tab
- **THEN** the refresh keeps them signed in and a new tab shows the sign-in screen

#### Scenario: Signing out forgets it
- **WHEN** a person who stayed signed in signs out and opens the app again in a new tab
- **THEN** the sign-in screen is shown and no token is stored

#### Scenario: Expired token
- **WHEN** the token in local storage is no longer accepted by the server
- **THEN** the sign-in screen is shown and the token is removed

#### Scenario: Changing the password
- **WHEN** a person who stayed signed in changes their password
- **THEN** the new token replaces the old one in the same storage and the person is still signed in after closing and reopening the tab

## ADDED Requirements

### Requirement: The token is kept in one place only
The token SHALL NOT be written to logs, to plain preferences, or on the web to cookies, and SHALL be in one of the two web storages at a time.

#### Scenario: No cookie or log
- **WHEN** a person signs in on the web
- **THEN** the token is in no cookie and in no log, and in only one of local storage and session storage

### Requirement: The top bar switches views for administrators
The top bar SHALL have no menu: for administrators only it SHALL have one icon that switches to the other view, announced as "Switch to participant view" in the admin view and "Switch to admin view" in the participant view, and for everyone else no action.

#### Scenario: Icon announced
- **WHEN** an administrator in the admin view uses a screen reader on the top bar
- **THEN** the icon is announced as "Switch to participant view"

### Requirement: Settings are the same in both views
"Appearance", "Notifications", "Change password" and "Sign out" SHALL be on the Settings page, as `client-settings` describes.

#### Scenario: Settings in the admin view
- **WHEN** an administrator opens Settings in the admin view
- **THEN** the same four items are there as in the participant view

### Requirement: Administrators start in the admin view
An administrator SHALL start in the admin view, and the view they choose SHALL last for the current run only.

#### Scenario: Next run
- **WHEN** an administrator switched to the participant view and the app is started again
- **THEN** it opens in the admin view

### Requirement: Others see no admin view
A person who is not an administrator SHALL see no admin view and no item to reach it.

#### Scenario: No admin item
- **WHEN** a participant who is not an administrator looks at the top bar and Settings
- **THEN** nothing leads to the admin view

### Requirement: The admin status comes from the server
The admin status SHALL come from the server's `GET /me` and be refreshed whenever the app starts or the person signs in.

#### Scenario: Status refreshed at sign-in
- **WHEN** a person who has become an administrator signs in again
- **THEN** the admin view is offered

### Requirement: A questions tab has its own address
A page that opens on its questions tab SHALL have the address of the page followed by `/questions`.

#### Scenario: Questions suffix
- **WHEN** a person opens the Receiver page of group 2 on its questions tab
- **THEN** the address is `#/groups/2/receiver/questions`

### Requirement: Addresses are plain
The addresses SHALL NOT contain the names of the app's classes. Bottom sheets SHALL NOT be part of the address.

#### Scenario: No class names
- **WHEN** a person moves through the app
- **THEN** no address in the address bar contains the name of a class

### Requirement: An address survives signing in
When the person is signed out, the sign-in screen SHALL be shown and, after signing in, the screen the address names.

#### Scenario: Sign in to a page
- **WHEN** a signed-out person opens `#/groups/2/receiver` and signs in
- **THEN** the Receiver page of group 2 is shown

### Requirement: Tabs work after a refresh
After a refresh every tab of the view SHALL work as it does without one.

#### Scenario: Moving between tabs after a refresh
- **WHEN** a person refreshes the page on a group and chooses the Profile tab and then the Groups tab
- **THEN** each tab is shown as it is when no refresh has happened

### Requirement: After a password change
On success the app SHALL keep the person signed in with the new token the server returns, and the sheet SHALL say that the password was changed and that the person is signed out on their other devices, with a way to close it.

#### Scenario: Still signed in
- **WHEN** the change succeeds
- **THEN** the person is still signed in and closing the sheet shows the screen that was under it

### Requirement: Closing the password sheet discards what was typed
Closing the sheet at any time, before or after a change, SHALL discard everything typed in it.

#### Scenario: Reopening
- **WHEN** a person types into the sheet, closes it and opens it again
- **THEN** all three fields are empty

### Requirement: No stale token is left behind
Signing out, a token the server rejects and a password change that returns a new token SHALL leave no stale token in either storage, and the new token SHALL stay where the old one was.

#### Scenario: New token where the old one was
- **WHEN** a person who signed in with "Stay signed in" unticked changes their password
- **THEN** the new token is in the tab's session storage only

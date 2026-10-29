# client-app Specification

## Purpose

The app's shared foundation on Android, iOS and the web: the server address, signing in and out, keeping the session, the two views by role, navigation and web addresses, how text, sheets and forms behave, and changing your own password.

## Requirements

### Requirement: One shared interface on every platform
The app SHALL present the same screens and behavior on Android, iOS and web, built from one shared code base, with only the way the session is stored and the way the server address is found differing between platforms.

#### Scenario: Same screens everywhere
- **WHEN** the app is started on Android, on iOS or in a web browser
- **THEN** the person sees the same sign-in screen and, after signing in, the same views

### Requirement: Server address
The app SHALL talk to one server whose address it learns at start. On Android and iOS the address SHALL be the one built into the app. On web it SHALL be the `baseUrl` found in a `config.json` served next to the app, and the page's own origin when that file is missing or has no usable `baseUrl`.

#### Scenario: Web with a config file
- **WHEN** the web app starts and `config.json` contains `{"baseUrl": "https://api.example.org"}`
- **THEN** every request goes to `https://api.example.org`

#### Scenario: Web without a config file
- **WHEN** the web app starts and no `config.json` is served
- **THEN** every request goes to the page's own origin

#### Scenario: Unusable config
- **WHEN** `config.json` is not valid JSON or has no `baseUrl`
- **THEN** the app falls back to the page's own origin instead of failing to start

### Requirement: Sign in
The app SHALL show a sign-in screen when there is no session, asking for a username and a password, and SHALL sign the person in through `POST /auth/login`. The username SHALL be sent as typed, since the server ignores case. The sign-in action SHALL be unavailable while either field is blank or a request is in progress.

#### Scenario: Successful sign in
- **WHEN** a person enters a correct username and password
- **THEN** the app stores the session and shows the first view for that person

#### Scenario: Blank fields
- **WHEN** the username or the password is empty
- **THEN** the sign-in action cannot be used and no request is sent

#### Scenario: Double submit
- **WHEN** a sign-in request is in progress
- **THEN** the action cannot be used again until it finishes

### Requirement: Sign-in failures are explained
The app SHALL explain a failed sign-in without revealing more than the server does: a wrong username or password SHALL show one generic message; a locked username (HTTP 429) SHALL show that sign-in is paused and for about how long, from `Retry-After`; and an unreachable server or an unexpected response SHALL show a message that the server could not be reached or failed, without technical details.

#### Scenario: Wrong credentials
- **WHEN** the server answers 401 to a sign-in
- **THEN** the app shows one generic "wrong username or password" message and stays on the sign-in screen with the username kept and the password cleared

#### Scenario: Locked out
- **WHEN** the server answers 429 with `Retry-After`
- **THEN** the app tells the person that sign-in is paused and roughly how many minutes to wait

#### Scenario: Server unreachable
- **WHEN** the request cannot reach the server
- **THEN** the app shows that the server could not be reached and lets the person try again

### Requirement: Temporary passwords must be replaced
When the server reports that the person must change their password, either at sign-in (`mustChangePassword`) or later by answering a request with HTTP 403 and the code `password_change_required`, the app SHALL show a password-change screen and nothing else until the password has been changed through `PUT /me/password`.

#### Scenario: Temporary password at sign-in
- **WHEN** a person signs in with a temporary password
- **THEN** the password-change screen is shown instead of any view

#### Scenario: Gate hit later
- **WHEN** any request is answered with 403 `password_change_required`
- **THEN** the app shows the password-change screen

#### Scenario: Change succeeds
- **WHEN** the password has been changed
- **THEN** the old token is replaced by the new one the server returned and the first view is shown without signing in again

### Requirement: Password-change form
The password-change screen SHALL ask for the current password, the new password and a repeat of it, SHALL check before sending that the new password is at least 8 characters, differs from the current one and matches the repeat, and SHALL show the server's field-level errors when it refuses.

#### Scenario: Checks before sending
- **WHEN** the new password is shorter than 8 characters, equals the current one, or does not match the repeat
- **THEN** the app says what is wrong and sends nothing

#### Scenario: Server refuses
- **WHEN** the server refuses the change with a validation error naming a field
- **THEN** the app shows that message next to the matching field

### Requirement: Session storage per platform
On Android the app SHALL keep the access token in encrypted storage backed by the platform keystore, and on iOS in the Keychain, so that the person stays signed in after closing the app. On web the app SHALL keep the token in the browser's local storage when the person chose to stay signed in, so that it survives closing the tab and the browser, and otherwise in the browser tab's session storage, so that it survives a refresh of the page and is lost when the tab is closed. The token SHALL NOT be written to logs, to plain preferences, or on the web to cookies, and SHALL be in one of the two web storages at a time.

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

### Requirement: Rejected sessions return to sign-in
When the server answers any authenticated request with HTTP 401, the app SHALL discard the session and show the sign-in screen with a note that the person has been signed out. A stored token that the server rejects at start SHALL be discarded the same way.

#### Scenario: Token no longer valid
- **WHEN** a token has expired or was invalidated by a password change on another device
- **THEN** the next request that is answered with 401 removes the session and shows the sign-in screen with a note

#### Scenario: Stale stored token
- **WHEN** the app starts with a stored token and the server answers 401 to the check
- **THEN** the stored token is deleted and the sign-in screen is shown

### Requirement: Sign out
The app SHALL offer to sign out from every view after sign-in. Signing out SHALL delete the session from the platform storage and from memory and show the sign-in screen.

#### Scenario: Sign out
- **WHEN** a signed-in person chooses to sign out
- **THEN** the stored token is deleted and the sign-in screen is shown

#### Scenario: Next person
- **WHEN** another person signs in afterwards on the same device
- **THEN** nothing of the previous person's session or screens is shown

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab and a Profile tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups and a Users tab, under the same top bar. The top bar SHALL have a menu with "Appearance", "Change password", "Sign out" and, for administrators only, an item that switches to the other view. An administrator SHALL start in the admin view. A person who is not an administrator SHALL see no admin view and no item to reach it. The choice SHALL last for the current run only. The admin status SHALL come from the server's `GET /me` and be refreshed whenever the app starts or the person signs in.

#### Scenario: Administrator
- **WHEN** an administrator signs in
- **THEN** the admin view is shown and the top-bar menu switches to the participant view and back

#### Scenario: Normal participant
- **WHEN** a non-administrator signs in
- **THEN** only the participant view is shown and there is no way to reach the admin view

#### Scenario: The admin view has two tabs
- **WHEN** an administrator is in the admin view
- **THEN** a bottom bar offers Groups, which shows the administrator's own groups, and Users

#### Scenario: Switching views
- **WHEN** an administrator switches to the participant view and back
- **THEN** each view opens on its first screen

#### Scenario: The menu in both views
- **WHEN** the person opens the top-bar menu in the participant view or in the admin view
- **THEN** it offers "Appearance", "Change password" and "Sign out", and the view switch for an administrator

### Requirement: Requests carry the session and nothing else
Every request after sign-in SHALL carry the access token in the `Authorization: Bearer` header and no other credential. The app SHALL send the password only to `POST /auth/login` and `PUT /me/password`.

#### Scenario: Authenticated request
- **WHEN** the app calls any endpoint other than sign-in
- **THEN** the request carries the current access token

#### Scenario: Password never elsewhere
- **WHEN** the app calls any endpoint other than sign-in and the password change
- **THEN** the request contains no password

### Requirement: Local network permission on Android
On Android 17 or later, when the server address is a private-network address (loopback, `10.x`, `172.16.x` to `172.31.x`, `192.168.x`, link-local `169.254.x`, a `.local` name, or a name without a dot), the app SHALL check the local-network permission before sending any request and, when it is not granted, SHALL first explain why it is needed and then ask for it. Any other server address, any earlier Android version, iOS and the web SHALL never show this prompt.

#### Scenario: Private address without the permission
- **WHEN** the app starts on Android 17 with a server address such as `http://192.168.1.20:8080` and the permission is not granted
- **THEN** the app explains why it needs it and shows the system request before any request is sent

#### Scenario: Permission granted
- **WHEN** the person allows it
- **THEN** the app continues to the normal start, restoring the session or showing sign-in

#### Scenario: Already granted
- **WHEN** the permission is already granted
- **THEN** no prompt is shown

#### Scenario: Public server
- **WHEN** the server address is a public name such as `https://santa.example.org`
- **THEN** no prompt is shown and no permission is needed

#### Scenario: Older Android, iOS and web
- **WHEN** the app runs on Android 16 or earlier, on iOS or in a browser
- **THEN** this prompt is never shown

### Requirement: Local network permission denied
When the person denies the local-network permission, the app SHALL show a screen saying that the server cannot be reached without it, with an action to try again and, once the system will no longer show its request, an action that opens the app's settings.

#### Scenario: Denied
- **WHEN** the person denies the request
- **THEN** the app shows that the server cannot be reached without the permission, with "Try again"

#### Scenario: The system stops asking
- **WHEN** the system will no longer show the request after repeated denials
- **THEN** the screen offers "Open settings" so the permission can be granted there

#### Scenario: Granted later
- **WHEN** the person grants the permission in settings and returns to the app
- **THEN** the app continues to the normal start

### Requirement: Back navigation
Moving deeper into the app (for example from the groups list into a group) SHALL be undone by the platform's own way of going back: the system back button or gesture on Android, the edge-swipe gesture on iOS and the browser's back button on the web. Going back from the Groups list SHALL leave the app on Android and do nothing visible on iOS; going back from the Profile tab SHALL first return to the Groups tab.

#### Scenario: Back from a group
- **WHEN** the person opens a group and then goes back in the platform's way
- **THEN** the groups list is shown

#### Scenario: Browser history
- **WHEN** the person opens a group in the web app and presses the browser's back button
- **THEN** the groups list is shown, the page is not reloaded and the session is kept

#### Scenario: Back from the first screens
- **WHEN** the person goes back on the Groups list, or on the Profile tab
- **THEN** on Android the Groups list leaves the app and the Profile tab returns to the Groups tab first

### Requirement: Tabs keep their place
Top-level tabs SHALL keep their own place: switching tabs and returning SHALL show the screen the person had left. A different person signing in afterwards SHALL start on the first screen of their view, not where the previous person was.

#### Scenario: Tabs remember their place
- **WHEN** the person opens a group, switches to the Profile tab and returns to Groups
- **THEN** the group's detail screen is still shown

#### Scenario: Sign out clears navigation
- **WHEN** the person signs out and another person signs in
- **THEN** the new person starts on the first screen of their view, not where the previous person was

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

### Requirement: Forms stay usable with the keyboard open
A form that has a primary button (Profile, Change password, Sign in, and the group form) SHALL lift its content above the keyboard and SHALL scroll, so that the primary button and the field being edited can be reached while the keyboard is open.

#### Scenario: Save above the keyboard
- **WHEN** the person is typing in a field of the Profile tab
- **THEN** the Save button can be scrolled into view above the keyboard

#### Scenario: Small screen
- **WHEN** the Sign in form is taller than the space left by the keyboard
- **THEN** it scrolls and the Sign in button can be reached

### Requirement: Dragging dismisses the keyboard
A drag by the person that scrolls a form or a bottom sheet SHALL close the keyboard. Scrolling the app does itself to bring a field into view SHALL NOT close it, and a drag that only scrolls the text inside a multi-line field SHALL NOT close it.

#### Scenario: Drag a form
- **WHEN** the keyboard is open on the Profile tab and the person drags the form
- **THEN** the form scrolls and the keyboard closes

#### Scenario: Field brought into view
- **WHEN** the person taps a field that is partly under the keyboard and the form scrolls to show it
- **THEN** the keyboard stays open

#### Scenario: Drag inside a field
- **WHEN** the person drags inside a multi-line field that is scrolling its own text
- **THEN** the keyboard stays open

### Requirement: Typing in a sheet keeps the cursor
In a text field of a bottom sheet the cursor SHALL stay after the text just typed, on every platform, so that typing "abcd" gives "abcd".

#### Scenario: First character
- **WHEN** the person types "abcd" into an empty field of a bottom sheet
- **THEN** the field holds "abcd" with the cursor after the d

#### Scenario: Every sheet field
- **WHEN** the person types into any field of the wishlist item, question, answer or participants sheet
- **THEN** the cursor behaves the same in each

### Requirement: Pulling a screen refreshes it
On Android and iOS a screen that shows data loaded from the server SHALL reload when the person pulls its content down, SHALL show the standard pull indicator until the answer arrives, and SHALL NOT offer a Refresh button. A screen with little or nothing to show SHALL still respond to the pull. A reload SHALL keep what the screen shows while it runs and when it fails.

#### Scenario: Pull a list
- **WHEN** a person pulls the Groups list down on Android or iOS
- **THEN** the pull indicator is shown, the list reloads and the indicator goes away when the answer arrives

#### Scenario: Pull a short screen
- **WHEN** a person pulls down a screen that shows only "You have not added anything yet"
- **THEN** it reloads in the same way

#### Scenario: No button
- **WHEN** a screen that loads data is shown on Android or iOS
- **THEN** it has no Refresh button

#### Scenario: Pull fails
- **WHEN** a pull cannot reach the server
- **THEN** the pull indicator goes away, what was shown stays, and the message with "Try again" is shown

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible screen reloads quietly, with the thin progress bar and without the pull indicator

### Requirement: The web has no refresh control
On the web a screen SHALL offer no refresh control, because the browser's own refresh reloads the page and keeps the person signed in on the same screen.

#### Scenario: No button on the web
- **WHEN** a screen that loads data is shown in the web app
- **THEN** it has no Refresh button and no pull gesture

#### Scenario: Browser refresh
- **WHEN** a signed-in person refreshes the web page on a group's screen
- **THEN** the same screen is shown with current data and the person is still signed in

### Requirement: Readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/gifter` and `#/groups/2/receiver` for its Gifter and Receiver pages, `#/profile`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them. The addresses SHALL NOT contain the names of the app's classes. Bottom sheets SHALL NOT be part of the address.

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

### Requirement: Opening a web address
In the web app, opening or refreshing an address, or typing one into the address bar while signed in, SHALL show the screen it names. An address that names no screen SHALL show the first screen of the person's view. An address of an admin screen opened by someone who is not an administrator SHALL show the first screen. When the person is signed out, the sign-in screen SHALL be shown and, after signing in, the screen the address names.

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

### Requirement: Long read-only text is cut and opens in full
Read-only text that people wrote (a wishlist note and link, a question and its answer, a group's description, and a user's or receiver's profile fields) SHALL show at most three lines, with an ellipsis when there is more; a wishlist item's title SHALL show on one row, with an ellipsis when it is longer, wherever the item is shown. Choosing text that was cut SHALL open a dialog with its full text, which scrolls when it is long and can be closed. Text that is not cut SHALL do nothing when chosen.

#### Scenario: A long note is cut
- **WHEN** a wishlist item has a note of more than three lines
- **THEN** the list shows the first three lines and an ellipsis

#### Scenario: Open the full text
- **WHEN** the person chooses a note that was cut
- **THEN** a dialog shows the whole note and can be closed

#### Scenario: A short note
- **WHEN** the person chooses a note that fits in three lines
- **THEN** no dialog opens

#### Scenario: Own list
- **WHEN** the person chooses a cut note on their own wishlist
- **THEN** the full text opens in a dialog, and nothing else on the item opens it for editing except its edit button

#### Scenario: A long title
- **WHEN** a wishlist item has a title longer than one row
- **THEN** it shows on one row with an ellipsis, and choosing it opens a dialog with the whole title

### Requirement: Read-only text can be copied
Every read-only value of the kinds named in the previous requirement, and the username headings, a user's temporary password and the Santa reveal, SHALL end with a copy icon that puts the whole text, not only the part shown, on the clipboard. The icon SHALL change to a check mark for about a second afterwards, and SHALL show that the copy failed when the platform refuses it. Labels, screen titles, status words and buttons SHALL NOT have one.

#### Scenario: Copy an address
- **WHEN** the owner chooses the copy icon next to a user's address
- **THEN** the whole address is on the clipboard and the icon shows a check mark for a moment

#### Scenario: Copy a cut note
- **WHEN** the person chooses the copy icon of a note that is cut at three lines
- **THEN** the clipboard holds the whole note

#### Scenario: Copy in the full-text dialog
- **WHEN** the full-text dialog of a note is open
- **THEN** it has the same copy icon

#### Scenario: Copy refused
- **WHEN** the platform refuses access to the clipboard
- **THEN** the icon shows that the copy failed and nothing else changes

### Requirement: Multi-line fields stop growing at three rows
Every multi-line text field SHALL keep its minimum height while its text is short, grow with its text up to three rows and then stop growing and scroll inside itself.

#### Scenario: Short text
- **WHEN** a multi-line field holds one line
- **THEN** it keeps its minimum height

#### Scenario: Long text
- **WHEN** the text of a multi-line field is longer than three rows
- **THEN** the field is three rows tall and its text scrolls inside it

#### Scenario: Same everywhere
- **WHEN** the address, the delivery information, a wishlist note, a question, an answer and a group description are edited
- **THEN** each stops growing at three rows

### Requirement: Actions that add are floating buttons
Adding a wishlist item, asking a question and creating a group (the owner's "New group") SHALL each be a floating action button at the bottom end of its screen, showing an icon and offering the action's name to assistive technology, shown only while the action is available. When the action is not available the screen SHALL keep saying why in text, as before. The button SHALL NOT cover the last row of the content.

#### Scenario: Add item
- **WHEN** the person's wishlist can be changed and has room
- **THEN** a floating button with a plus icon opens the add sheet

#### Scenario: Not available
- **WHEN** the wishlist is full or closed
- **THEN** there is no button and the screen says so in text

#### Scenario: Last row
- **WHEN** a list is scrolled to its end
- **THEN** its last item is fully visible above the button

### Requirement: Changing your own password
Choosing "Change password" in the top-bar menu SHALL open a bottom sheet over the current screen, which stays as it was, asking for the current password, the new password and a repeat of it, with the same checks before sending and the same field-level server errors as the password-change form, and sending `PUT /me/password`. On success the app SHALL keep the person signed in with the new token the server returns, and the sheet SHALL say that the password was changed and that the person is signed out on their other devices, with a way to close it. Closing the sheet at any time, before or after a change, SHALL discard everything typed in it.

#### Scenario: Open from either view
- **WHEN** a participant, or an administrator in the admin view, chooses "Change password" in the menu
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
The web sign-in screen SHALL show a "Stay signed in" checkbox, ticked by default; the Android and iOS sign-in screens SHALL NOT show it. Signing in with it ticked SHALL keep the session across closing and reopening the tab or the browser for as long as the server accepts the token; signing in with it unticked SHALL keep it only for the life of the tab. Signing out, a token the server rejects and a password change that returns a new token SHALL leave no stale token in either storage, and the new token SHALL stay where the old one was.

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

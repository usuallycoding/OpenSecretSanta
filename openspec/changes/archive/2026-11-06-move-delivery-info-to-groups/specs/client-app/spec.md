# Spec Delta

## MODIFIED Requirements

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab and a Settings tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups, a Users tab and the same Settings tab, under the same top bar.

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

### Requirement: Back navigation
Moving deeper into the app (for example from the groups list into a group) SHALL be undone by the platform's own way of going back: the system back button or gesture on Android, the edge-swipe gesture on iOS and the browser's back button on the web. Going back from the Groups list SHALL leave the app on Android and do nothing visible on iOS; going back from the Settings tab SHALL first return to the Groups tab.

#### Scenario: Back from a group
- **WHEN** the person opens a group and then goes back in the platform's way
- **THEN** the groups list is shown

#### Scenario: Browser history
- **WHEN** the person opens a group in the web app and presses the browser's back button
- **THEN** the groups list is shown, the page is not reloaded and the session is kept

#### Scenario: Back from the first screens
- **WHEN** the person goes back on the Groups list, or on the Settings tab
- **THEN** on Android the Groups list leaves the app and the Settings tab returns to the Groups tab first

### Requirement: Tabs keep their place
Top-level tabs SHALL keep their own place: switching tabs and returning SHALL show the screen the person had left. A different person signing in afterwards SHALL start on the first screen of their view, not where the previous person was.

#### Scenario: Tabs remember their place
- **WHEN** the person opens a group, switches to the Settings tab and returns to Groups
- **THEN** the group's detail screen is still shown

#### Scenario: Sign out clears navigation
- **WHEN** the person signs out and another person signs in
- **THEN** the new person starts on the first screen of their view, not where the previous person was

### Requirement: Forms stay usable with the keyboard open
A form that has a primary button (Delivery info, Change password, Sign in, and the group form) SHALL lift its content above the keyboard and SHALL scroll, so that the primary button and the field being edited can be reached while the keyboard is open.

#### Scenario: Save above the keyboard
- **WHEN** the person is typing in a field of the Delivery Info page
- **THEN** the Save button can be scrolled into view above the keyboard

#### Scenario: Small screen
- **WHEN** the Sign in form is taller than the space left by the keyboard
- **THEN** it scrolls and the Sign in button can be reached

### Requirement: Dragging dismisses the keyboard
A drag by the person that scrolls a form or a bottom sheet SHALL close the keyboard. Scrolling the app does itself to bring a field into view SHALL NOT close it, and a drag that only scrolls the text inside a multi-line field SHALL NOT close it.

#### Scenario: Drag a form
- **WHEN** the keyboard is open on the Delivery Info page and the person drags the form
- **THEN** the form scrolls and the keyboard closes

#### Scenario: Field brought into view
- **WHEN** the person taps a field that is partly under the keyboard and the form scrolls to show it
- **THEN** the keyboard stays open

#### Scenario: Drag inside a field
- **WHEN** the person drags inside a multi-line field that is scrolling its own text
- **THEN** the keyboard stays open

### Requirement: Readable web addresses
In the web app each screen SHALL have a short, readable address after the `#` (for example `#/groups/2` for a group, `#/groups/2/gifter` and `#/groups/2/receiver` for its Gifter and Receiver pages, `#/groups/2/delivery-info`, `#/settings`, `#/admin/groups/2` and `#/admin/users/alice`), and the address bar SHALL follow the screens as the person moves between them.

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
- **WHEN** a signed-in person types `#/settings` into the address bar
- **THEN** the Settings tab is shown without reloading the page

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
- **WHEN** a signed-in person refreshes the page on `#/settings` and chooses the Groups tab
- **THEN** the groups list is shown and its address is `#/groups`

#### Scenario: Other tabs after a refresh
- **WHEN** a signed-in person refreshes the page on `#/settings` and chooses the Groups tab, then the Settings tab, then the Groups tab
- **THEN** each tab is shown as it is when no refresh has happened

#### Scenario: The admin view after a refresh
- **WHEN** an administrator refreshes the page on `#/admin/users` and chooses the Groups tab
- **THEN** the administrator's groups are shown and its address is `#/admin/groups`

#### Scenario: Back after a refresh
- **WHEN** a signed-in person refreshes the page on `#/settings` and goes back
- **THEN** the Groups tab is shown first, as it is without a refresh

### Requirement: Long read-only text is cut and opens in full
Read-only text that people wrote (a wishlist note and link, a question and its answer, a group's description, and a receiver's delivery details) SHALL show at most three lines, with an ellipsis when there is more; a wishlist item's title SHALL show on one row, with an ellipsis when it is longer, wherever the item is shown. Choosing text that was cut SHALL open a dialog with its full text, which scrolls when it is long and can be closed. Text that is not cut SHALL do nothing when chosen.

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

### Requirement: Tabs work after a refresh
After a refresh every tab of the view SHALL work as it does without one.

#### Scenario: Moving between tabs after a refresh
- **WHEN** a person refreshes the page on a group and chooses the Settings tab and then the Groups tab
- **THEN** each tab is shown as it is when no refresh has happened


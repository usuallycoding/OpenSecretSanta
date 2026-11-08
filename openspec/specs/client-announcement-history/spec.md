# client-announcement-history Specification

## Purpose
How the app shows the announcements a group's owner has sent: an "Group Notifications" icon in the group information card, and the page or pane it opens, newest first, for the participants of the group and for its owner.

## Requirements

### Requirement: The Group Notifications icon
The group information card SHALL have, at its end and centered vertically in it, an icon button with a bell icon and the tooltip "Group Notifications" (on hover, and on a long press on a touch screen), on the group page of a Pending, Active or Archived group and on the admin group screen of such a group. A Draft group SHALL show no icon. A screen reader SHALL say "Group Notifications". The icon SHALL NOT be shown while the group is not known.

#### Scenario: Participant
- **WHEN** a participant opens a Pending, an Active or an Archived group
- **THEN** the information card shows the bell icon at its end, vertically centered, and its tooltip reads "Group Notifications"

#### Scenario: The owner, not in the group
- **WHEN** the owner, who is not a participant, opens their Active group on the admin screen
- **THEN** the card shows the same icon

#### Scenario: A Draft group
- **WHEN** the owner opens a Draft group
- **THEN** the card shows no icon

### Requirement: The Group Notifications page
The Group Notifications page SHALL show the group's announcements as a lazy list with the last one sent first, each in a card with its title, its message with the line breaks, cut when long and opening in full, and the date and time it was sent as the app shows times everywhere. With none, it SHALL say there are no group notifications yet. It SHALL be titled "Group Notifications" and SHALL have nothing to edit or delete a message.

#### Scenario: Newest first
- **WHEN** the owner has sent three announcements, the last one yesterday
- **THEN** the list starts with the one sent yesterday and ends with the first one sent

#### Scenario: What a message shows
- **WHEN** an announcement with the title "Schedule change", a message of two lines and the time `2026-12-01T10:00:00Z` is shown
- **THEN** its card shows the title, both lines and `1 Dec 2026, 12:00`

#### Scenario: No messages
- **WHEN** the group has had no announcement
- **THEN** the page says there are no group notifications yet

#### Scenario: A long list
- **WHEN** the group holds 200 announcements
- **THEN** the list scrolls and only the cards on screen are drawn

#### Scenario: Nothing to change
- **WHEN** the owner reads a message on the page
- **THEN** the page has no button to edit or delete it

### Requirement: The Group Notifications page has a back icon in the top bar
On a small screen the Group Notifications page SHALL be titled "Group Notifications" in the top bar and the top bar SHALL show the back arrow on it, for the participant and for the owner, as it does on the other pages below a group. Choosing the arrow SHALL go back to the group's page.

#### Scenario: Back arrow
- **WHEN** a person opens the Group Notifications page on a phone
- **THEN** the top bar is titled "Group Notifications" and has the back arrow

#### Scenario: Going back
- **WHEN** the person chooses the arrow
- **THEN** the group's page is shown as it was

#### Scenario: The owner
- **WHEN** the owner opens the page from the admin overview on a phone
- **THEN** the top bar has the back arrow and going back shows the admin group screen

### Requirement: The Group Notifications page loads, fails and refreshes
The Group Notifications page SHALL load with `GET /groups/{id}/announcements` (for the owner, `GET /admin/groups/{id}/announcements`), SHALL reload by pulling it down, SHALL say when it cannot load, with "Try again", keeping what it showed, and SHALL say the group is no longer available, with a way back, when the server answers 404.

#### Scenario: Group gone
- **WHEN** the server answers 404 for the group
- **THEN** the page says the group is no longer available and offers to go back

#### Scenario: Reload
- **WHEN** the person pulls the page down
- **THEN** the announcements are loaded again

#### Scenario: Server unreachable
- **WHEN** the page cannot reach the server
- **THEN** it says so and offers "Try again", keeping the messages it already showed

### Requirement: A page on small screens, a pane on wide windows
On a small screen choosing the icon SHALL open the Group Notifications page as a new screen with Back. On a wide window it SHALL open the page in a pane to the right of the group page, which keeps its readable width, with a button that closes the pane; the address SHALL name the Group Notifications page while the pane is open and the group's own page once it is closed. Choosing the icon again SHALL NOT open a second pane.

#### Scenario: Small screen
- **WHEN** a person on a phone chooses the icon
- **THEN** the Group Notifications page opens, and Back shows the group page as it was

#### Scenario: Wide window
- **WHEN** a person on a wide window chooses the icon
- **THEN** the pane opens to the right of the group page, which keeps its readable width

#### Scenario: Closing the pane
- **WHEN** the person closes the pane, or goes back
- **THEN** the group page is shown alone, centered at its readable width

#### Scenario: The address follows the pane
- **WHEN** on the web a person on a wide window opens the pane of group 7 and then closes it
- **THEN** the address is `#/groups/7/notifications` while it is open and `#/groups/7` once it is closed, and for the owner `#/admin/groups/7/notifications` and `#/admin/groups/7`

#### Scenario: Choosing the icon again
- **WHEN** the pane is open and the person chooses the icon again
- **THEN** the pane stays as it is, and going back once closes it

#### Scenario: The owner on a wide window
- **WHEN** the owner chooses the icon on the admin group screen
- **THEN** the pane opens beside the screen, like the other panes of that screen

### Requirement: The Group Notifications page has web addresses
In the web app the Group Notifications page SHALL have the address `#/groups/{id}/notifications`, and the owner's `#/admin/groups/{id}/notifications`. On a wide window an address that names it SHALL show the group's page with the pane open, and the address SHALL stay.

#### Scenario: Address of the page
- **WHEN** a person opens `#/groups/7/notifications` on a phone
- **THEN** the Group Notifications page of group 7 opens, with group 7's page and then the group list behind it

#### Scenario: Address on a wide window
- **WHEN** a person opens the same address on a wide window
- **THEN** group 7's page is shown with the Group Notifications pane open, and the address is still `#/groups/7/notifications`

#### Scenario: Not an administrator
- **WHEN** a person who is not an administrator opens `#/admin/groups/7/notifications`
- **THEN** the address opens nothing, as other admin addresses do

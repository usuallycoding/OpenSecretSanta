# client-layout Specification

## Purpose
How the signed-in app lays itself out on wide screens, so that every screen, bar and button shares one standard width.

## Requirements

### Requirement: One standard maximum width
On a window narrower than the wide breakpoint, 840dp, the signed-in app SHALL keep its top bar, its content, its floating buttons and its bottom bar within one centered column of one standard width, the same on every screen, as it did before. On a window that is not wider than the standard width nothing SHALL change. The standard width and the wide breakpoint SHALL each be defined once for the whole app. Sheets and dialogs are not part of this requirement.

#### Scenario: Wide window
- **WHEN** the app is shown in a window much wider than the standard width but narrower than 840dp, such as a phone in landscape
- **THEN** the top bar, the content and the bottom bar are the same width and centered, and a floating button sits at the bottom end of that column, not at the window's edge

#### Scenario: Narrow window
- **WHEN** the app is shown on a phone held upright
- **THEN** the screens, bars and buttons fill the width as before

#### Scenario: Every screen
- **WHEN** the person moves between the lists, a group, the settings and the owner's screens on a window narrower than 840dp
- **THEN** all of them are the same width

### Requirement: Wide windows have a sidebar
On a window 840dp wide or wider the signed-in app SHALL replace its bottom bar with a sidebar at the start edge, shown on every screen of the view the person is in, and SHALL show the screen's content in the area beside it, under the top bar. The sidebar SHALL show which of its items the open screen belongs to. Content SHALL keep a readable width inside that area and SHALL NOT stretch to its edges.

#### Scenario: Wide window
- **WHEN** the app is shown in a window 840dp wide or wider
- **THEN** there is no bottom bar, a sidebar is at the start edge and the open screen is shown beside it

#### Scenario: Below the breakpoint
- **WHEN** the window is narrower than 840dp
- **THEN** the bottom bar is shown and there is no sidebar

#### Scenario: Window resized across the breakpoint
- **WHEN** a window is made wider or narrower across 840dp while a screen is open
- **THEN** the same screen stays open with its content, its scroll place and what was typed, now with the sidebar or the bottom bar

### Requirement: The participant sidebar
In the participant view the sidebar SHALL have a Groups item and a Settings item. Under Groups it SHALL list the person's groups one to a line, and the group that is open SHALL be expanded into Overview and, by what the group's state has to offer, Giving and Receiving. Choosing a group SHALL open its Overview, expand it and collapse the one that was expanded. The item of the open page SHALL be shown as selected. The Groups item SHALL stay in view and the groups under it SHALL scroll, so that the Settings item is always in view at the foot of the sidebar.

#### Scenario: Open group expands
- **WHEN** the person chooses a group in the sidebar
- **THEN** the group's Overview opens and the group is expanded under Groups into Overview, Giving and Receiving, while every other group stays one line

#### Scenario: Pending group
- **WHEN** the open group's names are not drawn yet
- **THEN** it is expanded into Overview and Receiving, and there is no Giving

#### Scenario: Selected item
- **WHEN** the person is on the Giving page
- **THEN** Giving is shown as selected, and its group is shown as expanded

#### Scenario: Many groups
- **WHEN** the person has more groups than fit the sidebar
- **THEN** the groups under the Groups item scroll, the Groups and Settings items stay in view, and the Groups screen keeps its search and filters

### Requirement: Badges in the sidebar
Giving SHALL carry a badge with how much is new to the person as a Santa, the new or edited wishlist items, new answers and new letters together, and Receiving a badge with how many of the questions they were asked wait for an answer, each counted as the group page's cards count them. Nothing SHALL be badged while a count is unknown or none.

#### Scenario: New things
- **WHEN** the receiver changed their wishlist and sent a letter since the person last looked
- **THEN** Giving carries a badge with the two, and its name for assistive technology says how many are new

#### Scenario: Questions waiting
- **WHEN** the person's Santa asked two questions that have no answer yet
- **THEN** Receiving carries a badge with 2

### Requirement: Giving and Receiving side by side
On a wide window the Giving item SHALL open one page that shows, side by side and each under its own heading, "<receiver username>'s Wishlist", "Questions for <receiver username>" and the Inbox, and the Receiving item one page that shows "Your Wishlist", "Respond to Secret Santa" and the Outbox. Each column SHALL scroll and reload by itself and SHALL keep a readable width, and when all of them cannot, the page SHALL scroll sideways. A Pending group's Receiving page SHALL show only the wishlist, at the standard width.

#### Scenario: Giving page
- **WHEN** the person chooses Giving for a group whose receiver is `alice` on a wide window
- **THEN** "alice's Wishlist", "Questions for alice" and the Inbox are shown beside each other, and the page is titled "Giving to alice"

#### Scenario: Receiving page
- **WHEN** the person chooses Receiving on a wide window
- **THEN** "Your Wishlist", "Respond to Secret Santa" and the Outbox are shown beside each other

#### Scenario: Not enough room
- **WHEN** the area beside the sidebar is narrower than three columns of 300dp
- **THEN** the columns keep their width and the page scrolls sideways

#### Scenario: Pending group
- **WHEN** the person chooses Receiving in a Pending group
- **THEN** only the wishlist is shown, centered at the standard width

#### Scenario: Buttons to open the Inbox and the Outbox
- **WHEN** the person chooses the Inbox or the "Send letter" button on a group's Overview on a wide window
- **THEN** the Giving or the Receiving page is shown

### Requirement: The admin sidebar
In the admin view the sidebar SHALL have a Groups item, with the administrator's own groups listed under it one to a line, a Users item and a Settings item, in that order, the Groups item and the Users and Settings items staying in view while the groups scroll. Choosing a group SHALL open it in place of the group that is shown, and the group that is open SHALL be shown as selected. The switch between the admin and participant views SHALL stay in the top bar.

#### Scenario: Administrator
- **WHEN** an administrator is in the admin view on a wide window
- **THEN** the sidebar offers Groups with their groups under it, Users and Settings, and the top bar has the switch icon

#### Scenario: Many groups
- **WHEN** the administrator has more groups than fit the sidebar
- **THEN** the groups scroll while Groups, Users and Settings stay in view

#### Scenario: Choosing a group
- **WHEN** the administrator chooses a group in the sidebar
- **THEN** that group's page opens and the group is shown as selected

### Requirement: Page actions on wide windows
On a window 840dp wide or wider the actions that are floating buttons on narrower windows SHALL be buttons with an icon and their name as text, and there SHALL be no floating button. The three columns of a Giving or Receiving page SHALL each have theirs at the end of the column's heading: "Ask a question" for "Questions for <receiver username>", "Add item" for "Your Wishlist" and "Send letter" for "Letters to your Santa". Creating a group SHALL be a button in the top bar. A button SHALL be shown only while its action is available, as the floating button is.

#### Scenario: Ask a question
- **WHEN** the person can ask a question, on the Giving page of a wide window
- **THEN** an "Ask a question" button is at the end of the heading "Questions for <receiver username>" and there is no floating button

#### Scenario: Add item
- **WHEN** the person's wishlist can be changed and has room, on a wide window
- **THEN** an "Add item" button is at the end of the heading "Your Wishlist", also on a Pending group's wishlist page

#### Scenario: Send letter
- **WHEN** the person can send a letter, on the Receiving page of a wide window
- **THEN** a "Send letter" button is at the end of the heading "Letters to your Santa"

#### Scenario: Not available
- **WHEN** the wishlist is full or closed, on a wide window
- **THEN** there is no button and the page says so in text

#### Scenario: New group
- **WHEN** an administrator is on the list of their groups on a wide window
- **THEN** a "New group" button is in the top bar

#### Scenario: Last row
- **WHEN** a list is scrolled to its end on a wide window
- **THEN** its last item is fully visible, with no room left for a floating button

### Requirement: Top bar titles are centered
The title in the top bar SHALL be centered on every screen, on narrow and on wide windows.

#### Scenario: Any screen
- **WHEN** the person opens any screen of the signed-in app
- **THEN** the top bar's title is centered between its start and end buttons

### Requirement: Sheets are dialogs on wide windows
On a window 840dp wide or wider every bottom sheet SHALL be shown as a centered dialog of a limited width, with room above its title, the same content, the same buttons and the same ways to close it, and on narrower windows it SHALL stay a bottom sheet.

#### Scenario: Wide window
- **WHEN** a sheet opens on a wide window
- **THEN** it is a centered dialog, not a sheet across the window's width

#### Scenario: Keyboard
- **WHEN** the keyboard is open for a field in such a dialog
- **THEN** the dialog still lets the person put the keyboard away without closing it, as a sheet does

### Requirement: Scrolling works beside a page
Scrolling in the empty space beside a page, by the mouse wheel, the trackpad or a touch drag, SHALL scroll the shown page in the same direction and by the same amount as over it. This applies wherever a page is narrower than its area: on a wide window for the pages that keep the standard width, and between the standard width and the wide breakpoint. Over the page, and for a page that fills the window or has nothing to scroll, nothing SHALL change.

#### Scenario: Wheel in the empty space
- **WHEN** the window is wide, a page is taller than the window and the pointer is in the empty space beside it
- **THEN** turning the mouse wheel scrolls the page

#### Scenario: Drag in the empty space
- **WHEN** the same page is shown on a touch screen and the person drags up in the empty space beside it
- **THEN** the page scrolls with the drag, and keeps moving when the finger is lifted as it does over the page

#### Scenario: Between the standard width and the wide breakpoint
- **WHEN** the window is wider than the standard width but narrower than 840dp, so that the bars and the page are a centered column with empty space on both sides
- **THEN** scrolling in the empty space beside the column scrolls the page

#### Scenario: Over the page
- **WHEN** the person scrolls over the page itself
- **THEN** it scrolls as it did, once, and not twice

#### Scenario: A page that fits
- **WHEN** the page is shorter than the window
- **THEN** scrolling beside it does nothing

#### Scenario: A page that fills the window
- **WHEN** a page uses the whole area, such as Giving and Receiving side by side
- **THEN** each part scrolls over itself as before

### Requirement: Announcement badges in the sidebar
In the participant sidebar each group's line SHALL carry a badge with the number of announcements the person has not seen, and, for the group that is open, its Overview item SHALL carry it instead of the group's line. The number SHALL be the one `GET /groups` gives, with "9+" above 9, and nothing SHALL be badged for none or while it is not known. A screen reader SHALL hear the count in the name, for example "3 new announcements". The Groups item SHALL NOT carry a total.

#### Scenario: A collapsed group
- **WHEN** a group that is not open has two announcements the person has not seen
- **THEN** its line in the sidebar carries a badge with 2

#### Scenario: The open group
- **WHEN** the open group has two announcements the person has not seen
- **THEN** its Overview item carries a badge with 2 and its own line carries none

#### Scenario: Nothing new
- **WHEN** a group has no new announcement
- **THEN** neither its line nor its Overview item is badged

#### Scenario: After reading
- **WHEN** the person reads the announcements in the pane and closes it
- **THEN** the badge is gone from the group's Overview item and from its line

#### Scenario: No total
- **WHEN** several groups have new announcements
- **THEN** the Groups item carries no badge

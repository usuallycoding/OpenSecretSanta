# client-admin-oversight Specification

## Purpose

What an owner reads in the app about a group they run: the draw as pairs, a participant's wishlist, user and pair sheets and the question threads, read-only and never recorded.

## Requirements

### Requirement: The draw
The detail of an Active or Archived group SHALL show one list of every Santa and receiver pair, loaded with `GET /admin/groups/{id}/assignments`, in the order the server returns them, each row showing the Santa's username, a pair link (the control described by `client-pair-link`) and the receiver's username, the Santa's username at the start of the row and the receiver's username at the end of the row.

#### Scenario: Pairs shown
- **WHEN** an Active group has `alice` giving to `bob` and `bob` giving to `carol`
- **THEN** one list shows `alice -> bob` and `bob -> carol` and no separate participants list

#### Scenario: Archived group
- **WHEN** a group is Archived
- **THEN** it shows the same pair list

#### Scenario: Owner's own pair
- **WHEN** the administrator is a participant in the group
- **THEN** their username is marked "you" in the pair where they are the Santa and in the pair where they are the receiver

#### Scenario: Incomplete profile
- **WHEN** `bob`'s delivery info has no address and he gives to `carol` and receives from `alice`
- **THEN** `bob` is followed by the pin icon in both rows and the legend explains it as "delivery info incomplete"

#### Scenario: Not yet drawn
- **WHEN** the group is Pending
- **THEN** there is no pair list and no assignments request is made

#### Scenario: Draw cannot be loaded
- **WHEN** the assignments request fails while the group detail is shown
- **THEN** the list area shows the problem with "Try again" and the rest of the detail stays

#### Scenario: Long list
- **WHEN** the draw has more pairs than fit on the screen
- **THEN** the page grows and scrolls as one, the buttons above the list scroll away with it, and no area scrolls inside the page

#### Scenario: Alignment
- **WHEN** an Active group has `alice` giving to `bob`
- **THEN** `alice` is at the start of the row and `bob` is at the end of the row, with the pair link between them

#### Scenario: Participant count in the label
- **WHEN** an Active group has four participants
- **THEN** the label above the pairs reads "Who gives to whom (4 participants)"

### Requirement: Participant wishlist
Choosing the gift icon at the end of a participant's row in a Pending group SHALL open that person's wishlist for the group, loaded with `GET /admin/groups/{id}/participants/{username}/wishlist`, read-only and in the order the participant chose, showing for each item its title and, when set, its note and every link, and when it was added or last changed. On a narrow window it SHALL open in a bottom sheet and on a wide window in a pane beside the group page, which stays in place.

#### Scenario: List shown in order
- **WHEN** a Pending group's participant has three items in a chosen order
- **THEN** the sheet or pane shows the three in that order

#### Scenario: Empty list
- **WHEN** the participant has no items
- **THEN** the sheet or pane says the list is empty

#### Scenario: Read-only
- **WHEN** the wishlist is shown
- **THEN** there is no control that adds, edits, deletes or moves an item

#### Scenario: Link opens
- **WHEN** the owner chooses an item's link
- **THEN** the link opens in the browser at once, as it does in the person's own list

#### Scenario: Several links
- **WHEN** an item has three links
- **THEN** the sheet or pane shows the three one under another in their order, each opening in the browser

#### Scenario: Pane beside the page
- **WHEN** the owner chooses the gift icon of a participant's row on a wide window
- **THEN** the wishlist opens in a pane beside the group page, which keeps its scroll place and can still be used

#### Scenario: Another person's wishlist
- **WHEN** the pane is open and the owner chooses another participant's gift icon
- **THEN** the pane shows that person's wishlist

#### Scenario: Closing the pane
- **WHEN** the owner closes the pane, or goes back
- **THEN** the pane closes and the group page stays open

#### Scenario: Window made narrow
- **WHEN** a window with the pane open is made narrower than 840dp
- **THEN** the same wishlist is shown in a bottom sheet

### Requirement: User sheet
Choosing either username in a row of the pair list, or the username of a participant with an account in a Pending group, SHALL open a bottom sheet with that person's details, loaded with `GET /admin/users/{username}` and shown as the user detail of `client-admin-users`: the username and the temporary password while it is still temporary.

#### Scenario: Open from the Santa
- **WHEN** the owner chooses `alice` in the row `alice -> bob`
- **THEN** a sheet shows `alice`'s details

#### Scenario: Open from the receiver
- **WHEN** the owner chooses `bob` in the row `alice -> bob`
- **THEN** a sheet shows `bob`'s details

#### Scenario: Reset from the sheet
- **WHEN** the owner confirms "Reset password" for `alice` in her sheet
- **THEN** a dialog shows the new temporary password once and the sheet then shows `alice` as having to change it

#### Scenario: Own account
- **WHEN** the owner opens the sheet of their own username
- **THEN** there is no "Reset password"

#### Scenario: Person no longer available
- **WHEN** the server answers that the user does not exist for this administrator
- **THEN** the sheet says the user is no longer available and offers to close

#### Scenario: Open from a Pending participant
- **WHEN** the owner chooses the username of a participant with an account in a Pending group
- **THEN** a sheet shows that person's details

### Requirement: Pair sheet
Choosing the pair link in a row of the pair list SHALL open the pair, titled with the pair, showing the receiver's wishlist, loaded with `GET /admin/groups/{id}/participants/{receiver}/wishlist` and shown as in "Participant wishlist", and below it the thread of questions the Santa asked the receiver. On a narrow window it SHALL be a page with a back action and on a wide window a pane beside the group page, as the rules are, each at the pair's web address. It SHALL offer no way to ask, answer or change anything.

#### Scenario: Wishlist and thread shown
- **WHEN** the owner chooses the pair link in the row of `alice` and `bob`
- **THEN** the page or pane shows `bob`'s wishlist and the questions `alice` asked `bob` with his answers

#### Scenario: Read-only
- **WHEN** the pair page or pane is shown, whatever the group's state
- **THEN** there is no control that asks, answers or changes a question or a wishlist item

#### Scenario: Pane beside the page
- **WHEN** the owner chooses a pair link on a wide window
- **THEN** the pair opens in a pane beside the group page, and choosing another pair shows that pair in the pane

#### Scenario: Closing the pane
- **WHEN** the owner closes the pane, or goes back
- **THEN** the pane closes and the group page stays open

#### Scenario: A page on a narrow window
- **WHEN** the owner chooses a pair link on a phone-width window
- **THEN** the pair opens as a page titled with the pair, and back returns to the group page

#### Scenario: Window resized
- **WHEN** a pair is open and the window is made wider or narrower than 840dp
- **THEN** the pair is shown as a pane or as a page, never in a bottom sheet

### Requirement: Pair thread
The thread in the pair sheet SHALL be taken from `GET /admin/groups/{id}/questions`, only the thread of the chosen Santa and receiver, and SHALL list every question in order with its number, text and time asked and, when answered, the answer with when it was first given and when it was last changed, or "Not answered yet", and a note when nothing has been asked.

#### Scenario: Unanswered question
- **WHEN** a question has no answer
- **THEN** it shows "Not answered yet"

#### Scenario: Changed answer
- **WHEN** an answer was changed after it was first given
- **THEN** both the first and the last-changed times are shown

#### Scenario: Nothing asked
- **WHEN** the Santa has asked nothing yet
- **THEN** the thread part says nothing has been asked

#### Scenario: Only that pair
- **WHEN** the group has threads for four pairs
- **THEN** the sheet shows only the thread of the chosen pair

### Requirement: Reading is not recorded
The oversight sheets and the pair list SHALL only read from the server and SHALL keep nothing of what they show once they are closed.

#### Scenario: Only reads
- **WHEN** the owner opens the pair list, a pair sheet and a wishlist sheet
- **THEN** the app sends only GET requests for them

### Requirement: Oversight errors and refreshing
Each oversight sheet SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached, SHALL keep what it shows while a refresh fails and SHALL reload each time it is opened. When the server answers 404 the sheet SHALL say the group or person is no longer available and offer to close; when it answers 409 `invalid_state` it SHALL say the group is not in a state where this can be read and offer to close.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while a pair sheet loads
- **THEN** the sheet says so and offers "Try again"

#### Scenario: Participant removed
- **WHEN** the server answers 404 for a participant's wishlist
- **THEN** the sheet says the person is no longer available and offers to close

#### Scenario: State changed
- **WHEN** the server answers 409 `invalid_state` because the group is no longer in a readable state
- **THEN** the sheet says so and offers to close

#### Scenario: Reopened
- **WHEN** a sheet is closed and opened again
- **THEN** it loads again and shows what the server now says

### Requirement: Pair thread uses the same layout
In the pair thread each question SHALL be in a card, like the wishlist items in the same sheet, and SHALL show its number in a small filled circle, and each answer SHALL be indented under its question and joined to the circle by a line, and an unanswered question SHALL show the line with "Not answered yet" in a muted colour, as the Gifter and Receiver pages show them.

#### Scenario: Answered question in the thread
- **WHEN** the thread has a question with an answer
- **THEN** the question's number is in a circle and the answer is indented under the question and joined to the circle by a line

#### Scenario: Unanswered question in the thread
- **WHEN** the thread has a question with no answer
- **THEN** it shows the line and "Not answered yet" in a muted colour

#### Scenario: Questions are cards
- **WHEN** the pair sheet shows the wishlist and the thread
- **THEN** each question is in a card that looks like a wishlist item's card, with the same padding and the same space between cards

### Requirement: The pair list replaces the participants list
The pair list SHALL replace the participants list and any separate draw section, and SHALL be part of the page, which scrolls as one as described by `client-admin-groups`, with its legend under the list.

#### Scenario: One list on the page
- **WHEN** an Active group's detail is shown
- **THEN** there is no participants list and no separate draw section, and the legend is under the pair list

### Requirement: The pair list label counts the participants
The label above the pair list SHALL read "Who gives to whom" followed by the number of participants in the group in brackets, as in "Who gives to whom (4 participants)", and the number SHALL NOT change while a search narrows the list.

#### Scenario: Count while searching
- **WHEN** a search narrows the pair list to one pair in a group of four participants
- **THEN** the label still reads "Who gives to whom (4 participants)"

### Requirement: Marks on usernames in the oversight lists
The administrator's own username SHALL be marked "you" wherever it appears. A username with no account SHALL be followed by `*`; one whose account is still on a temporary password by a key icon; one whose delivery info lacks a full name, email, phone or address by a pin icon. Each icon SHALL be named for assistive technology, and a legend under the list SHALL give each mark shown and its meaning, only those present.

#### Scenario: Both marks
- **WHEN** the administrator takes part in the group and their own delivery info is incomplete
- **THEN** their username is marked "you" and followed by the pin icon wherever it appears

#### Scenario: Temporary password
- **WHEN** `bob`'s account is still on a temporary password in a Pending, an Active or an Archived group
- **THEN** `bob` is followed by the key icon wherever he appears and the legend says "temporary password"

#### Scenario: Both icons
- **WHEN** `bob` has a temporary password and incomplete delivery info
- **THEN** `bob` is followed by the key icon and the pin icon, and the legend explains both

#### Scenario: No account
- **WHEN** a Pending group has a participant with no account
- **THEN** that participant is followed by `*` only, with no key and no pin, and the legend says "no account"

#### Scenario: Draft group
- **WHEN** the group is a Draft
- **THEN** no key or pin icon is shown, because no account exists for the group yet and nobody can fill delivery info in

#### Scenario: A receiver's marks on the left
- **WHEN** a pair row or a pair sheet title shows the receiver, who has a temporary password and incomplete delivery info
- **THEN** the receiver's icons are on the left of the username, as in "pin key bob", while the Santa's stay on the right of the username

#### Scenario: Spoken
- **WHEN** a screen reader reads a username followed by the key icon
- **THEN** it reads that the account is on a temporary password

### Requirement: No pair list before the draw
A group that is Draft or Pending SHALL show no pair list, and the app SHALL NOT request it.

#### Scenario: Draft group
- **WHEN** a Draft group's detail is shown
- **THEN** there is no pair list and no assignments request is made

### Requirement: The wishlist sheet is read-only
There SHALL be no wishlist screen for a participant's wishlist, and the sheet SHALL offer no way to add, change, delete or reorder items.

#### Scenario: A sheet, not a screen
- **WHEN** the owner chooses a participant's gift icon
- **THEN** a bottom sheet opens over the group's detail and no wishlist screen is shown

### Requirement: The user sheet resets passwords
The user sheet SHALL offer "Reset password" with the same confirmation and one-time password dialog as the Users tab, and SHALL offer none for the administrator's own username.

#### Scenario: Cancelling the confirmation
- **WHEN** the owner chooses "Reset password" in a sheet and cancels the confirmation
- **THEN** the password is unchanged and no new password is shown

### Requirement: The user sheet shows the group's delivery info
Below the account part, the user sheet SHALL show the person's delivery info for the group, loaded with `GET /admin/groups/{id}/participants/{username}/delivery-info`: name, email, phone, address and delivery information, leaving out what is not set, and saying the person has not added delivery info yet when none is set. It SHALL be read-only, and SHALL load and fail apart from the account part.

#### Scenario: Delivery info shown
- **WHEN** the owner opens the sheet of `bob`, who set a full name, an email and an address in the group
- **THEN** below his username the sheet shows his name, email and address, and no phone or delivery information

#### Scenario: Nothing set
- **WHEN** the person has set no delivery info in the group
- **THEN** the sheet says the person has not added delivery info yet

#### Scenario: Account part unaffected
- **WHEN** the sheet is shown
- **THEN** it also shows the username, the temporary password while temporary and "Reset password" as before

#### Scenario: Failure of one part
- **WHEN** the delivery info cannot be loaded but the account loaded, or the account cannot be loaded but the delivery info did
- **THEN** the part that loaded stays shown and the other says what went wrong with "Try again"

#### Scenario: Person or group not readable
- **WHEN** the server answers 404 for the delivery info, or 409 `invalid_state` because the group is no longer readable
- **THEN** that part says the person is no longer available, or that the group is not in a state where this can be read, as in "Oversight errors and refreshing"

#### Scenario: Read-only
- **WHEN** the sheet is shown, whatever the group's state
- **THEN** there is no control that changes the person's delivery info

#### Scenario: Reloaded
- **WHEN** the sheet is closed and opened again
- **THEN** both parts load again and show what the server now says

### Requirement: The marks are counted
Above the list the group page SHALL say how many participants are on a temporary password and how many have incomplete delivery info, such as "2 on a temporary password, 3 with delivery info incomplete", leaving out a count of 0 and showing nothing when both are 0, in the same states as the icons. It SHALL be plain text with no filter and no tap.

#### Scenario: Both counts
- **WHEN** a Pending group has two participants on a temporary password and three with incomplete delivery info
- **THEN** the line reads "2 on a temporary password, 3 with delivery info incomplete"

#### Scenario: One count
- **WHEN** nobody has incomplete delivery info and one person is on a temporary password
- **THEN** the line reads "1 on a temporary password"

#### Scenario: Nothing to count
- **WHEN** nobody is on a temporary password and nobody has incomplete delivery info, or the group is a Draft
- **THEN** no line is shown

#### Scenario: Not a control
- **WHEN** the person taps the line
- **THEN** nothing happens and the list is not filtered

### Requirement: The user sheet names the missing delivery info
When the person has set something for the group but not all of the full name, email, phone and address, the delivery info part of the user sheet SHALL show a line "Missing: " naming the ones not set, as in "Missing: email, phone", besides what is set. When all four are set there SHALL be no such line, and when nothing is set the sheet SHALL only say the person has not added delivery info yet.

#### Scenario: Some missing
- **WHEN** `bob` set a full name and an address but no email or phone
- **THEN** the sheet shows his name and address and the line "Missing: email, phone"

#### Scenario: Only delivery information missing
- **WHEN** `bob` set the four required fields and no delivery information
- **THEN** the sheet shows no "Missing" line

#### Scenario: Only delivery information set
- **WHEN** `bob` set only the delivery information
- **THEN** the sheet shows it and the line "Missing: full name, email, phone, address"

#### Scenario: Nothing set
- **WHEN** `bob` set nothing for the group
- **THEN** the sheet only says he has not added delivery info yet

### Requirement: The pair sheet shows the letters
Below the thread of questions the pair sheet SHALL show the letters the pair's receiver has sent their Santa, taken from `GET /admin/groups/{id}/letters` for that pair only, newest first, each in a card with its title, its text and when it was sent, under the heading "Letters from" and the receiver's username. With none it SHALL say none have been sent. It SHALL load, fail and retry on its own, apart from the other parts, and SHALL offer no way to send, change, delete or reply.

#### Scenario: Letters shown
- **WHEN** the owner opens the pair sheet of `alice` and `bob` and `bob` has sent two letters
- **THEN** below the questions the sheet shows both letters, the newer first, under "Letters from bob"

#### Scenario: Only that pair
- **WHEN** the group has letters from four receivers
- **THEN** the sheet shows only the letters of the chosen pair's receiver

#### Scenario: None sent
- **WHEN** the receiver has sent no letter
- **THEN** the letters part says none have been sent

#### Scenario: Failure of one part
- **WHEN** the letters cannot be loaded but the wishlist and the questions did
- **THEN** the other parts stay shown and the letters part says what went wrong with "Try again"

#### Scenario: Read-only
- **WHEN** the pair sheet is shown, whatever the group's state
- **THEN** there is no control that sends, changes, deletes or replies to a letter

### Requirement: What was said of the pair sheet carries over
Wherever another requirement speaks of the pair sheet, it SHALL apply to the page or the pane that shows the pair, with the same title, content and marks.

#### Scenario: The thread and the letters
- **WHEN** another requirement says the pair sheet shows the thread of questions and the letters
- **THEN** the pair page and the pair pane show them

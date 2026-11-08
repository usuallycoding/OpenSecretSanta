# client-admin-groups Specification

## Purpose

How an administrator manages their own groups in the app: listing, creating, editing, duplicating, deleting and moving a group through its states, with its participants, the draw, the temporary passwords it creates and who was emailed.

## Requirements

### Requirement: Admin groups list
The admin view SHALL list the signed-in administrator's own groups, loaded with `GET /admin/groups`, each with its name on one row (with an ellipsis when it is longer, not opening anything by itself), state, date and time, location and number of participants, in the order the server returns them, with a "New group" floating button and, on every group, a "Duplicate" button. When there are none it SHALL say so and offer to create one. A group owned by another administrator SHALL never appear.

#### Scenario: Groups shown
- **WHEN** the administrator owns two groups
- **THEN** both are listed with their name, state and number of participants

#### Scenario: No groups yet
- **WHEN** the administrator owns no group
- **THEN** the screen says so and offers "New group"

#### Scenario: Open a group
- **WHEN** the administrator chooses a group
- **THEN** its detail opens, and going back returns to the list

#### Scenario: New group button
- **WHEN** the administrator is on the groups list
- **THEN** a floating button with a plus icon opens the form for a new group

#### Scenario: Duplicate button on every group
- **WHEN** the list shows a Draft, a Pending, an Active and an Archived group
- **THEN** each has a "Duplicate" button, and choosing it does not open the group

### Requirement: Creating a group
"New group" SHALL open a form with name, description, location, budget, currency, date and time. A successful `POST /admin/groups` SHALL open the new group, which is a Draft. A field the server refuses SHALL show its message next to the field.

#### Scenario: Create a group
- **WHEN** the administrator enters a name and saves
- **THEN** the group is created as a Draft and its detail opens

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while saving
- **THEN** the form keeps what was typed and says so

### Requirement: The group form checks what the server checks
Before anything is sent the form SHALL check every field exactly as the server does: a name of 1 to 100 characters, a description of up to 2000, a location of up to 200, a budget that is a whole number from 50 to 10000, a currency of up to 20, a real calendar date and a real time of day, with blanks taken as empty. A failed check SHALL show its message next to the field.

#### Scenario: Name required
- **WHEN** the administrator saves with a blank name
- **THEN** nothing is sent and the form says the name is required

#### Scenario: Invalid date or time
- **WHEN** the form holds a date or time that is not real, such as `2026-13-45` or `25:99`, which no picker can produce
- **THEN** nothing is sent and the form says what format is expected next to that field

#### Scenario: Blank means empty
- **WHEN** the administrator leaves description, location, budget, date and time blank
- **THEN** the group is saved without them

#### Scenario: Budget out of range
- **WHEN** the form holds a budget of 49, of 10001 or of `abc`
- **THEN** nothing is sent and the form says next to the budget field that it must be a whole number from 50 to 10000

### Requirement: Date and time are chosen with pickers
The group form's Date and Time fields SHALL be read-only and open a date picker or a time picker when chosen, and SHALL show the chosen value as `2026-12-24` and `18:30`. A field with a value SHALL show a clear control that empties it. The time picker SHALL be 24-hour on every device. The date and time SHALL be local values: the date chosen is the date saved in every timezone.

#### Scenario: Pick a date
- **WHEN** the administrator chooses the Date field, picks 24 December 2026 and confirms
- **THEN** the field shows `2026-12-24` and saving sends that date

#### Scenario: Pick a time
- **WHEN** the administrator chooses the Time field, picks 18:30 and confirms
- **THEN** the field shows `18:30`, and the picker showed hours 0 to 23

#### Scenario: Clear a date or time
- **WHEN** a field has a value and the administrator chooses its clear control
- **THEN** the field is empty and saving sends the group without that date or time

#### Scenario: Cancelled picker
- **WHEN** the administrator opens a picker and cancels
- **THEN** the field keeps the value it had, and stays empty if it was empty

#### Scenario: No typing
- **WHEN** the administrator uses the Date or Time field
- **THEN** no text can be typed into it and the date picker has no typed-entry mode

#### Scenario: Same date in any timezone
- **WHEN** the administrator picks 24 December 2026 on a device in any timezone
- **THEN** the field shows `2026-12-24` and the saved date is `2026-12-24`

### Requirement: Where the pickers start
When the field is empty, the date picker SHALL start on Christmas Day, 25 December of the current year or of the next year once that day has passed, and the time picker SHALL start on 00:00; neither SHALL fill the field until the administrator confirms. When the field has a value, its picker SHALL start on that value.

#### Scenario: Empty date
- **WHEN** the Date field is empty and it is 3 November 2026
- **THEN** the date picker opens on 25 December 2026 and the field stays empty until confirmed

#### Scenario: After Christmas
- **WHEN** the Date field is empty and it is 26 December 2026
- **THEN** the date picker opens on 25 December 2027

#### Scenario: Empty time
- **WHEN** the Time field is empty
- **THEN** the time picker opens on 00:00 and the field stays empty until confirmed

#### Scenario: Existing value
- **WHEN** the administrator edits a group whose date is `2026-12-24` and time is `18:30` and opens each picker
- **THEN** the date picker starts on 24 December 2026 and the time picker on 18:30

### Requirement: Editing a group's details
While a group is Draft, Pending or Active the administrator SHALL be able to open the same form filled with its details and save changes with `PUT /admin/groups/{id}`, after which the detail shows the new values. An Archived group SHALL show no edit action.

#### Scenario: Edit the location
- **WHEN** the administrator changes the location of an Active group and saves
- **THEN** the detail shows the new location

#### Scenario: Clear a field
- **WHEN** the administrator empties the description and saves
- **THEN** the group has no description

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** there is no way to edit its details

### Requirement: Group detail shows who takes part
Choosing a group SHALL open its detail, loaded with `GET /admin/groups/{id}`, showing its name on one row with an ellipsis when it is longer (choosing a name that was cut opens a dialog with the whole name), its details, its state with a sentence on what that state means for the owner, the actions of the group, and at the end who takes part.

#### Scenario: Group not available
- **WHEN** the server answers that the group does not exist for this administrator
- **THEN** the screen says the group is no longer available and offers to go back

#### Scenario: Pending group
- **WHEN** a Pending group's detail is shown
- **THEN** it lists the participants as rows, each username opens the user sheet and each row ends with a gift icon that opens the wishlist

#### Scenario: Draft offers no oversight
- **WHEN** the group is Draft
- **THEN** it lists the participants as chips with no gift icon and no pair list, and choosing a chip does nothing

#### Scenario: Drawn group
- **WHEN** the group is Active or Archived
- **THEN** the detail shows the pair list and no separate participants list, and no Questions button

#### Scenario: Many participants
- **WHEN** a Pending group has 13 participants
- **THEN** the page grows and scrolls as one, the buttons above the list scroll away with it, and no area scrolls inside the page

#### Scenario: A long name
- **WHEN** the group's name is longer than one row and the administrator chooses it
- **THEN** a dialog shows the whole name and can be closed

#### Scenario: Participant without an account in a Pending group
- **WHEN** a Pending group has a participant who has no account
- **THEN** their row shows the username and `*`, and choosing the username or the gift icon does nothing

### Requirement: Marks on participants
In the participants list of a Draft or Pending group a participant without an account SHALL be followed by a single `*`. In a Pending group a participant with an account SHALL also be followed by a key icon while still on a temporary password and by a pin icon while their delivery info lacks a full name, email, phone or address, as `client-admin-oversight` describes. The list SHALL end with a legend of the marks shown, only when at least one is.

#### Scenario: Participants with marks
- **WHEN** a Draft group has `alice` with an account and `newcomer` without one
- **THEN** `alice` has no mark, `newcomer` is followed by `*` and the legend "* no account" is shown

#### Scenario: Incomplete profile
- **WHEN** a Pending group has a participant with an account whose delivery info has no address
- **THEN** that participant is followed by the pin icon, not by `*`, and the legend explains the icon

#### Scenario: Incomplete profile after the draw
- **WHEN** an Active group has a participant whose delivery info has no address
- **THEN** that username is followed by the pin icon in the pair list

#### Scenario: Nobody marked
- **WHEN** every participant has an account, has changed their temporary password and, in a Pending group, has complete delivery info
- **THEN** no mark and no legend are shown

#### Scenario: Temporary password
- **WHEN** a Pending group has a participant who has not changed their temporary password
- **THEN** that participant is followed by the key icon and the legend explains it

### Requirement: Editing participants
While a group is Draft or Pending the administrator SHALL be able to edit the participants in a bottom sheet: add a username, remove a username, then save with `PUT /admin/groups/{id}/participants`, which replaces the whole list. A username is checked before sending as the server checks it (3 to 32 characters of letters, digits, `.`, `_` or `-`, case ignored) and a name already in the list is not added twice. Once the group is Active or Archived the participants SHALL be shown read-only.

#### Scenario: Add a participant
- **WHEN** the administrator adds `dave` in a Draft group and saves
- **THEN** `dave` appears among the participants

#### Scenario: Remove a participant
- **WHEN** the administrator removes `bob` and saves
- **THEN** `bob` is no longer listed

#### Scenario: Invalid or repeated username
- **WHEN** the administrator adds `x` or `Alice` while `alice` is already listed
- **THEN** the sheet says what is wrong and the list is unchanged

#### Scenario: Roster frozen
- **WHEN** the group is Active or Archived
- **THEN** there is no way to add or remove a participant

#### Scenario: Roster frozen elsewhere
- **WHEN** the server refuses a save because the roster is frozen
- **THEN** the sheet closes, the detail reloads and says the participants can no longer change

### Requirement: Moving a group forward
The detail SHALL offer the next step for the state: "Make pending" for a Draft (`POST /admin/groups/{id}/pending`), "Draw the names" for a Pending group (`POST /admin/groups/{id}/activate`) and "Archive" for an Active one (`POST /admin/groups/{id}/archive`), each after a confirmation that says what it does. The step SHALL be a button, last in the row of actions above the people, after the icon buttons; the row SHALL wrap onto a second line when it does not fit. An Archived group has no step.

#### Scenario: Make pending
- **WHEN** the administrator confirms "Make pending" on a Draft
- **THEN** the group shows as Pending

#### Scenario: Draw the names
- **WHEN** the administrator confirms "Draw the names" on a Pending group with 4 or more participants
- **THEN** the group shows as Active and its participants are frozen

#### Scenario: Archive
- **WHEN** the administrator confirms "Archive" on an Active group
- **THEN** the group shows as Archived and no action remains except reading and deleting

#### Scenario: Cancel a confirmation
- **WHEN** the administrator cancels any confirmation
- **THEN** nothing is sent

#### Scenario: Changed behind the screen
- **WHEN** the server refuses a step because the group is no longer in the state the screen showed
- **THEN** the detail reloads and says what happened

#### Scenario: Buttons together
- **WHEN** the detail of a Pending group is shown
- **THEN** the pencil, the bell, the key, the trash can and "Draw the names", in that order, are in one row above the participants

### Requirement: Confirmations say what a step does
The confirmation for "Make pending" SHALL say that accounts are created for people who have none, the one for "Draw the names" that the names are drawn and the participants frozen, and the one for "Archive" that the exchange is finished and becomes read-only.

#### Scenario: Wording
- **WHEN** the administrator chooses "Draw the names"
- **THEN** the confirmation says the names will be drawn and the participants can no longer change

### Requirement: Drawing the names needs four participants
"Draw the names" SHALL be unavailable while a Pending group has fewer than 4 participants and SHALL say that at least 4 are needed, instead of sending a request the server would refuse.

#### Scenario: Too few participants
- **WHEN** a Pending group has 3 participants
- **THEN** "Draw the names" is unavailable and says at least 4 are needed

### Requirement: Temporary passwords are shown once
When making a group Pending, or saving a Pending group's participants, creates accounts, the app SHALL show each created username with its temporary password in a dialog, selectable so that it can be copied, and SHALL say that each person must change it when first signing in and that an administrator can look it up under Users until then. The dialog SHALL be shown only when accounts were created. Once it is closed the app SHALL keep none of the passwords.

#### Scenario: Accounts created
- **WHEN** making a group Pending creates accounts for `bob` and `carol`
- **THEN** a dialog lists `bob` and `carol` with their temporary passwords

#### Scenario: No accounts created
- **WHEN** every participant already has an account
- **THEN** no dialog is shown

#### Scenario: Closing the dialog
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app

### Requirement: Loading, errors and refreshing
Every admin group screen SHALL show that it is loading, SHALL show a message with "Try again" when the server cannot be reached or answers with an error, and SHALL keep what it already shows while a refresh fails. Each screen SHALL be refreshed as described in "Pulling a screen refreshes it" and "The web has no refresh control" and SHALL reload when the app returns to the foreground.

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached while the list loads
- **THEN** the screen says so and offers "Try again"

#### Scenario: Refresh keeps what is shown
- **WHEN** a refresh fails while the list is shown
- **THEN** the list stays on screen with the error message

### Requirement: Duplicating a group
Choosing "Duplicate" on a group SHALL ask for confirmation, naming the group. Confirming SHALL create a Draft with `POST /admin/groups` having the original's description, location, budget, currency, date, time and participants and the name "Copy of " followed by the original's name, and SHALL open the new group.

#### Scenario: Duplicate an Active group
- **WHEN** the administrator confirms "Duplicate" on an Active group "Party" with participants `alice` and `bob`
- **THEN** a Draft "Copy of Party" with the same details and participants `alice` and `bob` is created and its detail opens, and "Party" is still Active

#### Scenario: Cancel
- **WHEN** the administrator cancels the confirmation
- **THEN** nothing is created

#### Scenario: A long name
- **WHEN** the original's name is 100 characters long
- **THEN** the copy's name is 100 characters long, starts with "Copy of " and ends with an ellipsis

#### Scenario: Participants without an account
- **WHEN** the original has a participant who has no account yet
- **THEN** the copy lists that username too

#### Scenario: Failure
- **WHEN** the server cannot be reached, or refuses the new group
- **THEN** the dialog says so, nothing is created and the list stays as it was

#### Scenario: Budget is copied
- **WHEN** the administrator confirms "Duplicate" on a group with the budget 100 and the currency `EUR`
- **THEN** the copy has the budget 100 and the currency `EUR`

### Requirement: Deleting a group
Every group, whatever its state, SHALL offer "Delete" in the row of buttons under its details, after a confirmation, sent with `DELETE /admin/groups/{id}`. Deleting SHALL return to the list.

#### Scenario: Delete a Draft
- **WHEN** the administrator confirms "Delete" on a Draft
- **THEN** the group is gone and the list is shown, with no second dialog

#### Scenario: Delete in any state
- **WHEN** the group is Pending, Active or Archived
- **THEN** it offers "Delete" like a Draft does

#### Scenario: Second confirmation
- **WHEN** the administrator confirms "Delete" on an Active group that has wishlist items, questions and answers
- **THEN** a second dialog says how many of each will be deleted, with the draw, and that this cannot be undone, and the group is still there

#### Scenario: Confirmed twice
- **WHEN** the administrator confirms the second dialog
- **THEN** the group is deleted and the list is shown

#### Scenario: Cancelled at the second dialog
- **WHEN** the administrator cancels the second dialog
- **THEN** nothing is deleted and the group's page is shown as it was

#### Scenario: Pending without wishlist items
- **WHEN** the administrator confirms "Delete" on a Pending group that has no wishlist items
- **THEN** the group is deleted with one confirmation

#### Scenario: Group gone meanwhile
- **WHEN** the server answers that the group no longer exists while the administrator deletes it
- **THEN** the app says the group is no longer available

### Requirement: Saying who was emailed
When the answer to "Draw the names" carries an email summary, the group's page SHALL say once how many participants were emailed their draw and, when there are any, how many emails failed and how many participants have no email address, until it is dismissed or the page is left. When the answer has no summary, the page SHALL say nothing about email.

#### Scenario: All emailed
- **WHEN** the draw is made for 4 participants and 4 emails are sent
- **THEN** the page says that emails were sent to all 4 participants

#### Scenario: Some failed or have no address
- **WHEN** 11 participants are drawn, 8 emails are sent, 1 fails and 2 have no address
- **THEN** the page says 8 of 11 were emailed, 1 failed and 2 have no email address

#### Scenario: Email not configured
- **WHEN** the answer has no email summary
- **THEN** nothing about email is shown

#### Scenario: Dismissed
- **WHEN** the administrator dismisses the note
- **THEN** it is gone until another draw

### Requirement: Searching a group's participants
The administrator's page for a Pending, Active or Archived group SHALL have a search field above its participants or pairs, shown once the group has loaded and has participants. The list SHALL show only the participants whose username or full name matches what is typed, ignoring upper and lower case; with several words, a participant SHALL match when every word is found in the username or the full name, in any order. An empty or blank search SHALL show everyone.

#### Scenario: Match by username
- **WHEN** the person types "ali" on a Pending group with `alice` and `bob`
- **THEN** only `alice` is listed

#### Scenario: Match by full name
- **WHEN** the person types "brown" and `bob`'s full name is "Bob Brown"
- **THEN** `bob` is listed

#### Scenario: Words in any order
- **WHEN** the person types "brown bob" and `bob`'s full name is "Bob Brown"
- **THEN** `bob` is listed

#### Scenario: Blank search
- **WHEN** the search holds only spaces
- **THEN** every participant or pair is listed

### Requirement: Searching pairs and what stays the same
On an Active or Archived group a pair SHALL be shown when its Santa or its receiver matches the search. The search SHALL change only what is shown: the participant count, the sheets that open from a row and every action stay as they are.

#### Scenario: Pair with a matching Santa
- **WHEN** the person types "alice" on an Active group where alice gives to bob and carol gives to alice and dave gives to carol
- **THEN** the pairs alice to bob and carol to alice are listed and dave to carol is not

#### Scenario: Pair with a matching receiver
- **WHEN** the person types "bob" on an Archived group where alice gives to bob
- **THEN** the pair alice to bob is listed

#### Scenario: Count unchanged
- **WHEN** a search narrows a Pending group of four participants to one
- **THEN** the heading still says four participants

### Requirement: Nothing matches a participant search
When no participant or pair matches, the page SHALL say so in place of the list and keep the search field, so that the person can change it. The search field SHALL have a control that empties it while it holds text.

#### Scenario: No match
- **WHEN** the person types a name that nobody in the group has
- **THEN** the page says that no participant matches and the search field is still there

#### Scenario: Clearing
- **WHEN** the search field holds text
- **THEN** it has a control that empties it and shows everyone again

### Requirement: Admin group details are shown in a card
On the admin group screen the group's name SHALL be centered horizontally, the group's description SHALL be shown in a card of its own above the card, and the date and time, location and budget in the card, with the same icons, as `client-groups` describes for the group page, below the name and the state with its note. The card SHALL always be shown, with `TBD` in place of a value the group does not have, and, outside a Draft group, SHALL have the Group Notifications icon at its end. The actions of the group (edit, announce, the next step and delete) SHALL be below the card and SHALL NOT be in it.

#### Scenario: Details in the card
- **WHEN** an administrator opens a group that has a description, a date and time, a location and a budget
- **THEN** the description is in a card of its own above the card, and the card shows the calendar icon with the date and time, the map pin icon with the location and the bank icon with the budget and its currency

#### Scenario: Actions stay below
- **WHEN** the screen shows the card and the group has actions
- **THEN** the buttons are below the card and none is inside it

#### Scenario: Nothing to show
- **WHEN** a group has no description, date, time, location or budget
- **THEN** the card is shown with its three lines, each with its icon and `TBD`, and the actions follow the card

#### Scenario: The messages icon
- **WHEN** the owner opens a Pending, an Active or an Archived group
- **THEN** the card has the Group Notifications icon at its end, and a Draft group's card has none

### Requirement: Who takes part depends on the state
For a Draft group who takes part SHALL be the participants as chips; for a Pending group the participants as rows; for an Active or Archived group the pair list of `client-admin-oversight`, which replaces the participants list. The chips of a Draft group SHALL wrap onto as many lines as needed and SHALL NOT be tappable.

#### Scenario: Chips wrap and do not respond
- **WHEN** a Draft group has many participants
- **THEN** the chips wrap onto several lines and tapping one does nothing

### Requirement: Pending participants open sheets
In a Pending group choosing a participant's username SHALL open that person's user sheet of `client-admin-oversight`, and each row SHALL end with a gift icon, named "Read wishlist" for assistive technology, that opens the participant's wishlist.

#### Scenario: Gift icon named for assistive technology
- **WHEN** a screen reader reads the rows of a Pending group
- **THEN** each gift icon is announced as "Read wishlist"

### Requirement: The group page scrolls as one
The whole page SHALL scroll as one: the details, the actions, the search field and the people move together, and no area of the page scrolls inside itself.

#### Scenario: Search field scrolls away
- **WHEN** the page of a group with many participants is scrolled down
- **THEN** the details, the actions and the search field scroll away with the people

### Requirement: Marks in the pair list
In the pair list of an Active or Archived group a username SHALL carry the key icon while its account is on a temporary password and the pin icon while its delivery info lacks a full name, email, phone or address, as described in `client-admin-oversight`.

#### Scenario: Pair list mark
- **WHEN** `bob`'s delivery info has no address and the group is Active
- **THEN** `bob` is followed by the pin icon in the pair list

### Requirement: Marks never block an action
The marks SHALL NOT block any action.

#### Scenario: Marks do not disable controls
- **WHEN** a participant is marked
- **THEN** no button or row of the group is disabled because of the mark

### Requirement: A duplicate's name is cut to 100 characters
A name that would exceed 100 characters with the prefix SHALL be cut, ending in an ellipsis, so that it is exactly 100 characters.

#### Scenario: Exactly 100
- **WHEN** the original's name is 98 characters long
- **THEN** the copy's name is exactly 100 characters and ends in an ellipsis

### Requirement: A duplicate copies nothing of the draw
Nothing of the original's draw, wishlists or questions SHALL be copied, and the original SHALL NOT change.

#### Scenario: Original untouched
- **WHEN** an Active group is duplicated
- **THEN** the copy has no draw, wishlists or questions and the original is unchanged

### Requirement: Deleting a group with content asks again
When the server answers 409 `confirmation_required` the app SHALL show a second dialog naming what would be deleted from the counts it returns (wishlist items, questions, answers and the draw) and saying that this cannot be undone, and SHALL send the request again with `confirm=true` only if that is confirmed.

#### Scenario: The dialog names the counts
- **WHEN** the server answers 409 with counts of 3 wishlist items and 2 questions
- **THEN** the second dialog names those counts and says that this cannot be undone

### Requirement: The group form has a budget
The group form SHALL have a "Budget (optional)" field that takes digits only and a "Currency" field that starts with `RON` on a new group and with the group's own currency when editing one that has a budget. Clearing the budget SHALL save the group without a budget and without a currency.

#### Scenario: New group
- **WHEN** the administrator opens the form for a new group
- **THEN** the budget is empty and the currency reads `RON`

#### Scenario: Edit a budget
- **WHEN** the administrator opens an Active group with the budget 100 and the currency `EUR`, sets the budget to 150 and saves
- **THEN** the detail shows "Budget? - 150 EUR"

#### Scenario: Clear the budget
- **WHEN** the administrator empties the budget field and saves
- **THEN** the group has no budget and the detail shows no budget line

#### Scenario: Letters are not accepted
- **WHEN** the administrator types letters in the budget field
- **THEN** the field keeps only the digits

### Requirement: Get passwords button
The administrator's page for a Pending or Active group SHALL show a "Get passwords" button after "Send announcement". Choosing it SHALL load the managed users with `GET /admin/users` and open a dialog listing each participant of that group who still has a temporary password, as `username: password`, selectable so that it can be copied. A Draft or Archived group SHALL NOT have the button.

#### Scenario: Pending group with new accounts
- **WHEN** a Pending group has `bob` and `carol` who have not changed their temporary passwords, and the administrator chooses "Get passwords"
- **THEN** a dialog lists `bob` and `carol` with their temporary passwords

#### Scenario: Only those who still have one
- **WHEN** `alice` has set her own password and `dave` has not
- **THEN** the dialog lists `dave` and not `alice`

#### Scenario: Only this group
- **WHEN** another group of the administrator has a person with a temporary password who is not in this group
- **THEN** that person is not listed

#### Scenario: Button after Send announcement
- **WHEN** the page of a Pending or Active group shows its buttons
- **THEN** "Get passwords" comes directly after "Send announcement"

#### Scenario: Other states
- **WHEN** the group is a Draft or Archived
- **THEN** there is no "Get passwords" button

### Requirement: The passwords dialog on demand
The dialog opened by "Get passwords" SHALL say that the listed people have not changed their password yet and must change it when they first sign in. When nobody in the group has a temporary password it SHALL say so and list nobody. Closing it SHALL leave none of the passwords in the app. The button SHALL be unavailable while the passwords load, and a failure to load SHALL be said with the page's usual notice and open no dialog.

#### Scenario: Nobody has one
- **WHEN** every participant has set their own password
- **THEN** the dialog says that nobody in the group has a temporary password

#### Scenario: Closing
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app

#### Scenario: Loading
- **WHEN** the passwords are being loaded
- **THEN** the button cannot be chosen again until they arrive

#### Scenario: Server unreachable
- **WHEN** the server cannot be reached
- **THEN** the page says so with its notice and no dialog opens

### Requirement: Copying from the passwords dialog
Every row of the temporary passwords dialog SHALL have the app's copy icon, which puts that row on the clipboard as `username: password` with the icon's usual confirmation or failure. With two or more rows the dialog SHALL also have a "Copy all" button beside "OK" that copies every row as `username: password`, one per line, in the order shown. The rows SHALL stay selectable, in every use of the dialog.

#### Scenario: Copy one
- **WHEN** the dialog lists `bob` and `carol` and the person chooses the copy icon of `bob`
- **THEN** the clipboard holds the single line `bob: <password>` and nothing of `carol`, and that icon shows it was copied

#### Scenario: Copy all
- **WHEN** the dialog lists `bob` and `carol` and the person chooses "Copy all"
- **THEN** the clipboard holds a line `bob: <password>` and a line `carol: <password>`, in that order

#### Scenario: One person
- **WHEN** the dialog lists one person, as after "Reset password"
- **THEN** it has that row's copy icon and no "Copy all"

#### Scenario: Nobody
- **WHEN** the dialog says that nobody has a temporary password
- **THEN** it has no copy icon and no "Copy all"

#### Scenario: Clipboard refused
- **WHEN** the platform refuses the copy, as the web does on a page that is not secure
- **THEN** the icon or button shows that it failed and the rows can still be selected and copied by hand

#### Scenario: Closing
- **WHEN** the dialog is closed
- **THEN** the passwords are no longer anywhere in the app

### Requirement: Group actions are icon buttons
On the administrator's group page, "Edit details" SHALL be an outlined pencil icon button, "Send announcement" a bell, "Get passwords" the app's key icon and "Delete" a trash can, all outlined, the trash can with its outline and icon in the error colour. They SHALL come in that order, each only where its button is offered, and keep the rules of those buttons, including being unavailable while the page is busy. Each SHALL be spoken by its old label.

#### Scenario: Icons in order
- **WHEN** the page of an Active group is shown
- **THEN** the actions are, in order, a pencil, a bell, a key and a trash can, and the step comes after them

#### Scenario: Spoken names
- **WHEN** a screen reader reaches the four icon buttons
- **THEN** it says "Edit details", "Send announcement", "Get passwords" and "Delete"

#### Scenario: Only where offered
- **WHEN** the page of a Draft is shown
- **THEN** it has the pencil, the trash can and the step, and no bell or key

#### Scenario: Busy
- **WHEN** the page is busy with a step or with loading the passwords
- **THEN** the key and the trash can cannot be chosen

### Requirement: The rules button
The administrator's group page SHALL show a gavel icon button in the row of the group's actions, in Pending, Active and Archived, and not in Draft. It SHALL be named "Draw rules" for a screen reader, and SHALL open the rules page.

#### Scenario: Pending
- **WHEN** the admin views a Pending group
- **THEN** the row shows the gavel button, enabled

#### Scenario: Active and Archived
- **WHEN** the admin views an Active or an Archived group
- **THEN** the row shows the gavel button, and the rules it opens are read-only

#### Scenario: Draft
- **WHEN** the admin views a Draft group
- **THEN** the row shows no gavel button

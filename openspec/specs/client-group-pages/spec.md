# client-group-pages Specification

## Purpose
What a participant sees of a group they are in: the two cards, the sheets with their own and their receiver's details, and the Gifter and Receiver pages with their wishlists and questions.

## Requirements

### Requirement: User details sheets
The "Delivery Info" button of the "Your stuff" card SHALL open a bottom sheet with the person's own details (the username and the delivery info they have set for the group), and that of the "Secret Santa" card one loaded with `GET /groups/{id}/receiver` showing the receiver's username and, for each of full name, email, address, phone and delivery information that they have set, its value, leaving out what is not set and saying that the receiver has not added postal details yet when none is.

#### Scenario: My details
- **WHEN** the person chooses "Delivery Info" in the "Your stuff" card
- **THEN** a sheet shows their username and the delivery info they have set for this group

#### Scenario: Receiver's details
- **WHEN** the receiver has set a full name and an address but no email, phone or delivery information
- **THEN** the sheet shows the username, the full name and the address and nothing for the others

#### Scenario: Receiver's email
- **WHEN** the receiver has set an email address
- **THEN** the sheet shows it, labelled as the email, with the other details that are set

#### Scenario: Only an email set
- **WHEN** the receiver has set an email address and none of the other four fields
- **THEN** the sheet shows the username and the email and does not say that postal details are missing

#### Scenario: No postal details yet
- **WHEN** the receiver has set none of the five fields
- **THEN** the sheet shows the username and says the receiver has not added postal details yet

#### Scenario: Santa hidden before archiving
- **WHEN** the group is Active
- **THEN** nothing offers to reveal the Santa and nothing shows who the Santa is

#### Scenario: Santa revealed after archiving
- **WHEN** the group is Archived and the person chooses "Reveal Santa"
- **THEN** a sheet shows the Santa's username and full name

#### Scenario: Revealed too early
- **WHEN** the server refuses the Santa request because the group is not Archived
- **THEN** the sheet says the Santa is revealed once the group is archived

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist
- **THEN** the sheet says the group is no longer available and offers to close

### Requirement: Receiver page
The Receiver page, the one for the relationship in which the person receives, SHALL open from the "Edit Wishlist" button of the "Your stuff" card, and from its "Respond to Santa" button once the group is Active or Archived. Nothing on the page SHALL identify the Santa.

#### Scenario: Items in order
- **WHEN** the person has three items
- **THEN** the Wishlist tab shows them in the order the person chose, each with its title and, when present, its note and link

#### Scenario: Empty list
- **WHEN** the person has not added anything to this group
- **THEN** the Wishlist tab says so and offers to add the first item

#### Scenario: Separate per group
- **WHEN** the person is in two groups
- **THEN** each group's Receiver page shows only that group's items

#### Scenario: Pending group
- **WHEN** the group is Pending
- **THEN** the page is titled "Your Wishlist", shows the wishlist and no tabs and no questions

#### Scenario: Active group
- **WHEN** the group is Active and the Santa has asked a question
- **THEN** the page is titled "Receiving from Secret Santa", has the tabs "Your Wishlist" and "Respond to Secret Santa", and the "Respond to Secret Santa" tab shows the question with an "Answer" action

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the wishlist and the questions with the person's answers are shown in their tabs and nothing can be changed

#### Scenario: Group phase not known yet
- **WHEN** the page is opened and the group's phase has not loaded
- **THEN** the page is titled "Your Wishlist" until the phase is known, and is then titled for that phase

#### Scenario: Switching tabs
- **WHEN** the person chooses the "Respond to Secret Santa" tab and then the "Your Wishlist" tab
- **THEN** each shows its own content and the tab row stays in place

#### Scenario: Swiping
- **WHEN** the person swipes the "Your Wishlist" tab's content to the left
- **THEN** the "Respond to Secret Santa" tab is shown and the tab row marks it

### Requirement: Gifter page
The Gifter page, the one for the relationship in which the person gives, SHALL open from the "See Wishlist" and "Ask Questions" buttons of the "Secret Santa" card once the group is Active or Archived and SHALL be titled "Giving to <receiver username>". Nothing the person does on the page SHALL be visible to the receiver.

#### Scenario: Receiver's items
- **WHEN** the person opens the page and the receiver has two items
- **THEN** the Wishlist tab shows both in the receiver's order with their note and link, and no control changes them

#### Scenario: Nothing added yet
- **WHEN** the receiver has not added anything
- **THEN** the Wishlist tab says the receiver has not added anything yet

#### Scenario: Questions and answers
- **WHEN** the person has asked two questions and the receiver answered the first
- **THEN** the Questions tab shows question 1 with its answer and question 2 with "Not answered yet"

#### Scenario: Ask from the page
- **WHEN** the group is Active and the person chooses to ask a question on the Questions tab
- **THEN** a bottom sheet opens, as `client-receiver-and-questions` describes

#### Scenario: Named after the receiver
- **WHEN** the person opens the Gifter page and their receiver is `bob`
- **THEN** the page is titled "Giving to bob" and its tabs read "bob's Wishlist" and "Questions for bob"

#### Scenario: Receiver not known yet
- **WHEN** the receiver's username has not loaded, or cannot be loaded
- **THEN** the page is titled "Giving to ..." and its tabs read "Wishlist" and "Questions", and they take the receiver's name once it loads

#### Scenario: Opened from an address
- **WHEN** the Gifter page is opened from its web address, with no earlier screen
- **THEN** the title and tabs show the fallback until the receiver loads and then name the receiver

#### Scenario: A long username
- **WHEN** the receiver's username is too long for its tab
- **THEN** the tab label stays on one line with an ellipsis, and a screen reader announces the whole label

#### Scenario: Swiping
- **WHEN** the person swipes the first tab's content to the left
- **THEN** the second tab is shown and the tab row marks it

### Requirement: Pages can open on a tab
The Receiver and Gifter pages SHALL be able to open on either of their two tabs, the wishlist tab by default. A page that has no tabs, the Receiver page of a Pending group, SHALL show its wishlist whichever tab was asked for. Opening on a tab SHALL NOT change what the tab shows or how it loads.

#### Scenario: Opens on the questions tab
- **WHEN** the Receiver page is opened on its questions tab in an Active group
- **THEN** the "Respond to Secret Santa" tab is shown and its questions load

#### Scenario: Default tab
- **WHEN** a page is opened without a tab
- **THEN** its wishlist tab is shown

#### Scenario: Pending group
- **WHEN** the Receiver page is opened on its questions tab while the group is Pending
- **THEN** the page shows the wishlist and no tabs

#### Scenario: Other tab still works
- **WHEN** a page opened on its questions tab is swiped to its wishlist tab
- **THEN** the wishlist tab shows and loads as it does when it is opened first

### Requirement: Page loading and refreshing
Each tab of the Gifter and Receiver pages SHALL load on its own, show that it is loading, and show a message with "Try again" when the server cannot be reached or answers with an error, without affecting the other tab or what it already showed.

#### Scenario: One section fails
- **WHEN** the questions cannot be loaded and the wishlist can
- **THEN** the Wishlist tab shows the list and the Questions tab shows the problem with "Try again"

#### Scenario: Pull to refresh
- **WHEN** the person pulls a tab down on Android or iOS
- **THEN** that tab reloads and what is shown stays until the new data arrives

#### Scenario: Choosing a tab
- **WHEN** the person chooses a tab they have not looked at for a while
- **THEN** its content reloads without clearing what it showed

#### Scenario: Swiping to a tab
- **WHEN** the person swipes to a tab they have not looked at for a while
- **THEN** its content reloads without clearing what it showed

### Requirement: The receiver's username comes from the server
The receiver's username SHALL come from `GET /groups/{id}/receiver`; while it loads the "Secret Santa" card SHALL show that, and when it fails the card SHALL say so with "Try again" and the rest of the page SHALL stay.

#### Scenario: Loading state
- **WHEN** the receiver request is still running
- **THEN** the card shows that it is loading and the rest of the page is shown

### Requirement: The Santa stays hidden until the group is archived
While the group is Pending or Active nothing on the page SHALL identify the Santa or offer to. Once it is Archived the "Your stuff" card SHALL have a "Reveal Santa" button that opens a bottom sheet loaded with `GET /groups/{id}/santa` showing the Santa's username and full name when it has one.

#### Scenario: Row does nothing
- **WHEN** the group is Active
- **THEN** the "Your stuff" card has no "Reveal Santa" button and no name is shown

#### Scenario: Archived
- **WHEN** the group is Archived and the person chooses "Reveal Santa"
- **THEN** a sheet shows the Santa's username and full name

### Requirement: The sheets are not screens
The sheets SHALL be part of the group page, not screens, so the web address stays on the page while one is open.

#### Scenario: Address while a sheet is open
- **WHEN** a person on the web opens a details sheet
- **THEN** the address is still the group page's

### Requirement: The Receiver page of a drawn group has two tabs
Once the group is Active or Archived the page SHALL be titled "Receiving from Secret Santa" and show a tab row with two tabs, **Your Wishlist** and **Respond to Secret Santa**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. On a wide window the page SHALL have no tab row and no swiping: it shows both sections, with the Outbox, side by side as `client-layout` describes.

#### Scenario: Title and tabs
- **WHEN** the Receiver page of an Active group is opened
- **THEN** it is titled "Receiving from Secret Santa" with the tabs Your Wishlist and Respond to Secret Santa

#### Scenario: Wide window
- **WHEN** the Receiver page of an Active group is opened on a wide window
- **THEN** there is no tab row, and "Your Wishlist", "Respond to Secret Santa" and the Outbox are shown beside each other

### Requirement: The Receiver page's wishlist tab
The first tab SHALL show the person's own wishlist for the group, loaded with `GET /groups/{id}/wishlist`, with each item's title, note and link in the person's own order, or a short explanation that nothing has been added yet with the offer to add the first item; the list SHALL be edited as `client-wishlist` describes.

#### Scenario: Loaded from the wishlist route
- **WHEN** the first tab is shown
- **THEN** the person's items come from `GET /groups/{id}/wishlist`

### Requirement: The Receiver page's questions tab
The second tab SHALL show the questions the person's Santa asked as `client-receiver-and-questions` describes, with the answering described there.

#### Scenario: Questions with the way to answer
- **WHEN** the second tab is shown for an Active group
- **THEN** it lists the questions the Santa asked, with the way to answer them

### Requirement: A pending group's Receiver page has no tabs
While the group is Pending the page SHALL be titled "Your Wishlist" and show the wishlist only, without tabs.

#### Scenario: Title and no tabs
- **WHEN** the Receiver page of a Pending group is opened
- **THEN** it is titled "Your Wishlist" and has no tab row

### Requirement: The Gifter page has two tabs
The Gifter page SHALL show a tab row with two tabs, **<receiver username>'s Wishlist** and **Questions for <receiver username>**, and one tab at a time, which the person SHALL be able to change by choosing a tab or by swiping the content sideways, the tab row following the swipe. On a wide window the page SHALL have no tab row and no swiping: it shows both sections, with the Inbox, side by side as `client-layout` describes.

#### Scenario: Tab titles
- **WHEN** the Gifter page for `alice` is opened
- **THEN** its tabs are "alice's Wishlist" and "Questions for alice"

#### Scenario: Wide window
- **WHEN** the Gifter page for `alice` is opened on a wide window
- **THEN** there is no tab row, and "alice's Wishlist", "Questions for alice" and the Inbox are shown beside each other

### Requirement: The Gifter page's wishlist tab
The first tab SHALL show the list the person's receiver keeps for the group, loaded with `GET /groups/{id}/receiver/wishlist`, read-only, in the receiver's order, with each item's title, note and link, or a short explanation that the receiver has not added anything yet.

#### Scenario: Read-only list
- **WHEN** the first tab is shown
- **THEN** there is no control that adds, changes, deletes or moves an item

### Requirement: The Gifter page's questions tab
The second tab SHALL show the person's own questions to the receiver as `client-receiver-and-questions` describes, with the receiver's answers and, while the group is Active, the way to ask a new question.

#### Scenario: Archived group
- **WHEN** the group is Archived
- **THEN** the questions and answers are shown and there is no way to ask a new question

### Requirement: Tabs reload
Pulling a tab down (on Android and iOS) SHALL reload that tab, and returning to the app and showing a tab, by choosing it or by swiping to it, SHALL reload what the tab shows, as "Pulling a screen refreshes it" and "The web has no refresh control" describe.

#### Scenario: Returning to the app
- **WHEN** a person returns to the app with a tab showing
- **THEN** that tab reloads what it shows

### Requirement: The Secret Santa card
Below the group's information the group page SHALL show, once the group is Active or Archived, a card headed "You're Secret Santa for:" with, on the next row in a bold title font, the receiver's username in capital letters, and under it the line "Shh... Keep it a secret!" in italics, then one row of four icon buttons, as `The card actions are icon buttons` describes: "See Wishlist", "Ask Questions", "Inbox" and, last, "Delivery Info". While the group is Pending there SHALL be no such card, as there is no receiver to tell about yet.

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the group page has no "You're Secret Santa for:" card

#### Scenario: The secret line
- **WHEN** the card is shown in an Active or an Archived group
- **THEN** the line "Shh... Keep it a secret!" is under the name, in italics

#### Scenario: Active
- **WHEN** the group is Active and the person's receiver is `bob`
- **THEN** the card shows the heading, "BOB", the line "Shh... Keep it a secret!" and one row of four icon buttons: "See Wishlist", "Ask Questions", "Inbox" and, last, "Delivery Info"

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the card is shown as for an Active group, and the pages it opens are read-only as before, "Inbox" included

#### Scenario: The name as it is
- **WHEN** a screen reader reads the card for the receiver `bob`
- **THEN** it reads "bob", not a spelled-out capital, and the sheet shows `bob` as written

#### Scenario: Delivery Info
- **WHEN** the person chooses "Delivery Info"
- **THEN** the receiver's details sheet opens

#### Scenario: See Wishlist
- **WHEN** the person chooses "See Wishlist"
- **THEN** the Gifter page opens on its wishlist tab

#### Scenario: Ask Questions
- **WHEN** the person chooses "Ask Questions"
- **THEN** the Gifter page opens on its questions tab

#### Scenario: Inbox
- **WHEN** the person chooses "Inbox"
- **THEN** the Inbox page opens, with the letters their receiver has sent them

#### Scenario: Receiver not known
- **WHEN** the receiver request has not finished or has failed
- **THEN** the name reads "..." and the four buttons are disabled, and a failure shows its message with "Try again"

### Requirement: The Your stuff card
Below the Secret Santa card the group page SHALL show a card headed "Your stuff:", the line "Tell your santa what you want and let them know you!" in italics under it, one row of icon buttons, as `The card actions are icon buttons` describes: "Edit Wishlist" and, from Active on, "Respond to Santa" and "Send letter" and, last, "Delivery Info". In an Archived group it SHALL also have, above that row, the "Reveal Santa" text button of `The Santa stays hidden until the group is archived`.

#### Scenario: The encouraging line
- **WHEN** the card is shown in a Pending, an Active or an Archived group
- **THEN** the line "Tell your santa what you want and let them know you!" is under "Your stuff:", in italics

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** the card has "Delivery Info" and "Edit Wishlist" and no "Respond to Santa" and no "Send letter"

#### Scenario: Active
- **WHEN** the group is Active
- **THEN** the card has "Delivery Info", "Edit Wishlist", "Respond to Santa" and "Send letter" and no "Reveal Santa"

#### Scenario: Archived
- **WHEN** the group is Archived
- **THEN** the card has "Delivery Info", "Edit Wishlist", "Respond to Santa", "Send letter" and "Reveal Santa"

#### Scenario: Delivery Info
- **WHEN** the person chooses "Delivery Info"
- **THEN** a sheet shows their own username and the delivery info they have set for this group

#### Scenario: Edit Wishlist
- **WHEN** the person chooses "Edit Wishlist"
- **THEN** the Receiver page opens on their wishlist tab

#### Scenario: Send letter
- **WHEN** the person chooses "Send letter"
- **THEN** the Send letter page opens, with the letters they have sent their Santa

#### Scenario: Respond to Santa
- **WHEN** the person chooses "Respond to Santa" in an Active group
- **THEN** the Receiver page opens on its "Respond to Secret Santa" tab

#### Scenario: Group phase not known
- **WHEN** the group has not loaded
- **THEN** neither card is shown

### Requirement: The username is centered in the Delivery Info sheets
In the two sheets opened by the "Delivery Info" buttons the username SHALL be centered horizontally in the sheet, with its copy icon at its end, so that the name itself stays in the middle whether or not the icon is there.

#### Scenario: Own sheet
- **WHEN** the person opens their own Delivery Info sheet
- **THEN** their username is in the middle of the sheet's width, with the copy icon beside it

#### Scenario: Receiver's sheet
- **WHEN** the person opens their receiver's Delivery Info sheet
- **THEN** the receiver's username is in the middle of the sheet's width, with the copy icon beside it

#### Scenario: A long username
- **WHEN** a username is longer than one row
- **THEN** it is cut as usual and choosing it opens the dialog with all of it

### Requirement: Edit Delivery Info
The sheet of the "Your stuff" card's "Delivery Info" button SHALL end with an "Edit Delivery Info" button, below the details or the note that none are set, while the group is Pending or Active. Choosing it SHALL close the sheet and open the group's Delivery Info page. The receiver's sheet SHALL NOT have this button.

#### Scenario: The button
- **WHEN** the person's own Delivery Info sheet is shown with their details
- **THEN** the last thing in the sheet is an "Edit Delivery Info" button

#### Scenario: No profile yet
- **WHEN** the person has set no delivery info for the group and opens their own sheet
- **THEN** the sheet says so and still ends with the "Edit Delivery Info" button

#### Scenario: Opening the profile
- **WHEN** the person chooses "Edit Delivery Info"
- **THEN** the sheet closes and the Delivery Info page of that group is shown

#### Scenario: Not on the receiver's sheet
- **WHEN** the person opens their receiver's Delivery Info sheet
- **THEN** the sheet has no "Edit Delivery Info" button

#### Scenario: Archived group
- **WHEN** the person opens their own Delivery Info sheet in an Archived group
- **THEN** the sheet has no "Edit Delivery Info" button

### Requirement: Respond to Santa counts unanswered questions
While the group is Active and at least one question from the Santa has no answer, the "Respond to Santa" button of the "Your stuff:" card SHALL carry a badge on its corner in the theme's tertiary colour with the number of such questions, "9+" when there are more than 9. With none, or in a Pending or an Archived group, it SHALL carry no badge. A screen reader SHALL hear the number with the button's name and SHALL NOT read the badge on its own.

#### Scenario: Some unanswered
- **WHEN** the group is Active and the Santa has asked three questions of which the person answered one
- **THEN** the "Respond to Santa" button has a badge showing 2

#### Scenario: Back from answering
- **WHEN** the person answers a question on the Receiver page and returns to the group page
- **THEN** the badge shows the lower number, and is gone when none is left

#### Scenario: More than nine
- **WHEN** twelve questions have no answer
- **THEN** the badge shows "9+"

#### Scenario: None, Pending or Archived
- **WHEN** every question is answered, or the group is Pending or Archived
- **THEN** the button has no badge

#### Scenario: Screen reader
- **WHEN** a screen reader reads the button while two questions are unanswered
- **THEN** it reads "Respond to Santa, 2 unanswered" and the badge is not read on its own

#### Scenario: Layout
- **WHEN** the badge is shown on the button
- **THEN** the buttons on the row keep their size and place, and the badge is not cut off by the card

### Requirement: The group page counts from the Santa's questions
The group page SHALL count the unanswered questions from `GET /groups/{id}/santa/questions` while the group is Active, and SHALL reload them whenever the page is shown again, is pulled to refresh or returns to the foreground. While they load, or when loading them fails, the count is unknown: no badge SHALL be shown and the page SHALL show no message about it.

#### Scenario: Not loaded or failed
- **WHEN** the questions are still loading, or the request to load them failed
- **THEN** no badge is shown and the page shows no message about it

#### Scenario: Reloaded when shown again
- **WHEN** the person returns to the group page after the Santa asked another question
- **THEN** the badge counts it once the questions have reloaded

### Requirement: The Your stuff card asks for missing delivery info
In a Pending or an Active group, when the person's delivery info for the group lacks the full name, email, phone or address, the "Your stuff:" card SHALL show the text "Please fill out your delivery info in order to be able to receive your secret gift!" in the theme's error colour, directly above the row of icon buttons, below "Reveal Santa" if that is shown, and while it is shown the "Delivery Info" icon button SHALL be drawn in the error colour too, its outline and its icon. The message SHALL be plain text, not a control, and nothing here SHALL block any action.

#### Scenario: Incomplete in a Pending group
- **WHEN** the group is Pending and the person has not set their phone
- **THEN** the message is shown directly above the row of icon buttons, and the "Delivery Info" icon button is red

#### Scenario: Incomplete in an Active group
- **WHEN** the group is Active and the person has set nothing
- **THEN** the message is shown directly above the row of icon buttons, below "Reveal Santa" if that is shown

#### Scenario: Complete
- **WHEN** the person has set their full name, email, phone and address
- **THEN** no message is shown and the "Delivery Info" icon button is drawn as the others are

#### Scenario: Only the delivery information empty
- **WHEN** the person has set the four required fields and left the delivery information empty
- **THEN** no message is shown

#### Scenario: Archived group
- **WHEN** the group is Archived and the person's delivery info is incomplete
- **THEN** no message is shown, because nothing can be changed any more

#### Scenario: Wording
- **WHEN** the message is shown
- **THEN** it reads exactly "Please fill out your delivery info in order to be able to receive your secret gift!" in the error colour, and a screen reader reads it as text

#### Scenario: Nothing is blocked
- **WHEN** the message is shown
- **THEN** every button of the card can still be used

### Requirement: The group page checks the person's delivery info quietly
While the group is Pending or Active the group page SHALL load the person's own delivery info with `GET /groups/{id}/delivery-info` and reload it whenever the page is shown again, is pulled to refresh or returns to the foreground. Until the first load has finished, or when loading fails, the page SHALL show no message about it and SHALL NOT show the missing delivery info message; a reload keeps what was shown until its answer arrives.

#### Scenario: Loading
- **WHEN** the group page is opened and the delivery info has not arrived yet
- **THEN** no message is shown

#### Scenario: Reloading
- **WHEN** the message is shown and the page reloads the delivery info
- **THEN** the message stays until the answer arrives, and goes if the answer is complete

#### Scenario: Loading fails
- **WHEN** the person's delivery info cannot be loaded, or a reload fails after an earlier load
- **THEN** no message is shown and the page shows no error about it

#### Scenario: Gone after saving
- **WHEN** the person fills in the missing fields on the Delivery Info page, saves and goes back to the group page
- **THEN** the page reloads their delivery info and the message is gone

#### Scenario: Draft group
- **WHEN** the group is a Draft
- **THEN** the person's delivery info is not requested

### Requirement: The card actions are icon buttons
The actions of the "You're Secret Santa for:" and "Your stuff:" cards, except "Reveal Santa", SHALL be outlined icon buttons with no text, in one row at the start of the card. The icons SHALL be the gift for "See Wishlist" and "Edit Wishlist", the Material "Send" paper plane for "Ask Questions" and "Respond to Santa", the mail icon for "Inbox" and "Send letter" and the outlined map pin for both "Delivery Info" buttons, in that order. "Reveal Santa" SHALL stay a text button.

#### Scenario: Secret Santa card icons
- **WHEN** the group is Active
- **THEN** the "You're Secret Santa for:" card has one row of four outlined icon buttons, gift, send, mail and pin, with no text on them

#### Scenario: Your stuff card icons
- **WHEN** the group is Active
- **THEN** the "Your stuff:" card has one row of four outlined icon buttons, gift (Edit Wishlist), send (Respond to Santa), mail (Send letter) and pin (Delivery Info), and no text on them

#### Scenario: Before the draw
- **WHEN** the group is Pending
- **THEN** the "Your stuff:" card has only the gift and the pin buttons

#### Scenario: Reveal Santa
- **WHEN** the group is Archived
- **THEN** "Reveal Santa" is still a text button, above the row of icon buttons in the "Your stuff:" card

### Requirement: Card icon buttons keep their names, badges and states
Each card icon button's former label SHALL be its spoken name and its tooltip, shown on hover and on a long press, so the requirements that name these buttons still describe them. The count badges of `client-read-marks` and `Respond to Santa counts unanswered questions` SHALL stay on the corner of their buttons with the count still spoken with the name, and a button SHALL be disabled where it was.

#### Scenario: A tooltip
- **WHEN** the person hovers a card icon button, or presses it for a long time on a touch screen
- **THEN** a tooltip shows its name, such as "Ask Questions", and a short press still opens what it opens

#### Scenario: Spoken name
- **WHEN** a screen reader reads a card icon button
- **THEN** it reads the name, such as "Inbox", and with new items "Inbox, 3 new"

#### Scenario: Badges stay
- **WHEN** three new letters wait for the Santa
- **THEN** the mail button of the "You're Secret Santa for:" card has a badge showing 3 on its corner

#### Scenario: Receiver not known
- **WHEN** the receiver has not loaded
- **THEN** the four icon buttons of the "You're Secret Santa for:" card are disabled

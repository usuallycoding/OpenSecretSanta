# push-notifications Specification

## Purpose
Tells people, by push notification on their Android and web devices, when something happens to them in a group: they are added to one, its names are drawn or the owner sends an announcement, and, once a day, that something changed in their wishlist, questions, answers or letters. Each person chooses whether they receive announcements and whether they receive the daily digest.

## Requirements

### Requirement: Registering a device
A signed-in person SHALL be able to register a device for push notifications by giving the server its push token and its platform (`android` or `web`), and to remove it again. A person MAY have several devices. A token SHALL belong to one person at a time: registering a token that another person had registered moves it to the new person. Registering the same token twice SHALL leave one registration.

#### Scenario: Two devices
- **WHEN** `alice` registers an Android token and a web token
- **THEN** both are kept and both receive her notifications

#### Scenario: Same token twice
- **WHEN** `alice` registers the same token twice
- **THEN** the server holds one registration for it

#### Scenario: A device changes hands
- **WHEN** `bob` registers a token that `alice` had registered
- **THEN** the token belongs to `bob` only and `alice` no longer receives notifications on it

#### Scenario: Removing a device
- **WHEN** `alice` removes a token she registered
- **THEN** no further notification is sent to it

#### Scenario: Not signed in
- **WHEN** a request to register or remove a device carries no valid sign-in
- **THEN** it is refused with 401

### Requirement: Announcement of a group
The server SHALL send an announcement to a person when a group they take part in becomes visible to them: to every participant other than the actor when the owner moves the group out of Draft, and to a person the owner adds to a group that is already Pending. A group still in Draft SHALL notify nobody. The text SHALL give the group's name.

#### Scenario: Group leaves Draft
- **WHEN** the owner moves a group of `alice`, `bob` and `carol` out of Draft
- **THEN** each of the three is sent an announcement naming the group

#### Scenario: Added to a pending group
- **WHEN** the owner adds `dave` to a Pending group
- **THEN** `dave` is sent an announcement naming the group

#### Scenario: Only the new ones
- **WHEN** the owner saves a Pending group's participant list again with one new person
- **THEN** only the new person is sent an announcement

#### Scenario: Draft is silent
- **WHEN** the owner adds or removes participants of a group that is still in Draft
- **THEN** nobody is notified

#### Scenario: Not the actor
- **WHEN** an owner who is also a participant moves the group out of Draft
- **THEN** they are not sent an announcement for their own action

### Requirement: Announcement of the draw
When a group's names are drawn the server SHALL send an announcement to every participant other than the one who made the change. The text SHALL give the group's name and say that the names have been drawn, and SHALL NOT name who anyone's receiver or Santa is.

#### Scenario: Drawn
- **WHEN** a group of four is drawn
- **THEN** every participant is sent an announcement that the names have been drawn, and none names a receiver

#### Scenario: Not the actor
- **WHEN** an owner who is also a participant draws the group
- **THEN** they are not sent an announcement for their own action

### Requirement: Notifications keep the Santa secret
No notification SHALL contain the identity of a participant's Santa or receiver, the text of a question, an answer or a letter, the contents of a wishlist item, the kind of change that happened or the name of a group other than in an announcement about that group, because notifications can be read on a locked screen. The same wording SHALL be used whichever participant the notification is for.

#### Scenario: A question
- **WHEN** a question is asked and the daily digest is sent
- **THEN** the notification says only that some things changed, without the Santa's name, without the question and without saying a question came in

#### Scenario: A letter
- **WHEN** a letter titled "Thank you" with the text "For the scarf" is sent and the daily digest is sent
- **THEN** the notification contains neither the title nor the text, and does not say a letter came in

#### Scenario: A wishlist item
- **WHEN** an item titled "Blue scarf" is added and the daily digest is sent
- **THEN** the notification does not contain "Blue scarf"

### Requirement: Opening a notification goes to its group
Every notification about a group SHALL carry the identifier of the group it is about and its category, so that the app can open that group when the notification is tapped. A notification for an announcement the owner sent SHALL also say that it is a message from the owner, so that it can be told from the other notifications of the announcements category, which are about being added to a group and the names being drawn. The daily digest is about no group: it SHALL carry the category `daily` and no group.

#### Scenario: Tap target
- **WHEN** a notification that a person was added to group 7 is delivered
- **THEN** it carries group 7 and the category "announcements"

#### Scenario: An announcement from the owner
- **WHEN** the owner's announcement is sent as a push notification for group 7
- **THEN** it carries group 7, the category "announcements" and the mark that it is a message from the owner

#### Scenario: Added to a group
- **WHEN** a person is told they were added to group 7, or that its names were drawn
- **THEN** the notification carries group 7 and the category "announcements" and does not carry the mark

#### Scenario: The daily digest
- **WHEN** the daily digest is delivered
- **THEN** it carries the category "daily" and no group

### Requirement: Delivery never affects the action
A notification SHALL be sent only after the action that caused it has been stored. Failing to send, including the push service being unreachable, refusing the request or timing out, SHALL NOT undo or fail that action, SHALL be logged without the service-account key, push tokens or message text, and SHALL NOT be retried by a later request. An action that fails SHALL send nothing.

#### Scenario: Push service down
- **WHEN** the push service cannot be reached while a question is asked
- **THEN** the question is stored and the response is a success

#### Scenario: Refused action
- **WHEN** a question is refused because the group is not Active
- **THEN** no notification is sent

### Requirement: Dead devices are forgotten
When the push service reports that a token is no longer valid, the server SHALL remove that registration and keep sending to the person's other devices.

#### Scenario: Uninstalled app
- **WHEN** the push service answers that one of `alice`'s tokens is unregistered
- **THEN** that token is removed and her other tokens still receive the notification

### Requirement: Push is optional and its key stays secret
Push notifications SHALL be on only when a Firebase service-account key is configured, and the server SHALL start and work as before without it. The key SHALL NOT be logged, returned by the API or served to browsers. The server SHALL tell a signed-in client whether push is available, and with it off, registering a device and changing switches SHALL still succeed but nothing is sent.

#### Scenario: Not configured
- **WHEN** no service-account key is configured
- **THEN** the server starts, every action works, nothing is sent and the clients are told that push is not available

#### Scenario: Unreadable key
- **WHEN** the configured key file is missing or is not a service-account key
- **THEN** the server refuses to start and the message names the setting

#### Scenario: The key is not exposed
- **WHEN** the server logs its configuration or answers any request
- **THEN** the key's contents do not appear

### Requirement: Changes wait for the daily digest
When a participant of an Active group adds, edits or deletes a wishlist item, their Santa SHALL be marked as having news. When a Santa asks a question their receiver SHALL be marked, and when a receiver answers a question or sends a letter their Santa SHALL be marked. Reordering a wishlist, a change in a group that is not Active, and a person's own actions SHALL mark nobody.

#### Scenario: New item
- **WHEN** `bob`'s Santa is `alice` and `bob` adds an item in an Active group
- **THEN** `alice` is marked and nothing is sent yet

#### Scenario: A question
- **WHEN** `alice`, Santa of `bob`, asks a question in an Active group
- **THEN** `bob` is marked

#### Scenario: An answer
- **WHEN** `bob` answers a question of `alice`
- **THEN** `alice` is marked

#### Scenario: A letter
- **WHEN** `bob` sends a letter to his Santa `alice`
- **THEN** `alice` is marked

#### Scenario: Deleting an item
- **WHEN** `bob` deletes a wishlist item in an Active group
- **THEN** `alice` is marked

#### Scenario: Reorder
- **WHEN** `bob` only changes the order of his items
- **THEN** nobody is marked

#### Scenario: Not drawn yet
- **WHEN** a participant edits their wishlist in a Pending group
- **THEN** nobody is marked

### Requirement: Marks are one per person and are kept
A mark SHALL be one per person, whatever the group, the kind or the number of changes, and SHALL be kept in the database so that a server restart does not lose it. Marking SHALL NOT send anything and SHALL NOT affect the action that caused it.

#### Scenario: Many changes
- **WHEN** `bob` adds three items, answers two questions and sends a letter in two groups the same day
- **THEN** `alice` has one mark

#### Scenario: A restart
- **WHEN** the server restarts after `alice` was marked and before 20:00
- **THEN** `alice` is still marked

#### Scenario: Marking never fails the action
- **WHEN** a mark cannot be stored while `bob` adds an item
- **THEN** the item is stored and the response is a success

### Requirement: The daily digest
At 20:00 every day in Bucharest local time (`Europe/Bucharest`, with its summer time) the server SHALL send each marked person one push notification to every device of theirs, if they have the Daily digest switch on, and SHALL then clear every mark, whether or not anything was sent. A person who is not marked SHALL be sent nothing. A server that is not running at 20:00 SHALL send no digest for that day; the marks stay for the next one.

#### Scenario: Something happened
- **WHEN** `alice` was marked during the day and has Daily digest on
- **THEN** at 20:00 she is sent one notification on each of her devices, and she is no longer marked

#### Scenario: Nothing happened
- **WHEN** nothing marked `carol` since the last digest
- **THEN** she is sent nothing

#### Scenario: Several groups
- **WHEN** `alice` is marked for changes in three groups
- **THEN** she is sent one notification, with no group

#### Scenario: Switched off
- **WHEN** `alice` is marked and has turned Daily digest off
- **THEN** she is sent nothing, and the mark is cleared

#### Scenario: Summer time
- **WHEN** the clocks change in Bucharest
- **THEN** the digest is still sent at 20:00 Bucharest time, which is 17:00 UTC in summer and 18:00 UTC in winter

#### Scenario: Push off
- **WHEN** no push service is configured at 20:00
- **THEN** nothing is sent and the marks are cleared

#### Scenario: Missed digest
- **WHEN** the server was stopped at 20:00
- **THEN** no digest is sent for that day, and the marks that were stored are sent at the next 20:00

#### Scenario: Another day
- **WHEN** a person is marked at 20:05 and is not marked again
- **THEN** they are sent the digest at 20:00 the next day

### Requirement: The digest's words
The daily digest SHALL read "Some things changed in your groups" under the app's name, SHALL have the category `daily` and SHALL carry no group.

#### Scenario: The words
- **WHEN** a digest is sent
- **THEN** it reads "Some things changed in your groups", has the category "daily" and names no group

### Requirement: Two notification categories
Each person SHALL have two switches that decide what they are notified about: Announcements and Daily digest. Each SHALL be on until the person turns it off. The person SHALL be able to read and change them, and a change SHALL apply to every device of that person and to every notification sent after it.

#### Scenario: Defaults
- **WHEN** a person who has never changed them reads their switches
- **THEN** both are on

#### Scenario: Turn daily off
- **WHEN** `alice` turns "Daily digest" off
- **THEN** she is sent no digest, while announcements still reach her

#### Scenario: Turn announcements off
- **WHEN** `alice` turns "Announcements" off
- **THEN** announcements send her nothing and the digest still does

#### Scenario: Shared by devices
- **WHEN** `alice` turns "Daily digest" off from her phone
- **THEN** her browser also receives no digest

#### Scenario: Unknown or missing values
- **WHEN** a change names a switch that does not exist, or gives a value that is not true or false
- **THEN** it is refused with 400 and no switch changes

### Requirement: Older apps keep their four switches
For apps that predate the Daily switch the server SHALL accept and report the four older switches (wishlist, questions, answers, letters): it reports each of them with the value of Daily digest, and a request that gives them and no Daily value SHALL set Daily digest on when any of the four is on and off when all four are off. Choices saved before the Daily switch existed SHALL read the same way.

#### Scenario: Earlier choices, some on
- **WHEN** a person who had turned "questions" off and kept the other three on is read after the update
- **THEN** Daily digest is on

#### Scenario: Earlier choices, all off
- **WHEN** a person who had turned wishlist, questions, answers and letters all off is read after the update
- **THEN** Daily digest is off

#### Scenario: An older client
- **WHEN** a request gives announcements, wishlist, questions, answers and letters and no Daily value, with wishlist on
- **THEN** it is accepted and Daily digest is on, and the answer reports the four older switches as on

#### Scenario: An older client turns everything off
- **WHEN** a request gives wishlist, questions, answers and letters all off and no Daily value
- **THEN** Daily digest is off

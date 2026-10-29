# push-notifications Specification

## Purpose
Tells people, by push notification on their Android and web devices, when something happens to them in a group: they are added to one, its names are drawn, their receiver's wishlist changes, their Santa asks a question or their receiver answers. Each person chooses which of those four kinds they receive.

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

### Requirement: Four notification categories
Each person SHALL have four switches that decide what they are notified about: announcements, wishlist changes, questions and answers. Each SHALL be on until the person turns it off. The person SHALL be able to read and change them, and a change SHALL apply to every device of that person and to every notification sent after it.

#### Scenario: Defaults
- **WHEN** a person who has never changed them reads their switches
- **THEN** all four are on

#### Scenario: Turn one off
- **WHEN** `alice` turns "questions" off
- **THEN** a question for her sends her nothing, while her announcements, wishlist changes and answers still do

#### Scenario: Shared by devices
- **WHEN** `alice` turns "answers" off from her phone
- **THEN** her browser also receives no answer notification

#### Scenario: Unknown or missing values
- **WHEN** a change names a switch that does not exist, or gives a value that is not true or false
- **THEN** it is refused with 400 and no switch changes

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

### Requirement: Wishlist changes
When a participant of an Active group adds, edits or deletes a wishlist item, the server SHALL send a wishlist notification to the person who is their Santa. Reordering SHALL NOT notify. Changes that follow one another closely SHALL result in one notification, not one per change. A change in a group that has not been drawn SHALL notify nobody.

#### Scenario: New item
- **WHEN** `bob`'s Santa is `alice` and `bob` adds an item to his wishlist in an Active group
- **THEN** `alice` is sent a wishlist notification naming the group

#### Scenario: Several quick edits
- **WHEN** `bob` adds three items within a minute
- **THEN** `alice` is sent one notification, not three

#### Scenario: Reorder
- **WHEN** `bob` only changes the order of his items
- **THEN** nobody is notified

#### Scenario: Not drawn yet
- **WHEN** a participant edits their wishlist in a Pending group
- **THEN** nobody is notified

### Requirement: Questions and answers
When a Santa asks a question the server SHALL send a question notification to their receiver, and when a receiver answers one it SHALL send an answer notification to their Santa. Each SHALL name the group.

#### Scenario: Question
- **WHEN** `alice`, Santa of `bob`, asks a question
- **THEN** `bob` is sent a question notification naming the group

#### Scenario: Answer
- **WHEN** `bob` answers `alice`'s question
- **THEN** `alice` is sent an answer notification naming the group

#### Scenario: Switched off
- **WHEN** `bob` has turned "questions" off and `alice` asks him a question
- **THEN** the question is stored as usual and no notification is sent

### Requirement: Notifications keep the Santa secret
No notification SHALL contain the identity of a participant's Santa, the text of a question or an answer, or the contents of a wishlist item, because notifications can be read on a locked screen. The same wording SHALL be used whichever participant the notification is for.

#### Scenario: A question
- **WHEN** a question is sent
- **THEN** the notification says only that a question came in for the group, without the Santa's name and without the question

#### Scenario: A wishlist item
- **WHEN** an item titled "Blue scarf" is added
- **THEN** the notification does not contain "Blue scarf"

### Requirement: Opening a notification goes to its group
Every notification SHALL carry the identifier of the group it is about and its category, so that the app can open that group when the notification is tapped.

#### Scenario: Tap target
- **WHEN** a question notification for group 7 is delivered
- **THEN** it carries group 7 and the category "questions"

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

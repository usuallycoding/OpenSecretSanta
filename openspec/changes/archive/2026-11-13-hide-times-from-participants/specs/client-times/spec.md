# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Participants see when things were made`
- TO: `### Requirement: Only the Admin view shows when things were made`

## MODIFIED Requirements

### Requirement: Times are shown in Bucharest time
The app SHALL show every time it displays for something added, changed, asked, answered or sent in Bucharest local time (`Europe/Bucharest`, with its summer time), as day, abbreviated month, year, a comma, and the 24-hour clock, for example `1 Dec 2026, 12:00`, with no zone suffix, whatever time zone the device is set to. A time the app cannot read SHALL be shown as it came.

#### Scenario: Winter time
- **WHEN** the server sends `2026-12-01T10:00:00Z`
- **THEN** the app shows `1 Dec 2026, 12:00`

#### Scenario: Summer time
- **WHEN** the server sends `2026-07-01T10:00:00Z`
- **THEN** the app shows `1 Jul 2026, 13:00`

#### Scenario: Clocks change
- **WHEN** the server sends `2026-03-29T00:59:00Z` and then `2026-03-29T01:00:00Z`, or `2026-10-25T00:59:00Z` and then `2026-10-25T01:00:00Z`
- **THEN** the app shows `29 Mar 2026, 02:59` and `29 Mar 2026, 04:00`, then `25 Oct 2026, 03:59` and `25 Oct 2026, 03:00`

#### Scenario: Next day
- **WHEN** the server sends `2026-12-31T22:30:00Z`
- **THEN** the app shows `1 Jan 2027, 00:30`

#### Scenario: Another time zone on the device
- **WHEN** the device is set to another time zone
- **THEN** the times shown are the same Bucharest times

#### Scenario: Unreadable time
- **WHEN** the value is not a time the app can read
- **THEN** it is shown as it came

### Requirement: Only the Admin view shows when things were made
The app SHALL show, in one small line under each item, question or letter, when it was made only in the Admin view, on the owner's sheets: on a participant's wishlist items "Added" and, when edited, "Changed"; on every question of a pair's thread "Asked" and, under an answer, "Answered" and, when the answer was edited, "Answer changed"; on each letter of a pair "Sent". The Participant view SHALL show none of these lines, for anyone, including an administrator who has switched to it. The Announcements page SHALL keep its "Sent" time in both views.

#### Scenario: Own item
- **WHEN** a participant reads an item they added and never edited
- **THEN** it shows no time

#### Scenario: Own item edited
- **WHEN** a participant reads an item they edited after adding it
- **THEN** it shows no time for the addition or the change

#### Scenario: The receiver's list
- **WHEN** the Gifter page shows the receiver's wishlist
- **THEN** no item shows when it was added or changed

#### Scenario: A question
- **WHEN** a question was asked and has not been answered
- **THEN** it shows no time on the Gifter and the Receiver page

#### Scenario: An answer
- **WHEN** a question was answered and the answer edited later
- **THEN** it shows no time for the question, the answer or the change

#### Scenario: Letters
- **WHEN** the person opens the Send letter page or the Inbox
- **THEN** no letter card shows when it was sent

#### Scenario: The owner's sheets
- **WHEN** the owner, in the Admin view, reads a participant's wishlist, a pair's thread or a pair's letters
- **THEN** the same lines are shown in Bucharest time, without "UTC"

#### Scenario: An administrator as a participant
- **WHEN** an administrator switches to the Participant view and reads a wishlist, a thread or a letter
- **THEN** no time line is shown, as for any participant

#### Scenario: Announcements
- **WHEN** any person opens the Announcements page
- **THEN** each announcement still shows when it was sent

#### Scenario: Still the same behaviour
- **WHEN** the person taps an item or reads a cut note
- **THEN** it behaves as before

#### Scenario: Read marks are unaffected
- **WHEN** an item, answer or letter is new or edited for the Santa
- **THEN** its "New" or "Edited" pill and accent bar are shown as before, with no time beside them

### Requirement: The owner sees the same times
The owner's wishlist, thread and letters sheets in the Admin view SHALL show the times in the same way, and SHALL show them whatever the participants see.

#### Scenario: Thread sheet
- **WHEN** the owner opens a pair sheet
- **THEN** each question shows when it was asked and, when answered, when it was answered

#### Scenario: Participants' view is not shared
- **WHEN** the owner opens a sheet while participants see no times
- **THEN** the owner still sees them

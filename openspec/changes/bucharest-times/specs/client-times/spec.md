# client-times Specification

## Purpose
How the app shows the times at which wishlist items and questions were made and changed: one zone and one format for everyone, and the same times for participants as for the owner.

## ADDED Requirements

### Requirement: Times are shown in Bucharest time
The app SHALL show every time at which something was added, changed, asked or answered in Bucharest local time (`Europe/Bucharest`, with its summer time), as day, abbreviated month, year, a comma, and the 24-hour clock, for example `1 Dec 2026, 12:00`, with no zone suffix, whatever time zone the device is set to. A time the app cannot read SHALL be shown as it came.

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

### Requirement: Participants see when things were made
A participant SHALL see, in one small line under each item or question, when it was made: on their own wishlist items "Added" and, when the item was edited after it was added, "Changed"; on the receiver's wishlist on the Gifter page the same two; on every question on the Gifter and Receiver pages "Asked" and, under an answer, "Answered" and, when the answer was edited after it was first given, "Answer changed". The owner's wishlist and thread sheets SHALL show the same times in the same way. These lines SHALL NOT be tappable and SHALL NOT change what tapping the item does.

#### Scenario: Own item
- **WHEN** the person added an item and never edited it
- **THEN** it shows "Added" with the time and nothing about a change

#### Scenario: Own item edited
- **WHEN** the person edited an item after adding it
- **THEN** it shows both "Added" and "Changed" with their times

#### Scenario: The receiver's list
- **WHEN** the Gifter page shows the receiver's wishlist
- **THEN** each item shows when it was added and, if edited, changed

#### Scenario: A question
- **WHEN** a question was asked and has not been answered
- **THEN** it shows "Asked" with the time on both the Gifter and the Receiver page, and no answer time

#### Scenario: An answer
- **WHEN** a question was answered and the answer edited later
- **THEN** it shows "Asked", "Answered" and "Answer changed" with their times

#### Scenario: The owner's sheets
- **WHEN** the owner reads a participant's wishlist or a pair's thread
- **THEN** the same lines are shown in Bucharest time, without "UTC"

#### Scenario: Still the same behaviour
- **WHEN** the person taps an item or reads a cut note
- **THEN** it behaves as before; the time line does nothing

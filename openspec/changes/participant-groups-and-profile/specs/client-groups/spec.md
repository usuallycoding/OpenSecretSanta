# Spec Delta

## ADDED Requirements

### Requirement: Groups tab
The participant view SHALL have a Groups tab that lists the groups the signed-in person participates in, as returned by `GET /groups`, in the order the server returns them. Each entry SHALL show the group's name and state and, when present, its date and time and its location, and the number of participants.

#### Scenario: Listed groups
- **WHEN** the person opens the Groups tab and is in two groups
- **THEN** both are listed with their name, state, date, time, location and number of participants

#### Scenario: Missing details are left out
- **WHEN** a group has no date, time or location
- **THEN** the entry shows only what the group has

#### Scenario: No groups
- **WHEN** the person is in no group
- **THEN** the tab explains that an organiser adds people to groups by username instead of showing an empty list

#### Scenario: Draft groups
- **WHEN** an organiser has named the person only in a group that is still a Draft
- **THEN** that group is not shown, because the server does not return it

### Requirement: Group states
The app SHALL show a group's state as Pending, Active or Archived, with a short note on the detail screen saying what it means: Pending, that the organiser has not drawn the names yet; Active, that the names have been drawn; Archived, that the exchange is over. No note and no other part of the groups screens SHALL say or imply who gives to whom.

#### Scenario: Pending group
- **WHEN** a group is Pending
- **THEN** it is labelled Pending and its note says the names have not been drawn yet

#### Scenario: Active group
- **WHEN** a group is Active
- **THEN** it is labelled Active and its note says the names have been drawn, without naming anyone

#### Scenario: Archived group
- **WHEN** a group is Archived
- **THEN** it is labelled Archived and its note says the exchange is over

### Requirement: Group detail
Choosing a group SHALL open its detail screen, loaded with `GET /groups/{id}`, showing its name, state and state note, description, date, time, location and the participants' usernames, with the person's own username marked. Parts the group does not have SHALL be left out.

#### Scenario: Opening a group
- **WHEN** the person chooses a group in the list
- **THEN** the detail screen shows its details and every participant's username, with their own marked "you"

#### Scenario: Back to the list
- **WHEN** the person goes back from the detail screen
- **THEN** the list is shown again

#### Scenario: Group no longer available
- **WHEN** the server answers that the group does not exist, for example because the person was removed from it
- **THEN** the screen says the group is no longer available and offers to go back

### Requirement: Dates and times are shown as the group has them
The app SHALL show a group's date and time as the local values the organiser entered, formatted for reading (for example "24 Dec 2026, 18:30"), without converting them to another timezone.

#### Scenario: Date and time
- **WHEN** a group has the date `2026-12-24` and the time `18:30`
- **THEN** the app shows "24 Dec 2026, 18:30" on every platform and in every timezone

#### Scenario: Date only
- **WHEN** a group has a date but no time
- **THEN** only the date is shown

### Requirement: Loading, errors and refresh
While a groups screen loads, the app SHALL show a progress indicator. When the server cannot be reached or answers with an error, the app SHALL show a message that says so, without technical details, with an action to try again. The groups screens SHALL offer a refresh action and SHALL reload when the app returns to the foreground, keeping what is shown while the new answer loads.

#### Scenario: Loading
- **WHEN** a groups screen is opened
- **THEN** a progress indicator is shown until the answer arrives

#### Scenario: Server unreachable
- **WHEN** the groups cannot be loaded because the server cannot be reached
- **THEN** the screen says so and offers "Try again", which loads them again

#### Scenario: Refresh
- **WHEN** the person refreshes the list after an organiser has activated a group
- **THEN** the list shows the new state

#### Scenario: Return to the app
- **WHEN** the app returns to the foreground
- **THEN** the visible groups screen reloads without clearing what it shows

#### Scenario: Session ended
- **WHEN** the server answers 401 while a groups screen loads
- **THEN** the app handles it as for any request: the sign-in screen is shown with the note that the person was signed out

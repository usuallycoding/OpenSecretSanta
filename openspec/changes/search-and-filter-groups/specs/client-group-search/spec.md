# Spec Delta

## Purpose
How a person narrows the groups lists: a search field over a group's name and description and state chips under it, on the participant's Groups tab and on the administrator's groups list, always keeping the order the server gives.

## ADDED Requirements

### Requirement: Search field
The participant's Groups tab and the administrator's groups list SHALL each have a search field above the list, shown once the groups have loaded and there is at least one. While the field holds text it SHALL have a control that empties it. An empty or blank search SHALL show every group.

#### Scenario: Shown with groups
- **WHEN** the groups have loaded and there is at least one
- **THEN** the search field is shown above the list

#### Scenario: Blank search
- **WHEN** the search holds only spaces
- **THEN** every group is listed

#### Scenario: Clearing
- **WHEN** the search field holds text
- **THEN** it has a control that empties it

### Requirement: What the search matches
The list SHALL show only the groups whose name or description matches what is typed, ignoring upper and lower case. With several words, a group SHALL match when every word is found in its name or description, in any order. A group's date, time, location and participants SHALL NOT be searched.

#### Scenario: Matches the name
- **WHEN** the person types "party" and one group is called "Office Party"
- **THEN** that group is listed

#### Scenario: Matches the description
- **WHEN** the person types "snacks" and a group's description is "Bring snacks"
- **THEN** that group is listed

#### Scenario: Case is ignored
- **WHEN** the person types "OFFICE"
- **THEN** "Office Party" is listed

#### Scenario: Several words
- **WHEN** the person types "party snacks" and a group is called "Office Party" with the description "Bring snacks"
- **THEN** that group is listed, and a group that has only "party" is not

#### Scenario: Words in any order
- **WHEN** the person types "office party" and a group is called "Party at the Office"
- **THEN** that group is listed

#### Scenario: Other details are not searched
- **WHEN** the person types "Town Hall" and only a group's location is "Town Hall"
- **THEN** that group is not listed

### Requirement: State chips
Under the search field there SHALL be one chip for each state a group can be shown in: Draft, Pending, Active and Archived on the administrator's list, and Pending, Active and Archived on the participant's. Each chip SHALL be switched on and off by choosing it, any number of them MAY be on at once, and with none on every state SHALL be shown. The list SHALL show only groups in a state whose chip is on.

#### Scenario: Administrator's chips
- **WHEN** the administrator opens their groups list
- **THEN** the chips are Draft, Pending, Active and Archived

#### Scenario: Participant's chips
- **WHEN** a participant opens the Groups tab
- **THEN** the chips are Pending, Active and Archived, with no Draft

#### Scenario: One chip
- **WHEN** the Active chip is switched on
- **THEN** only Active groups are listed

#### Scenario: Several chips
- **WHEN** the Pending and Active chips are both on
- **THEN** Pending and Active groups are listed and Archived ones are not

#### Scenario: Switching off
- **WHEN** the only chip that was on is chosen again
- **THEN** no chip is on and every group is listed

### Requirement: Search and chips work together
A group SHALL be listed only when it matches the search and its state is allowed by the chips. The groups listed SHALL keep the order the server returned them in.

#### Scenario: Both narrow the list
- **WHEN** the person types "party" and switches the Active chip on
- **THEN** only Active groups with "party" in their name or description are listed

#### Scenario: Newest first stays
- **WHEN** a search matches an older and a newer group
- **THEN** the newer one is listed first

### Requirement: When nothing matches
When groups exist but the search and the chips match none, the list SHALL say that no group matches and offer a way to clear both, and the search field and chips SHALL stay where they are. This SHALL NOT be shown as the message for a person who has no groups at all.

#### Scenario: No match
- **WHEN** the person types a word that no group contains
- **THEN** the list says that no group matches, offers to clear the search and chips, and the search field is still there

#### Scenario: Clearing
- **WHEN** the person chooses to clear
- **THEN** the search is empty, every chip is off and every group is listed

#### Scenario: No groups at all
- **WHEN** a person has no group
- **THEN** the screen shows its usual message for that, with no search field and no chips

### Requirement: The choices are kept while the person uses the app
The search and the chips SHALL be kept while the list reloads and while the person moves to another screen or tab and back, and SHALL start empty each time the person signs in. They SHALL NOT be kept across a restart of the app.

#### Scenario: Reload
- **WHEN** the person has searched and the list reloads, by pulling it down or by returning to the app
- **THEN** the search and the chips are as they were and the reloaded groups are filtered by them

#### Scenario: Another tab
- **WHEN** the person searches, opens a group, and comes back
- **THEN** the search and the chips are as they were

#### Scenario: New sign-in
- **WHEN** the person signs out and in again
- **THEN** the search is empty and no chip is on

### Requirement: Groups in a state without a chip
A group in a state that has no chip SHALL be listed when no chip is on, and SHALL NOT be listed when any chip is on.

#### Scenario: Unknown state
- **WHEN** a group is in a state the app has no chip for
- **THEN** it is listed with no chip on and hidden as soon as any chip is on

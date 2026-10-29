# Spec Delta

## ADDED Requirements

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

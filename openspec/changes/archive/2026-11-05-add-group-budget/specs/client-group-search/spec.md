# Spec Delta

## MODIFIED Requirements

### Requirement: What the search matches
The list SHALL show only the groups whose name or description matches what is typed, ignoring upper and lower case. With several words, a group SHALL match when every word is found in its name or description, in any order. A group's date, time, location, budget, currency and participants SHALL NOT be searched.

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

#### Scenario: Budget is not searched
- **WHEN** the person types "100" and only a group's budget is 100
- **THEN** that group is not listed


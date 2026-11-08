# Spec Delta

## ADDED Requirements

### Requirement: Announcement badges in the sidebar
In the participant sidebar each group's line SHALL carry a badge with the number of announcements the person has not seen, and, for the group that is open, its Overview item SHALL carry it instead of the group's line. The number SHALL be the one `GET /groups` gives, with "9+" above 9, and nothing SHALL be badged for none or while it is not known. A screen reader SHALL hear the count in the name, for example "3 new announcements". The Groups item SHALL NOT carry a total.

#### Scenario: A collapsed group
- **WHEN** a group that is not open has two announcements the person has not seen
- **THEN** its line in the sidebar carries a badge with 2

#### Scenario: The open group
- **WHEN** the open group has two announcements the person has not seen
- **THEN** its Overview item carries a badge with 2 and its own line carries none

#### Scenario: Nothing new
- **WHEN** a group has no new announcement
- **THEN** neither its line nor its Overview item is badged

#### Scenario: After reading
- **WHEN** the person reads the announcements in the pane and closes it
- **THEN** the badge is gone from the group's Overview item and from its line

#### Scenario: No total
- **WHEN** several groups have new announcements
- **THEN** the Groups item carries no badge

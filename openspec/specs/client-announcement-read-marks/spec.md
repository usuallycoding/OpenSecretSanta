# client-announcement-read-marks Specification

## Purpose
What a participant sees in the app of the announcements they have not read yet: a badge on the Announcements bell, a pill and an accent bar on each new announcement, and what looking at them clears.

## Requirements

### Requirement: A badge on the Announcements bell
In a Pending, Active or Archived group the bell of the group page's information card SHALL carry a badge on its corner, in the theme's tertiary colour, with the number of announcements the person has not seen, as the group's data says it: the number, "9+" above 9, and no badge for none or while the group is not loaded. A screen reader SHALL hear the count with the button's name, for example "Announcements, 3 new". The bell of the owner's admin screen SHALL carry no badge.

#### Scenario: New announcements
- **WHEN** the owner has sent three announcements the person has not seen
- **THEN** the bell has a badge with 3 and is read out as "Announcements, 3 new"

#### Scenario: Nothing new
- **WHEN** the person has seen every announcement
- **THEN** the bell has no badge

#### Scenario: More than nine
- **WHEN** twelve announcements are new
- **THEN** the badge says "9+"

#### Scenario: The owner's screen
- **WHEN** the owner opens their group on the admin screen
- **THEN** the bell has no badge

### Requirement: New announcements are marked on the page
While the Announcements page or pane of a participant is open, an announcement sent after the time the person had seen when the page was opened SHALL carry a "New" pill and an accent bar along its card's start edge, as a new letter does. A person who never looked SHALL see every announcement marked, and one whose time could not be read none. The owner's page SHALL mark nothing.

#### Scenario: Some are new
- **WHEN** the person had seen the first of three announcements and opens the page
- **THEN** the second and the third carry the pill and the bar and the first does not

#### Scenario: Never looked
- **WHEN** a person who has never opened the page opens it
- **THEN** every announcement is marked

#### Scenario: The time cannot be read
- **WHEN** the server cannot be asked for the time seen
- **THEN** no announcement is marked and the page shows all of them as usual

#### Scenario: The owner
- **WHEN** the owner reads the announcements
- **THEN** none is marked and the page asks for no time

### Requirement: The time seen is asked for once per visit
The Announcements page of a participant SHALL ask for the time the person had seen with `GET /groups/{id}/announcements/reads` when it first loads, and not again while it stays open, so that a reload keeps the marks and an announcement that arrives while it is open is marked as well. The next visit SHALL ask again.

#### Scenario: Marks stay while it is open
- **WHEN** the person pulls the page down to reload it while it is open
- **THEN** the same announcements are still marked, and one that arrived since is marked as well

#### Scenario: A new visit
- **WHEN** the person closes the page and opens it again
- **THEN** the page asks for the time seen again

### Requirement: The page tells the server what it showed
Once the Announcements page or pane of a participant is on screen with its announcements, the app SHALL tell the server, with `PUT /groups/{id}/announcements/reads`, that everything up to the newest announcement shown has been seen, once for each newest time and not while the time seen could not be read. A failed mark SHALL change nothing, so that the marks come back next time.

#### Scenario: Marked after showing
- **WHEN** the person opens the page and the announcements are shown
- **THEN** the newest one's time is sent as seen

#### Scenario: Not twice
- **WHEN** the page reloads and the newest announcement is the same
- **THEN** nothing more is sent

#### Scenario: A failed mark
- **WHEN** telling the server fails
- **THEN** the announcements are still new, and the page marks them again the next time it is shown

### Requirement: Looking clears the marks for the next time
The pill and the bar SHALL stay while the Announcements page is open and be gone the next time it is opened. The badges on the bell, on the group's entry in the list and in the sidebar SHALL be gone once the person has left the page or closed the pane and the group's data has been loaded again.

#### Scenario: Opened again
- **WHEN** the person closes the page and opens it again with nothing new in between
- **THEN** no announcement carries the pill or the bar

#### Scenario: Badges after leaving
- **WHEN** the person goes back from the page to the group's page, or closes the pane
- **THEN** the bell, the group's entry in the list and the sidebar no longer show the announcements as new

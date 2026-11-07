## Purpose

Defines the owner's draw rules page in the app: how it is reached, what it lists, how rules are added and removed in a Pending group, how it reads in Active and Archived groups, and how it sits on a wide window.

## ADDED Requirements

### Requirement: The rules page lists the rules
The rules page SHALL list every rule of the group, each as a sentence in words, "<giver> must give to <receiver>" or "<giver> must not give to <receiver>", with musts before nevers and each group sorted by giver. A group with no rules SHALL show a line saying so and what rules are for.

#### Scenario: A list
- **WHEN** the admin opens the page of a group with one must and one never
- **THEN** the page shows "ana must give to bo" above "cy must not give to di"

#### Scenario: No rules
- **WHEN** the admin opens the page of a Pending group with no rules
- **THEN** the page says there are no rules yet and explains that rules fix or forbid who gives to whom in the draw

### Requirement: The rules page is a page, or a pane on a wide window
On a narrow window the gavel button SHALL open the rules page as a new page with a back action. On a wide window it SHALL open the same content in the pane beside the group page, like a participant's wishlist, with a close action, leaving the group page where it was. Opening a participant's wishlist or a pair while the rules pane is open SHALL replace it.

#### Scenario: Narrow window
- **WHEN** the admin taps the gavel on a phone-width window
- **THEN** the rules page opens as a page and back returns to the group page

#### Scenario: Wide window
- **WHEN** the admin taps the gavel on a wide window
- **THEN** the rules show in the pane to the right of the group page

#### Scenario: Replaced by another pane
- **WHEN** the rules pane is open and the admin opens a participant's wishlist
- **THEN** the pane shows the wishlist instead

### Requirement: Adding a rule
In a Pending group the page SHALL offer an add form with a choice of giver and of receiver from the group's participants, a choice between "Must give to" and "Must not give to", and a "Both ways" toggle, off by default. Adding SHALL be possible only when a giver and a receiver who differ are chosen. A successful add SHALL show the new rule or rules in the list and reset the form.

#### Scenario: A one-way rule
- **WHEN** the admin chooses `ana`, "Must give to", `bo` and adds
- **THEN** the list shows "ana must give to bo"

#### Scenario: Both ways
- **WHEN** the admin chooses `cy`, "Must not give to", `di`, turns on "Both ways" and adds
- **THEN** the list shows "cy must not give to di" and "di must not give to cy" as two rules

#### Scenario: Same person
- **WHEN** the admin has chosen the same person as giver and receiver
- **THEN** the add button is disabled

#### Scenario: The server refuses
- **WHEN** the server answers `rules_conflict`
- **THEN** the page shows the server's message naming the conflict, nothing is added and the form keeps what was chosen

### Requirement: Deleting a rule
In a Pending group every rule SHALL have a delete icon button, named "Delete rule" for a screen reader. Deleting SHALL remove the rule from the list without asking again.

#### Scenario: Delete
- **WHEN** the admin deletes "ana must give to bo"
- **THEN** the rule is gone from the list and from the server

### Requirement: Read-only after Pending
In an Active or Archived group the page SHALL show the rules but no add form and no delete buttons, and SHALL say that the rules are fixed because the names were drawn.

#### Scenario: Active group
- **WHEN** the admin opens the rules of an Active group
- **THEN** the rules are listed, there is no add form and no delete button, and a line says they are fixed

### Requirement: Few possible draws warn
When the server reports few possible draws, the page SHALL show a warning that the rules leave little room and that a participant who knows them could work out who gives to whom.

#### Scenario: Warning
- **WHEN** the rules leave few possible draws
- **THEN** the page shows the warning above the list

### Requirement: The owner who takes part is told
When the owner of the group is also a participant, the page SHALL say that the rules show part of the draw to them.

#### Scenario: Owner who takes part
- **WHEN** an admin who participates in the group opens the rules
- **THEN** the page shows the notice

#### Scenario: Owner who does not
- **WHEN** an admin who is not a participant opens the rules
- **THEN** the page shows no such notice

### Requirement: Removing a participant says which rules went
When the admin removes participants from a Pending group and the server reports deleted rules, the app SHALL tell the admin which rules were deleted along with them, in the same sentences the page uses.

#### Scenario: Rules deleted
- **WHEN** the admin saves a participant list without `bo`, and the server deleted "ana must give to bo"
- **THEN** a notice says that rule was deleted because `bo` left the group

### Requirement: Activation refused by the rules
When the server refuses to activate with `rules_conflict`, the app SHALL show the message and keep the group Pending, and SHALL offer a way to open the rules page.

#### Scenario: Refused
- **WHEN** the admin draws the names and the server answers `rules_conflict`
- **THEN** the page shows the message with a button to the rules and the group stays Pending

### Requirement: The gavel is drawn in the app
The gavel icon SHALL be drawn by the app itself, as the other custom icons are, and SHALL follow the content colour of the button it is in.

#### Scenario: Colour
- **WHEN** the gavel button is shown enabled or disabled
- **THEN** the gavel takes the matching colour of the button

### Requirement: Participants see nothing of the rules
No page a participant sees SHALL mention draw rules or show any sign that a group has them.

#### Scenario: Participant pages
- **WHEN** a participant opens a group, the Giving page or the Receiving page
- **THEN** nothing on them refers to rules

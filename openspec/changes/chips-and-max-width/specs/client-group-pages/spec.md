## MODIFIED Requirements

### Requirement: Pair rows
Below the group's details the group page SHALL show, while the group is Pending, one row with the person's own username and a gift icon that opens the Receiver page; and, once the group is Active or Archived, two rows: `my username -> receiver's username`, whose arrow opens the Gifter page, and `Secret Santa -> my username`, whose arrow opens the Receiver page. In each of those rows the name before the arrow SHALL start at the start of the row and the name after the arrow SHALL end at the end of the row. The receiver's username SHALL come from `GET /groups/{id}/receiver`; while it loads the row SHALL show that, and when it fails the row SHALL say so with "Try again" and the rest of the page SHALL stay. A Draft group SHALL show no rows.

#### Scenario: Pending
- **WHEN** the group is Pending
- **THEN** one row shows the person's own username with a gift icon, and no row names a receiver or a Santa

#### Scenario: Active or Archived
- **WHEN** the group is Active or Archived
- **THEN** two rows are shown: `alice -> bob` and `Secret Santa -> alice` for the person `alice` whose receiver is `bob`

#### Scenario: Receiver cannot be loaded
- **WHEN** the receiver request fails in an Active group
- **THEN** the first row says so and offers "Try again", and the rest of the page is unchanged

#### Scenario: The arrows
- **WHEN** the person chooses the arrow of the first row, and then of the second
- **THEN** the Gifter page opens, and the Receiver page opens

#### Scenario: Alignment
- **WHEN** the group is Active
- **THEN** in both rows the name before the arrow is at the start of the row and the name after the arrow is at the end of the row

## MODIFIED Requirements

### Requirement: The letter pages have web addresses
In the web app the Send letter page SHALL have the address `#/groups/{id}/outbox` and the Inbox page `#/groups/{id}/inbox`, so that a refresh or a pasted address opens the same page. An address for a group the person is not in SHALL show the not available message.

#### Scenario: Send letter address
- **WHEN** the person opens the Send letter page of group 2 on the web
- **THEN** the address ends in `#/groups/2/outbox`

#### Scenario: Inbox address
- **WHEN** the person opens the Inbox page of group 2 on the web
- **THEN** the address ends in `#/groups/2/inbox`

#### Scenario: Refresh
- **WHEN** the person refreshes the page on `#/groups/2/inbox`
- **THEN** the Inbox of group 2 is shown again

#### Scenario: The former address
- **WHEN** the person opens `#/groups/2/letters`
- **THEN** it names no screen and the first screen of their view is shown

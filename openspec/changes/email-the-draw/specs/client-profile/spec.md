## MODIFIED Requirements

### Requirement: Why name and address matter
The Profile tab SHALL say that a Santa uses the person's name and address to post their gift, and that the person's email address is used to send them their draw when a group they are in is drawn. While the name or the address is empty it SHALL say so more directly. It SHALL NOT say or imply who the person's Santa or receiver is, or whether groups have been drawn.

#### Scenario: Incomplete profile
- **WHEN** the name or the address is empty
- **THEN** the tab asks the person to add them so a gift can reach them

#### Scenario: Complete profile
- **WHEN** both the name and the address are filled in
- **THEN** only the general note is shown

#### Scenario: What the email is for
- **WHEN** the Profile tab is shown
- **THEN** its general note says the email is used to send the person their draw

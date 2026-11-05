# Spec Delta

## MODIFIED Requirements

### Requirement: Email recipients
Each participant who has set an email in their delivery info for the group SHALL receive one email addressed only to them. Participants without an address SHALL be counted and SHALL NOT stop the others from being emailed.

#### Scenario: One email each
- **WHEN** a group of four participants with addresses is announced to
- **THEN** four emails are sent, each addressed to one person only

#### Scenario: No address
- **WHEN** one of four participants has no address
- **THEN** three emails are sent and the summary counts one participant without an address


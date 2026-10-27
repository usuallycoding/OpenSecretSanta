# Spec Delta

## MODIFIED Requirements

### Requirement: Santa never disclosed before archive
While a group is not Archived, no response to a participant on the participant endpoints SHALL contain, or let them tell apart, the identity of their Santa, including group views, receiver details, question threads, answers, timestamps, question numbers and error messages. This SHALL NOT apply to the group owner's own admin endpoints (`/admin/groups/{id}/assignments` and the admin-oversight endpoints), which name every pair.

#### Scenario: Receiver's view of questions
- **WHEN** a receiver reads the questions from their Santa in an Active group
- **THEN** the response contains no username, name or other identifier of the Santa

#### Scenario: Question numbers carry no information
- **WHEN** two Santas in the same group each ask a question
- **THEN** both questions are numbered from 1 within their own thread, so numbers reveal nothing about other threads

#### Scenario: Group view
- **WHEN** a participant views an Active group
- **THEN** the response lists the participants as usual and indicates neither their receiver nor their Santa

#### Scenario: Owner who participates
- **WHEN** the owner of an Active group, who also participates, calls `/groups/{id}/santa` or any other participant endpoint
- **THEN** they are treated like any other participant and learn nothing about their Santa from it

### Requirement: Nothing about other people's assignments
A participant SHALL NOT be able to learn who anyone else's receiver or Santa is through the participant endpoints. The group owner's admin endpoints are the only exception.

#### Scenario: Only my own receiver
- **WHEN** any participant uses the receiver endpoint
- **THEN** the response concerns only their own receiver, never another participant's

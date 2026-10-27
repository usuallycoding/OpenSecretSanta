# Spec Delta

## MODIFIED Requirements

### Requirement: Threads are private to the pair
A thread SHALL be readable and writable only by its Santa (for questions) and its receiver (for answers), with one exception: the owner of the group SHALL be able to read every thread through `GET /admin/groups/{id}/questions` as described by the admin-oversight capability. No other participant and no other admin SHALL have any way to read the content, and no admin, including the owner, SHALL have any way to write it.

#### Scenario: Other participants
- **WHEN** a participant who is neither the Santa nor the receiver of a thread uses the question endpoints
- **THEN** they only ever see and change their own two threads

#### Scenario: Owner has no access
- **WHEN** the group's owner, who is not a participant, uses the participant question endpoints
- **THEN** the response is 404, and only the admin-oversight endpoint returns question or answer text

#### Scenario: Owner reads through oversight
- **WHEN** the group's owner uses the admin-oversight questions endpoint
- **THEN** the response contains every thread with its questions and answers, read-only

#### Scenario: Other admins
- **WHEN** an admin who does not own the group requests its questions through any admin endpoint
- **THEN** the response is 404 and contains no question or answer text

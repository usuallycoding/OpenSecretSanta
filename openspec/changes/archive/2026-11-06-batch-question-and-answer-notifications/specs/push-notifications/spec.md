# Spec Delta

## MODIFIED Requirements

### Requirement: Questions and answers
When a Santa asks a question the server SHALL send a question notification to their receiver, and when a receiver answers one it SHALL send an answer notification to their Santa, each naming the group. Questions that follow one another closely SHALL make one notification, and so SHALL answers; the two kinds, different people and different groups SHALL be kept apart. A notification for several says so in the plural.

#### Scenario: Question
- **WHEN** `alice`, Santa of `bob`, asks a question
- **THEN** `bob` is sent a question notification naming the group

#### Scenario: Answer
- **WHEN** `bob` answers `alice`'s question
- **THEN** `alice` is sent an answer notification naming the group

#### Scenario: Switched off
- **WHEN** `bob` has turned "questions" off and `alice` asks him a question
- **THEN** the question is stored as usual and no notification is sent

#### Scenario: Several questions in a row
- **WHEN** `alice`, Santa of `bob`, asks three questions, each less than a minute after the previous one
- **THEN** `bob` is sent one question notification, a minute after the last, and it reads "Your Secret Santa has questions for you" with the group's name

#### Scenario: Several answers in a row
- **WHEN** `bob` answers three questions, each less than a minute after the previous one
- **THEN** `alice` is sent one answer notification, a minute after the last, and it reads "Your receiver answered questions" with the group's name

#### Scenario: A single one keeps the singular
- **WHEN** `alice` asks one question and nothing else follows
- **THEN** `bob` is sent one notification, a minute later, reading "Your Secret Santa has a question for you" with the group's name

#### Scenario: Never held for long
- **WHEN** `alice` keeps asking a question every thirty seconds for ten minutes
- **THEN** `bob` is sent a notification at the latest five minutes after the first question of each batch, not one at the very end

#### Scenario: Kept apart
- **WHEN** `bob` changes his wishlist, answers a question and, as a Santa, asks his own receiver a question within the same minute
- **THEN** each of the three makes its own notification, to the right person

#### Scenario: Other groups and people
- **WHEN** `alice` asks a question in one group and `carol` asks one in the same group, or `alice` asks in two groups
- **THEN** every receiver of every group is sent their own notification

### Requirement: Notifications keep the Santa secret
No notification SHALL contain the identity of a participant's Santa, the text of a question or an answer, or the contents of a wishlist item, because notifications can be read on a locked screen. The same wording SHALL be used whichever participant the notification is for.

#### Scenario: A question
- **WHEN** a question is sent
- **THEN** the notification says only that a question (or, for several, questions) came in for the group, without the Santa's name and without the question

#### Scenario: A wishlist item
- **WHEN** an item titled "Blue scarf" is added
- **THEN** the notification does not contain "Blue scarf"

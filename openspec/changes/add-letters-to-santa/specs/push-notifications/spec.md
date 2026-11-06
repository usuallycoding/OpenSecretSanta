# Spec Delta

## MODIFIED Requirements

### Requirement: Notifications keep the Santa secret
No notification SHALL contain the identity of a participant's Santa, the text of a question or an answer, or the contents of a wishlist item, because notifications can be read on a locked screen. The same wording SHALL be used whichever participant the notification is for.

#### Scenario: A question
- **WHEN** a question is sent
- **THEN** the notification says only that a question (or, for several, questions) came in for the group, without the Santa's name and without the question

#### Scenario: A letter
- **WHEN** a letter titled "Thank you" with the text "For the scarf" is sent
- **THEN** the notification contains neither the title nor the text, only that a letter came in for the group

#### Scenario: A wishlist item
- **WHEN** an item titled "Blue scarf" is added
- **THEN** the notification does not contain "Blue scarf"

## ADDED Requirements

### Requirement: Five notification categories
Each person SHALL have five switches that decide what they are notified about: announcements, wishlist changes, questions, answers and letters from their receiver. Each SHALL be on until the person turns it off. The person SHALL be able to read and change them, and a change SHALL apply to every device of that person and to every notification sent after it. A request that does not mention the letters switch SHALL turn it on.

#### Scenario: Defaults
- **WHEN** a person who has never changed them reads their switches
- **THEN** all five are on, including letters

#### Scenario: Turn letters off
- **WHEN** `alice` turns "letters" off
- **THEN** a letter from her receiver sends her nothing, while her announcements, wishlist changes, questions and answers still do

#### Scenario: Turn one off
- **WHEN** `alice` turns "questions" off
- **THEN** a question for her sends her nothing, while the other four kinds still do

#### Scenario: Shared by devices
- **WHEN** `alice` turns "letters" off from her phone
- **THEN** her browser also receives no letter notification

#### Scenario: An older client
- **WHEN** a request gives the four older switches and no letters switch
- **THEN** it is accepted and the letters switch is on

#### Scenario: Unknown or missing values
- **WHEN** a change names a switch that does not exist, or gives a value that is not true or false
- **THEN** it is refused with 400 and no switch changes

### Requirement: Letters from your receiver
When a receiver sends a letter the server SHALL send a letter notification to their Santa if the Santa has the letters switch on, naming the group. Letters that follow one another closely SHALL make one notification, held like questions: sent after a minute of quiet and at the latest five minutes after the first, with the plural "letters" when several are waiting. It SHALL be kept apart from other kinds, people and groups.

#### Scenario: One letter
- **WHEN** `bob` sends one letter to his Santa `alice`
- **THEN** a minute later `alice` is sent "Your receiver sent you a letter" with the group's name

#### Scenario: Several letters
- **WHEN** `bob` sends three letters less than a minute apart
- **THEN** `alice` is sent one notification reading "Your receiver sent you letters" with the group's name

#### Scenario: Switched off
- **WHEN** `alice` has turned "letters" off and `bob` sends a letter
- **THEN** the letter is stored as usual and no notification is sent

#### Scenario: Tap target
- **WHEN** a letter notification for group 7 is delivered
- **THEN** it carries group 7 and the category "letters"

## REMOVED Requirements

### Requirement: Four notification categories
**Reason**: There are now five kinds, with "Letters from your receiver".
**Migration**: See "Five notification categories".

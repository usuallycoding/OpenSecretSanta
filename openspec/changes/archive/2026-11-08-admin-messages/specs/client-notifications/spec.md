# Spec Delta

## MODIFIED Requirements

### Requirement: Tapping a notification opens its screen
Tapping a notification SHALL open the screen it is about with the group list and the group's page behind it: a question from the person's Santa opens the Receiver page on its questions tab, an answer from the person's receiver opens the Gifter page on its questions tab, a change to the receiver's wishlist opens the Gifter page on its wishlist tab, a letter from the person's receiver opens the Inbox page, an announcement the owner sent opens the Group Notifications page, and any other notification, or one with no known category, opens the group's page.

#### Scenario: A letter
- **WHEN** a person taps the notification of a letter from their receiver
- **THEN** the screens, from the bottom, are the group list, the group's page and the Inbox page

#### Scenario: Order of the screens
- **WHEN** a person taps the notification of a question from their Santa
- **THEN** the screens, from the bottom, are the group list, the group's page and the Receiver page on its questions tab

#### Scenario: An announcement from the owner
- **WHEN** a person taps the notification of an announcement the owner sent
- **THEN** the screens, from the bottom, are the group list, the group's page and the Group Notifications page, and on a wide window the group's page with the Group Notifications pane open

#### Scenario: Added to a group, or the names drawn
- **WHEN** a person taps the notification that they were added to a group, or that the names were drawn
- **THEN** the group's page opens, not the Group Notifications page

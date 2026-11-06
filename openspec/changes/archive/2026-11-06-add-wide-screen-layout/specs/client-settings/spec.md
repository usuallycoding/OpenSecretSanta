## MODIFIED Requirements

### Requirement: The Settings tab
The bottom bar of the participant view and of the admin view, and on a wide window their sidebar, SHALL each have a Settings item with a settings icon and the label "Settings", after the view's other tabs. Choosing it SHALL open the Settings page, and the item SHALL be shown as selected while the page is open. Like the other tabs, it SHALL keep its place when the person moves to another tab and back.

#### Scenario: Participant's bar
- **WHEN** a participant is signed in
- **THEN** the bottom bar, or on a wide window the sidebar, offers Groups and Settings, in that order

#### Scenario: Administrator's bar
- **WHEN** an administrator is in the admin view
- **THEN** the bottom bar, or on a wide window the sidebar, offers Groups, Users and Settings, in that order

#### Scenario: Selected
- **WHEN** the Settings page is open
- **THEN** the Settings item is shown as selected and no other item is

#### Scenario: One page for both views
- **WHEN** a participant and an administrator each open Settings
- **THEN** both see the same Settings page

### Requirement: Settings does not change the view
Opening the Settings page SHALL NOT switch between the participant view and the admin view: it SHALL be shown in the view the person is in, with that view's bottom bar, or on a wide window that view's sidebar.

#### Scenario: Administrator in the admin view
- **WHEN** an administrator in the admin view chooses Settings
- **THEN** the Settings page is shown with the admin view's bar or sidebar, Groups, Users and Settings

#### Scenario: Administrator in the participant view
- **WHEN** an administrator who has switched to the participant view chooses Settings
- **THEN** the Settings page is shown with the participant view's bar or sidebar

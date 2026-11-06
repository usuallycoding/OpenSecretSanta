## MODIFIED Requirements

### Requirement: Views by role
After sign-in the app SHALL show a participant view to everyone and an admin view to administrators only. The participant view SHALL be a Groups tab and a Settings tab under a top bar; the admin view SHALL be a Groups tab with the administrator's groups, a Users tab and the same Settings tab, under the same top bar. The tabs are in a bottom bar, or in the sidebar on a wide window, as `client-layout` describes.

#### Scenario: Administrator
- **WHEN** an administrator signs in
- **THEN** the admin view is shown and the switch icon in the top bar switches to the participant view and back

#### Scenario: Normal participant
- **WHEN** a non-administrator signs in
- **THEN** only the participant view is shown and there is no way to reach the admin view

#### Scenario: The admin view has two tabs
- **WHEN** an administrator is in the admin view
- **THEN** a bottom bar, or on a wide window the sidebar, offers its two tabs, Groups, which shows the administrator's own groups, and Users, and then the Settings tab that both views share

#### Scenario: Switching views
- **WHEN** an administrator switches to the participant view and back
- **THEN** each view opens on its first screen

#### Scenario: The menu in both views
- **WHEN** the person looks at the top bar in the participant view or in the admin view
- **THEN** there is no menu: an administrator sees the one switch icon and anyone else sees no action, apart from the page's own buttons on a wide window

### Requirement: Actions that add are floating buttons
On a window narrower than 840dp adding a wishlist item, asking a question and creating a group (the owner's "New group") SHALL each be a floating action button at the bottom end of its screen, showing an icon and offering the action's name to assistive technology, shown only while the action is available. When the action is not available the screen SHALL keep saying why in text, as before. The button SHALL NOT cover the last row of the content. On wider windows `client-layout` puts them in the top bar.

#### Scenario: Add item
- **WHEN** the person's wishlist can be changed and has room
- **THEN** a floating button with a plus icon opens the add sheet

#### Scenario: Not available
- **WHEN** the wishlist is full or closed
- **THEN** there is no button and the screen says so in text

#### Scenario: Last row
- **WHEN** a list is scrolled to its end
- **THEN** its last item is fully visible above the button

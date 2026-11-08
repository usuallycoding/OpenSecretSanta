# Spec Delta

## MODIFIED Requirements

### Requirement: The rules page has a readable web address
On the web, the rules page of an administrator's group SHALL have the address `#/admin/groups/{id}/rules`, which opens it for an administrator and names no screen for anyone else. The address SHALL be the same on a narrow and on a wide window and SHALL stay while the rules are open: on a wide window it SHALL show the group page with the rules in the pane beside it.

#### Scenario: Address of the page
- **WHEN** the admin is on the rules page of group 7 on a narrow window
- **THEN** the address is `#/admin/groups/7/rules`

#### Scenario: Opening the address
- **WHEN** an administrator opens `#/admin/groups/7/rules` on a narrow window
- **THEN** the rules page of group 7 opens, with the group page behind it

#### Scenario: Opening the address on a wide window
- **WHEN** an administrator opens `#/admin/groups/7/rules` on a wide window
- **THEN** the group page shows with the rules in its pane, and the address is still `#/admin/groups/7/rules`

#### Scenario: Not an administrator
- **WHEN** someone who is not an administrator opens that address
- **THEN** it names no screen, as any admin address does

#### Scenario: The gavel on a wide window
- **WHEN** the admin chooses the gavel on a wide window
- **THEN** the rules open in the pane and the address becomes `#/admin/groups/7/rules`, and `#/admin/groups/7` again once the pane is closed

#### Scenario: Resizing
- **WHEN** the window is made narrower or wider with the rules open
- **THEN** the address stays `#/admin/groups/7/rules` and the rules are shown as a page or a pane

#### Scenario: Group in Draft
- **WHEN** an administrator opens `#/admin/groups/7/rules` and group 7 is in Draft
- **THEN** the group's page is shown, since a Draft group has no rules page

### Requirement: Back closes an open sheet
Going back with the system back button on Android while a bottom sheet is open SHALL close the sheet and stay on the screen behind it. On the web the browser's back button belongs to the screens' history, which a sheet is not part of, so it SHALL leave the screen as it does without a sheet; the Escape key SHALL close an open sheet there.

#### Scenario: Back from a sheet on Android
- **WHEN** a user sheet is open on a group and the person uses the system back button on Android
- **THEN** the sheet closes and the group detail is still shown

#### Scenario: Back again on Android
- **WHEN** the person goes back once more
- **THEN** the groups list is shown

#### Scenario: Browser back on the web
- **WHEN** a sheet is open on a group in the web app and the person presses the browser's back button
- **THEN** the groups list is shown, the page is not reloaded and the session is kept

#### Scenario: Escape on the web
- **WHEN** a sheet is open on a group in the web app and the person presses Escape
- **THEN** the sheet closes and the group detail is still shown

## ADDED Requirements

### Requirement: A pair has a readable web address
On the web, the pair of an administrator's drawn group SHALL have the address `#/admin/groups/{id}/pairs/{santa}/{receiver}`, shown while the pair is open and the same on a narrow and a wide window, which opens it for an administrator and names no screen for anyone else. On a wide window it SHALL show the group page with the pair in the pane beside it. A pair that is not in the group's draw, and any pair before the draw, SHALL open the group's page.

#### Scenario: Choosing the pair link
- **WHEN** the admin chooses the pair link of `alice` and `bob` in group 7 on any window
- **THEN** the address becomes `#/admin/groups/7/pairs/alice/bob`, and `#/admin/groups/7` once the pair is closed

#### Scenario: Opening the address on a narrow window
- **WHEN** an administrator opens `#/admin/groups/7/pairs/alice/bob` on a narrow window
- **THEN** the pair opens as a page, with the group page behind it

#### Scenario: Opening the address on a wide window
- **WHEN** an administrator opens that address on a wide window
- **THEN** the group page shows with the pair in its pane, and the address stays

#### Scenario: Not a pair of the draw
- **WHEN** an administrator opens `#/admin/groups/7/pairs/alice/carol` and `alice` does not give to `carol`, or group 7 is not drawn
- **THEN** the group's page is shown

#### Scenario: Not an administrator
- **WHEN** someone who is not an administrator opens that address
- **THEN** it names no screen, as any admin address does

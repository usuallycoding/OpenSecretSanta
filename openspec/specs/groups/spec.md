# groups Specification

## Purpose

Defines the Secret Santa group as the system stores it: its fields, who may manage it, how its participant list behaves in each state, and what participants themselves can see of the groups they belong to.

## Requirements

### Requirement: Group record
A group SHALL have an owner, a name, a description, a location, a date, a time, a list of participant usernames and a state. Only the name is required; description, location, date and time MAY be empty. The date and time are local values without a timezone.

#### Scenario: Minimal group
- **WHEN** an admin creates a group with only a name
- **THEN** the group exists in state Draft, owned by that admin, with no participants and empty description, location, date and time

#### Scenario: Invalid date or time
- **WHEN** an admin submits a date that is not a real calendar date or a time that is not a valid time of day
- **THEN** the response is 400 and nothing is saved

#### Scenario: Name required
- **WHEN** an admin submits a blank name, or one longer than 100 characters
- **THEN** the response is 400 naming the `name` field

### Requirement: Create a group
The server SHALL expose `POST /admin/groups` letting an admin create a group, which always starts in Draft and is owned by its creator. The participant list MAY be given at creation.

#### Scenario: Creation
- **WHEN** an admin posts a valid group
- **THEN** the response is 201 with the new group in state Draft and the admin as owner

#### Scenario: Non-admin refused
- **WHEN** a user who is not an admin posts a group
- **THEN** the response is 403

### Requirement: Owner-only management
Only a group's owner SHALL be able to view its management details, edit it, change its participants, move it through its lifecycle or delete it. Any other user, including other admins, SHALL be answered as if the group did not exist.

#### Scenario: Other admin
- **WHEN** admin B requests, edits or transitions a group owned by admin A
- **THEN** the response is 404 and nothing changes

#### Scenario: Unknown group
- **WHEN** an admin requests a group id that does not exist
- **THEN** the response is 404

### Requirement: List and view owned groups
The server SHALL expose `GET /admin/groups` listing every group the admin owns in any state, and `GET /admin/groups/{id}` returning one with all its fields and, per participant, whether an account exists and whether the profile is complete (full name and address both present).

#### Scenario: Only own groups listed
- **WHEN** admin A lists groups while admin B also owns groups
- **THEN** only A's groups are returned

#### Scenario: Participant flags
- **WHEN** the owner views a Pending group containing `alice` with a full name and address, `bob` without an address, and `carol` who has no account yet
- **THEN** alice is shown as existing and complete, bob as existing and incomplete, carol as not existing and incomplete

### Requirement: Edit group details
The owner SHALL be able to change a group's name, description, location, date and time through `PUT /admin/groups/{id}` while the group is Draft, Pending or Active. An Archived group SHALL NOT be editable.

#### Scenario: Edit in Active
- **WHEN** the owner changes the location of an Active group
- **THEN** the response is 200 and the new location is stored

#### Scenario: Archived is read-only
- **WHEN** the owner edits an Archived group
- **THEN** the response is 409 with code `invalid_state` and nothing changes

### Requirement: Participant list in Draft
While a group is Draft, its participants SHALL be a list of usernames set through `PUT /admin/groups/{id}/participants`, replacing the previous list. Usernames MUST be valid but need not belong to existing users, and duplicates ignoring case are stored once.

#### Scenario: Names without accounts
- **WHEN** the owner sets participants `alice` and `newcomer` and only `alice` has an account
- **THEN** both are stored, no account is created, and `newcomer` has no account

#### Scenario: Duplicates and case
- **WHEN** the list contains `Alice` and `alice`
- **THEN** one participant `alice` is stored

#### Scenario: Invalid username
- **WHEN** the list contains an invalid username
- **THEN** the response is 400 naming `participants` and the stored list is unchanged

### Requirement: Participant changes in Pending
While a group is Pending, the owner SHALL be able to replace the participant list; usernames without an account get an account with a generated temporary password, and removed participants leave the group while keeping their account. The response SHALL list the newly created accounts with their temporary passwords.

#### Scenario: Adding a new person
- **WHEN** the owner adds `dave`, who has no account, to a Pending group
- **THEN** `dave` becomes a participant with a new account that must change its temporary password, and the response includes his temporary password

#### Scenario: Removing a person
- **WHEN** the owner removes `bob` from a Pending group
- **THEN** `bob` is no longer a participant and his account still exists

#### Scenario: Adding an existing user
- **WHEN** the owner adds `erin`, who already has an account, to a Pending group
- **THEN** no account is created or changed for `erin`

### Requirement: Roster frozen after activation
Once a group is Active or Archived, its participant list SHALL NOT change.

#### Scenario: Change attempt
- **WHEN** the owner tries to replace the participants of an Active group
- **THEN** the response is 409 with code `invalid_state` and the list is unchanged

### Requirement: Participants see their groups
The server SHALL expose `GET /groups` and `GET /groups/{id}` to any authenticated user, returning the name, description, location, date, time, state and participant usernames of the non-Draft groups in which that user participates, and nothing else about the group or the other participants.

#### Scenario: Member sees group
- **WHEN** `alice`, a participant of a Pending group, requests the group
- **THEN** she receives its details and the participants' usernames only

#### Scenario: Draft is invisible
- **WHEN** `alice` is named in a Draft group
- **THEN** that group is not listed for her and requesting it returns 404

#### Scenario: Not a participant
- **WHEN** a user requests a group they do not participate in
- **THEN** the response is 404

#### Scenario: Password change first
- **WHEN** a user who must still change a temporary password requests their groups
- **THEN** the response is 403 with code `password_change_required`

### Requirement: Delete a group
The owner SHALL be able to delete a group in any state with `DELETE /admin/groups/{id}`. Deleting SHALL remove the group together with its participants, its draw, every wishlist item, every question and every answer. When the group has linked data (a draw, a wishlist item, a question or an answer), the server SHALL refuse the request without `confirm=true` with HTTP 409, the code `confirmation_required` and the counts of what would be deleted, and SHALL delete only when the request carries `confirm=true`. A group without linked data SHALL be deleted by the plain request.

#### Scenario: Delete a Draft
- **WHEN** the owner deletes a Draft group
- **THEN** the response is 204 and the group no longer exists

#### Scenario: Pending group without wishlist items
- **WHEN** the owner deletes a Pending group in which nobody has added a wishlist item
- **THEN** the response is 204 and the group no longer exists

#### Scenario: Confirmation required
- **WHEN** the owner deletes an Active group that has a draw, wishlist items, questions and answers, without `confirm=true`
- **THEN** the response is 409 with code `confirmation_required` and the counts of wishlist items, questions and answers and whether there is a draw, and the group remains

#### Scenario: Confirmed
- **WHEN** the owner repeats the request with `confirm=true`
- **THEN** the response is 204 and the group, its draw, wishlist items, questions and answers are all gone

#### Scenario: Pending group with wishlist items
- **WHEN** the owner deletes a Pending group that has wishlist items without `confirm=true`
- **THEN** the response is 409 with code `confirmation_required` and the number of wishlist items

#### Scenario: Archived group
- **WHEN** the owner deletes an Archived group with `confirm=true`
- **THEN** the response is 204 and nothing of it remains

#### Scenario: Someone else's group
- **WHEN** an administrator who does not own the group deletes it, with or without `confirm=true`
- **THEN** the response is 404 and the group remains

#### Scenario: Missing group
- **WHEN** the group does not exist
- **THEN** the response is 404

### Requirement: Group lists are newest first
`GET /admin/groups` and `GET /groups` SHALL return their groups newest first, by the time each group was created, and a group created later SHALL come first among groups created in the same second. Changing a group's details, its participants or its state SHALL NOT change its place.

#### Scenario: Administrator's list
- **WHEN** an administrator creates "First" and then "Second" and lists their groups
- **THEN** "Second" comes before "First"

#### Scenario: Participant's list
- **WHEN** `alice` takes part in a Pending group created earlier and an Active group created later
- **THEN** her list has the later group first

#### Scenario: A copy comes first
- **WHEN** an administrator duplicates an old group
- **THEN** the copy is the first group of their list

#### Scenario: Changes do not reorder
- **WHEN** the oldest group of a list is edited, moved to Pending and drawn
- **THEN** it is still the last group of the list

#### Scenario: Same second
- **WHEN** two groups are created within the same second
- **THEN** the one created last is returned first

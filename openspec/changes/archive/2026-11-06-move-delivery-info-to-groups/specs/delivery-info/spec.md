# Spec Delta

## Purpose

What a participant tells a group about where and how a gift can reach them: five optional details kept per group, who may read and change them, and what became of the profiles people had saved on their accounts.

## ADDED Requirements

### Requirement: Delivery info belongs to a group
A participant SHALL have, for each group they take part in, delivery info: a full name, an email, a phone number, an address and delivery information, each optional and initially empty. It SHALL belong to that group alone and SHALL NOT be shared with the person's other groups.

#### Scenario: Nothing set yet
- **WHEN** a person becomes a participant of a group
- **THEN** their delivery info for it has no full name, email, phone, address or delivery information

#### Scenario: One group at a time
- **WHEN** `alice` sets her address in group 1 and is also a participant of group 2
- **THEN** her delivery info for group 2 still has no address

#### Scenario: Gone with the group
- **WHEN** a group is deleted
- **THEN** the delivery info of its participants for it is gone

### Requirement: Read own delivery info
The server SHALL expose `GET /groups/{id}/delivery-info` to a participant of a Pending, Active or Archived group, returning their own delivery info for it, with the fields they have not set empty. It SHALL return nothing about any other participant.

#### Scenario: Own delivery info
- **WHEN** `alice`, a participant of a Pending group who has set an address, requests the route
- **THEN** the response has her address and empty fields for the others

#### Scenario: Not a participant or a Draft
- **WHEN** a person who is not in the group, or anyone for a Draft group, requests the route
- **THEN** the response is 404

#### Scenario: Password change first
- **WHEN** a person who must still change a temporary password requests the route
- **THEN** the response is 403 with code `password_change_required`

### Requirement: Edit own delivery info
The server SHALL expose `PUT /groups/{id}/delivery-info` letting a participant replace their full name, email, phone, address and delivery information for the group while it is Pending or Active, any of which may be cleared. A given email SHALL be a syntactically valid address and each text field at most 500 characters. An Archived group SHALL NOT be changed.

#### Scenario: Updated
- **WHEN** `alice` submits new values in a Pending group
- **THEN** the response is 200 and a later `GET` of the route shows them

#### Scenario: Field cleared
- **WHEN** a participant submits a null or blank phone
- **THEN** the phone becomes empty

#### Scenario: Invalid email
- **WHEN** a participant submits an email that is not a valid address
- **THEN** the response is 400 naming the `email` field and nothing changes

#### Scenario: Too long
- **WHEN** a participant submits an address of 501 characters
- **THEN** the response is 400 naming the `address` field and nothing changes

#### Scenario: Archived
- **WHEN** a participant edits their delivery info in an Archived group
- **THEN** the response is 409 with code `invalid_state` and nothing changes

#### Scenario: Only their own
- **WHEN** `alice` calls the route
- **THEN** only her own delivery info for that group changes

### Requirement: Others read delivery info only as the draw allows
Another participant's delivery info SHALL be readable only as `gift-visibility` describes: the receiver's by their Santa, and the Santa's full name after the group is archived. The group's owner SHALL see, for each participant, only the full name and whether the delivery info is complete, as `groups` describes.

#### Scenario: A stranger's details
- **WHEN** `carol`, who is neither `bob`'s Santa nor his owner, asks for `bob`'s delivery info
- **THEN** no route gives it to her

#### Scenario: The owner's view
- **WHEN** the owner views a group
- **THEN** each participant carries a full name and a completeness flag, and no email, phone, address or delivery information

### Requirement: Profiles saved on accounts are kept
When delivery info moves to the groups, each profile saved on an account SHALL be copied into the delivery info of every group that person takes part in, in any state, and accounts SHALL no longer hold profile fields.

#### Scenario: Copied into each group
- **WHEN** `alice` had saved an address and is in three groups
- **THEN** her delivery info for each of the three has that address

#### Scenario: Nothing saved
- **WHEN** a person had saved no profile
- **THEN** their delivery info for every group is empty


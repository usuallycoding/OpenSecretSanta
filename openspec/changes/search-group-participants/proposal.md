## Why

The owner of a group with many participants has no way to find one person on the group page: the Pending list and the Active or Archived pairs have to be scanned by eye. The groups list already has a search; the people inside a group do not, and the page does not even carry the participants' full names to search by.

## What Changes

- The administrator's page for a Pending, Active or Archived group gets a search field over its participants. It matches a participant's username or full name, words in any order, ignoring case.
- On a Pending group the list shows only the participants that match. On an Active or Archived group each pair stays when its Santa or its receiver matches, so searching for "alice" shows who gives to alice and who alice gives to.
- When nothing matches, the page says so instead of showing an empty box. An empty or blank search shows everyone.
- The server's answer for an owned group (`GET /admin/groups/{id}` and the answers that carry the group) gains each participant's `fullName`, when the account has one. The owner can already read these profiles through the user pages.
- The search only changes what is shown: counts, the pair and person sheets and every action stay as they are.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `groups`: "List and view owned groups" says that each participant also carries their full name when there is one.
- `client-admin-groups`: a new requirement for the participant search on a group page.

## Impact

- Contract: `ParticipantDto` gains an optional `fullName`. Domain: `ParticipantStatus` gains it, filled by `GroupViewer` from the accounts it already loads. Server mapper: one field. No new request, route or migration.
- Client: `AdminParticipant` and the pair rows carry the full name; a filter function, a search field on `AdminGroupScreen`, strings. Tests for the filter, the view model state and the server's answer.

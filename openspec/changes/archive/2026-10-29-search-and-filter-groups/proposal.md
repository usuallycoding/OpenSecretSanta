# Proposal

## Why

The groups lists get long: an administrator keeps every group they ever made, and a participant keeps every group they were ever in, with the oldest first, so the group someone wants is usually at the bottom. There is no way to look for one by what it is called or about, or to leave out the ones that are over. Newest first puts what is current at the top, and a search field with state filters finds the rest.

## What Changes

- Both group lists, the participant's Groups tab and the administrator's groups list, get a **search field** above the list. It matches the group's name and description, ignoring case, and a search of several words matches a group whose name and description together contain every word.
- Under the search field are **state chips** that can be switched on in any combination. Nothing switched on means every state. The administrator's chips are Draft, Pending, Active and Archived; the participant's are Pending, Active and Archived.
- The search and the chips work together: a group is shown only when it matches both.
- A search or a set of chips that matches nothing says so, with a way to clear them; it does not show the "no groups yet" message, which is only for a person who really has none.
- The server returns both lists **newest first** (by the date the group was created), instead of oldest first. The clients keep showing the order the server gives, so filtering never reorders.
- The participant list's rows keep the group's description so that it can be searched; nothing else about what a row shows changes.

## Capabilities

### New Capabilities
- `client-group-search`: the search field and the state chips of the two groups lists, what they match, how they combine, when they show, and what the list says when nothing matches.

### Modified Capabilities
- `groups`: the two group lists the server returns (`GET /admin/groups` and `GET /groups`) are ordered newest first. This is a new requirement added to the existing capability; the existing requirements are unchanged.

## Impact

- **Server**: the two list queries in the group repository change their ordering; their tests, and the route tests that read the lists in order, change with them. No new field, route or migration: `groups.created_at` already exists.
- **Client (shared)**: a description on the participant's row model; a pure filter function and a small filter state; a search-and-chips composable used by both screens; both groups view models and screens; new strings.
- **Not affected**: the API's shape, every other screen, notifications, and the existing `client-groups` and `client-admin-groups` requirements (they already say the lists show the server's order).
- **Behaviour people will notice**: both lists reverse. A group made with "Duplicate" now appears at the top of the administrator's list instead of the bottom. Editing, drawing or archiving a group does not move it.

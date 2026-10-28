# Proposal

## Why

The server already lets a group's owner read everything going on inside it: each participant's wishlist, every Santa's question thread and the full draw. The admin app has no screens for any of it, so an administrator who wants to check on a group (are the lists filled in, did the receiver answer, who gives to whom) has to call the API by hand. This is the last unbuilt part of the admin view.

## What Changes

- **Who gives to whom.** On the detail of an Active or Archived group, a section lists every Santa and receiver pair, loaded from `GET /admin/groups/{id}/assignments`. The owner's own pairs are included and marked "you".
- **Participant wishlists.** On the same detail, choosing a participant (Pending, Active or Archived) opens that person's wishlist for the group, read-only, in their order: title, note, link (opens in the browser) and when it was added or changed.
- **All questions.** On an Active or Archived group, a "Questions" screen shows one thread per Santa and receiver pair with every question, its answer and the times. It has no way to ask, answer or change anything.
- **Reading leaves no trace.** The screens only issue GET requests and keep nothing beyond what is on screen.
- **Concurrent changes.** A group or participant that is no longer available (404) or whose state no longer allows the read (409) is shown as such, with a way back.

Out of scope:
- Any change to wishlists, questions or answers by the owner; notifications; searching or exporting; server changes (the routes exist).

## Capabilities

### New Capabilities
- `client-admin-oversight`: how the owner reads the draw, a participant's wishlist and all question threads in the app.

### Modified Capabilities
- `client-admin-groups`: the group detail offers the draw, the participant wishlists and the questions where the state allows them.
- `client-app`: the new routes join the admin navigation and the address bar on the web.

## Impact

- Code: three admin read calls in `ApiClient`, three view models and three screens (draw section on the detail, participant wishlist, all questions) in `app/shared`, two typed routes, strings and the README sentence about oversight.
- APIs: none changed.
- Dependencies: none.
- Systems: none.

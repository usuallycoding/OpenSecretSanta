# Proposal

## Why

Signing in now leads to a placeholder: a participant sees "your groups will appear here" and nothing else. The server already lets a participant list the non-Draft groups they are in, read one, and edit their own profile, but the app has no screen for any of it. Groups are the way into everything a participant does later (wishlist, receiver, questions), and the profile is what a Santa uses to post a gift, so people need both before the rest can be built. This is the first change with real navigation: moving from a list into a group and back.

## What Changes

- **Groups tab.** The participant view shows the groups the person is in: name, state (Pending, Active or Archived), date and time, location and number of participants, in the order the server returns them. Someone in no group sees a short explanation that an organiser adds people by username.
- **Group detail.** Tapping a group opens its details: name, state, description, date, time, location and the participants' usernames (the person's own marked), with a short note on what the state means. It carries no assignment information; wishlists, receiver and questions come in later changes.
- **Profile tab.** The person sees their username (read-only) and edits full name, email, phone, address and delivery information. The form checks the same rules as the server before sending, shows the server's field errors next to their fields, and says why name and address matter (a Santa needs them to post a gift) without saying anything about who the Santa is.
- **Navigation shell.** The participant view gets a top bar with a menu (switch to the admin view for administrators, sign out) and bottom tabs `Groups` and `Profile`. Back works the platform's way: the system back button on Android, the swipe gesture on iOS and the browser's back button on the web.
- **Loading and failure.** Every screen shows a progress indicator while loading and, when the server cannot be reached or answers with an error, a message with a retry action. Screens refresh when the app returns to the foreground, and a refresh action is always there.
- **Library.** Navigation uses the Compose Multiplatform navigation library (typed routes), added in the first task and built on all three targets before anything depends on it.

The admin view stays a placeholder. The server needs no change.

Out of scope:
- Wishlists, receiver details, questions and the Santa reveal (changes 1b and 1c), and all admin screens.
- Changing the password voluntarily from the menu, notifications, live updates, sorting or filtering groups, and showing other participants' names.

## Capabilities

### New Capabilities
- `client-groups`: what a participant sees of their groups in the app: the list, the detail, the empty state, what each state means, loading, errors and refreshing.
- `client-profile`: how a participant views and edits their own profile in the app: the form, validation, server errors, saving and the note about why name and address matter.

### Modified Capabilities
- `client-app`: the participant view is no longer a placeholder but the tabbed shell (the control to switch between views moves into the top-bar menu), and the app gains back navigation on every platform.

## Impact

- Code: navigation, a scaffold with top bar and tabs, load-state handling, three view models and screens (groups, group detail, profile), date formatting and new `ApiClient` calls (`groups`, `group`, `updateProfile`) in `app/shared`; the web host binds navigation to the browser history; the README.
- APIs: none new. The app additionally uses `GET /groups`, `GET /groups/{id}` and `PUT /me`, with the contract types that already exist.
- Dependencies: the Compose Multiplatform navigation library (and the Kotlin serialization plugin for the shared module, for typed routes).
- Systems: none.

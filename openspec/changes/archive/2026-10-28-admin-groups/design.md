# Design

## Context

The signed-in app is one `Scaffold` and one `NavController` (`ShellScreen`): the participant view is a `NavHost` of typed routes, the admin view is still a centered placeholder, and the top-bar menu switches `AppView`. The web binds browser history to that one controller (`onNavigationReady`), so anything the address bar should follow has to live in it. Screens load through `Loader` and `LoadContent` and reload on `ON_RESUME`; view models take a `CoroutineScope`, are created by Koin, and are tested over `ScriptedServer` with a real `ApiClient`. The wishlist and question changes set the pattern this one follows: the server's answer is what is shown, a refusal reloads and says what happened, sheets keep their text on a network failure. The contract types (`AdminGroupDto`, `ParticipantDto`, `CreateGroupRequest`, `GroupDetailsRequest`, `ParticipantsRequest`, `GroupChangeResponse`, `CreatedAccountDto`) and routes exist. `GroupDetails.of` and `Username.parse` in `core/domain` validate exactly as the server does, so the form reuses them. See proposal.md for motivation and specs/ for behavior.

## Goals / Non-Goals

**Goals:**
- A group's whole life (create, edit, participants, three steps, delete) from the app, with the server's rules checked before sending and its refusals shown in place.
- Temporary passwords visible exactly once and never kept.

**Non-Goals:**
- Users, owner oversight, participants at creation, copying groups, notifications (see proposal.md).

## Decisions

### D1: Routes and view switching
```
 AdminGroupsRoute -+-> AdminGroupNewRoute            create form
                   +-> AdminGroupRoute(id) -> AdminGroupEditRoute(id)   details form
```
Typed routes in the same `NavController` and the same `NavHost` as the participant routes, so the address bar and the back button work on every platform without new code. Switching views clears the back stack and opens the other view's first screen, so each view starts fresh and the back button leaves the app from either root rather than wandering into the other view. (Remembering each view's place would need a nested graph per view; not worth it for a switch that is rare.) The admin view starts on `AdminGroupsRoute`: the start destination is chosen from the view when the shell is created. The top bar shows a back arrow below the admin list (detail, form) and the title of each screen. There is no bottom bar yet: it arrives with the second tab in the next admin change.

### D2: Calls
`ApiClient` gains `adminGroups()`, `adminGroup(id)`, `createGroup(request)`, `updateGroup(id, request)`, `setParticipants(id, usernames)`, `deleteGroup(id)`, `makePending(id)`, `activate(id)` and `archive(id)` through the existing `call` helper. `setParticipants`, `makePending`, `activate` and `archive` answer `GroupChangeResponse` (the group and the accounts created); `createGroup` and `updateGroup` answer `AdminGroupDto`; `DELETE` answers 204 with no body (`Unit`, as the wishlist delete does). Errors arrive as `Validation(field, message)` (400, `participants` for fewer than 4 or a bad username), `Rejected(409, invalid_state)` and `Rejected(404, ...)` (also what another administrator's group answers).

### D3: Three view models
```
 AdminGroupsViewModel   Loader<List<AdminGroup>>
 AdminGroupFormViewModel(groupId?)   fields, per-field errors, saving; create or edit
 AdminGroupViewModel(groupId)        Loader<AdminGroup> + sheet + confirmation + passwords + notice
```
- **List:** a plain `Loader` over `adminGroups()`; a deleted group is removed by reloading on return.
- **Form:** holds the five fields as text. `save()` runs `GroupDetails.of` (the same code the server uses), maps its `ValidationFailed.field` to the field, then sends `createGroup` or `updateGroup`. A server `Validation` with a field is shown on that field; a network failure keeps the text and says so. The edit form is filled from the loaded group, so it does not load by itself. Success hands the saved group to the caller (the new group opens; an edit goes back).
- **Detail:** loads `adminGroup(id)`. `state` is parsed to a phase (`Draft`, `Pending`, `Active`, `Archived`, `Other`) deciding which actions exist: edit details unless Archived, edit participants in Draft and Pending, the next step, delete in Draft. Each step first sets a `confirming` value (what and which wording) and sends only when confirmed. The 4-participant rule is checked locally for "Draw the names", and the server's 400 naming `participants` is shown if the count changed meanwhile.

### D4: How a refusal reads
As in the wishlist: a 409 (the group is no longer in the state the screen showed, the roster frozen) closes the sheet or the confirmation, reloads the group and says `ChangedElsewhere`; a 404 on the group makes the whole screen "no longer available"; a 400 naming `participants` stays in the sheet or on the action; a network failure keeps the sheet and its text, or for a step says the server could not be reached. The server's answer to a change replaces the loaded group through `Loader.replace`, so nothing is shown before the server has accepted it.

### D5: Dates and times are typed
Material 3 date and time pickers are experimental and differ between Android, iOS and the web. The form uses two text fields with a hint (`2026-12-24`, `18:30`) and the domain's own parsing, so the three platforms accept the same input and a typo shows the same sentence everywhere. *Alternative:* pickers; rejected for the cost of verifying them on three platforms, and easy to add later without changing the view model.

### D6: The participants sheet
A bottom sheet (the same Material 3 pattern as the wishlist sheet) over the detail, holding a working copy of the list: one row per username with a remove button, and a field with an Add button. Adding runs `Username.parse` (3 to 32 characters, lowercase letters, digits, `.`, `_`, `-`), lowercases the name and ignores a name already listed. Save sends the whole list. The sheet is only offered in Draft and Pending and is closed with a notice if a save is refused because the roster froze.

### D7: Temporary passwords
`GroupChangeResponse.createdAccounts` becomes `passwords: List<CreatedAccount>?` in the detail view model's UI state, shown in a dialog with each username and password in selectable text and the sentence that each person must change it at first sign-in. The dialog is shown only when the list is not empty. Dismissing sets the state to null; the list is never written to the loaded group, to storage or to a log, so nothing keeps it.

### D8: Tests
Common tests over `ScriptedServer`, extended with a stateful fake of the admin routes (create, edit, replace participants with account creation in Pending, the three steps with the server's rules, delete of a Draft, owner scoping, scripted failures):
- `ApiClient`: every call, 204 on delete, 400 with a field, 404, 409.
- Form: each local check and its field, blanks as empty, create and edit success, a server field error, a network failure keeping the text.
- Detail: actions per phase, the four-participant rule, each step with its confirmation and cancel, the passwords dialog (shown only when accounts were created, gone after dismissal), participants add (invalid, repeated, case), remove and save, 409 and 404 handling, a network failure.
- List: order, empty, unreachable then retry.

The screens are verified by running the app: the Android emulator against a real server with a second account, and headless Chrome for the web.

## Risks / Trade-offs

- [Typed dates are easier to get wrong than a picker] → The hint and the same validation message on every platform; pickers remain a later option (D5).
- [The temporary password dialog is the only chance to see the passwords] → The wording says so and the dialog needs an explicit close; resetting a password is the next admin change.
- [A confirmation can be stale when the group changed elsewhere] → The 409 reloads and says what happened (D4).
- [One shared `NavController` for both views] → A view switch clears the stack and opens the other root, so the two stacks never mix; the participant routes are unchanged.

## Migration Plan

Nothing to migrate: new screens, no server change and no new dependency. Rollback is the previous build.

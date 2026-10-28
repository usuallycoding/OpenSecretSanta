# Design

## Context

The admin group detail (`AdminGroupScreen`, `AdminGroupViewModel`) lists participants and the steps allowed in the group's state. Screens load through `Loader` and `LoadContent`, reload on `ON_RESUME`, treat 404 as "gone" via `isGone()`, and view models are created by Koin and tested over `ScriptedServer` with a real `ApiClient`. The contract already has `AssignmentDto`, `AdminQuestionThreadDto`, `QuestionDto` and `SharedWishlistItemDto`, and `ApiRoutes` has `adminGroupAssignments`, `adminGroupParticipantWishlist` and `adminGroupQuestions`. The participant side already has read-only wishlist and thread rendering (`ReceiverWishlistScreen`, `ReceiverScreen`) that can be reused for the item rows and question rows. The server rules (owner-only, 404 for others, 409 `invalid_state`, read-only, no trace) are in the `admin-oversight` spec.

## Goals / Non-Goals

**Goals:**
- Let the owner read the draw, any participant's wishlist and all question threads, in the states the server allows.
- Keep reading side-effect free.

**Non-Goals:**
- Writing anything, and server changes.

## Decisions

### D1: Routes and entry points
```
 AdminGroupRoute(id)
   |-- "Who gives to whom"   section on the detail (Active, Archived)
   |-- tap participant  ->  AdminParticipantWishlistRoute(groupId, username)
   '-- "Questions" button ->  AdminQuestionsRoute(groupId)   (Active, Archived)
```
The draw is a section on the detail because it is a short list that belongs with the participants. Questions get their own screen because threads can hold up to 30 questions each. The wishlist is its own screen per participant. Both new routes are typed routes in the admin `NavHost`; they sit under the Groups tab, so that tab stays selected. The username goes into the path unescaped, as for users.

### D2: Calls
`ApiClient` gains `adminAssignments(id)`, `adminParticipantWishlist(id, username)` and `adminQuestions(id)` over the existing routes and contract types. Rejections arrive as `Rejected(404, ...)` and `Rejected(409, invalid_state)`.

### D3: View models
```
 AdminGroupViewModel            + Loader<List<Assignment>> loaded when the group is Active/Archived
 AdminParticipantWishlistVM     Loader<List<SharedItem>>
 AdminQuestionsViewModel        Loader<List<Thread>>
```
The assignments load alongside the group and are only requested when the loaded state is Active or Archived, so a Pending group never asks. A failure of the assignments call shows a notice in the section and does not hide the rest of the detail. The wishlist and questions models reload on resume like the others. 404 shows "no longer available" with a back button; 409 shows that the group is not in a state where this can be read (for example the group was moved while the screen was open) with a back button.

### D4: Rendering is shared and read-only
The item row and the question and answer rows used for the participant's receiver view are lifted into shared composables taking plain values, so the owner screens show no buttons that write. The owner's own username is marked "you" in pairs and thread headers, using the signed-in account.

### D5: Tests
Common tests over `ScriptedServer`, extended with a fake of the three routes (owner scoping, states, 404 and 409, scripted network failure):
- `ApiClient`: the three calls, a 404, a 409.
- Detail: assignments requested only in Active and Archived, the owner's pairs marked, failure leaves the detail.
- Wishlist: order kept, empty list, links, 404, 409.
- Questions: thread order, unanswered shown as such, times, empty threads, 409.

Screens are verified by running the app on the Android emulator as the owner against a real server, and on the web in headless Chrome.

## Risks / Trade-offs

- [The owner reading their own Santa pair spoils their own draw] → The server specifies this deliberately ("the owner sees the whole draw"); the section marks the pair so the owner knows what they are looking at.
- [A long thread makes a long screen] → One scrolling list per screen; no paging is needed at 30 questions per thread.

## Migration Plan

Nothing to migrate. Rollback is the previous build.

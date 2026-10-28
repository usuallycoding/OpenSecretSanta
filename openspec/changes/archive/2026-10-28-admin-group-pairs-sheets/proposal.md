# Proposal

## Why

Once the names are drawn, an owner looking at a group sees the same people three times: a participants list, a "who gives to whom" list and, behind buttons, a wishlist screen per person and one long questions screen. The owner usually wants to follow one pair or one person, so they jump between screens and lose their place. A single list of pairs, with the details one tap away in a sheet, keeps them on the group page.

## What Changes

- **One pair list.** On an Active or Archived group, the participants list and the "Who gives to whom" list are replaced by one list of rows `[santa] -> [receiver]`, in the order the server returns them. The owner's own side keeps its "you" mark, and a username whose profile is incomplete gets a `*` after it, with a one-line legend "* profile incomplete" under the list.
- **User sheet.** Tapping either username in a row opens a bottom sheet with that person's details (profile, temporary password while it is temporary) and **Reset password**, with the same confirmation, the same one-time password dialog and no reset on the owner's own account.
- **Pair sheet.** Tapping the arrow in a row opens a bottom sheet titled with the pair. It shows the receiver's wishlist and the thread of questions the Santa asked the receiver with the receiver's answers, read-only.
- **Screens removed.** The participant wishlist screen, the Questions screen, their routes, the "Read wishlist" link on Active and Archived groups and the "Read all questions" button go.
- **Pending keeps a list.** A Draft or Pending group keeps its participants list. In a Pending group "Read wishlist" stays, now opening that person's wishlist in a bottom sheet instead of a screen. Draft offers no reading.
- **Back closes a sheet on Android.** The system back closes an open sheet before leaving the screen. On the web the browser's back button leaves the screen as it does without a sheet (a sheet is not part of the history); Escape closes the sheet there.

Out of scope:
- Server changes (every call already exists), editing anything the sheets show, a pair-scoped questions call, and changes to the Users tab beyond sharing its detail content.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `client-admin-oversight`: the draw becomes the single pair list with marks, the wishlist and the question threads move into sheets, and the user sheet is added.
- `client-admin-groups`: the group detail lists participants only before the draw, and the marks requirement gains the `*`.
- `client-admin-users`: the user detail can also be shown as a sheet from a group.
- `client-app`: the oversight routes go, and back closes an open sheet.

## Impact

- Code: the group detail, a pair list row, a user sheet that reuses the user detail and `AdminUserViewModel`, a pair sheet, a wishlist sheet for Pending, a new view model for one pair's thread, and removal of the questions view model, screen and route, the wishlist screen and route, their strings, Koin entries and tests, plus the README. Nothing in `core` or `server`.
- APIs: none.
- Dependencies: none.
- Web: oversight no longer has its own address-bar entries; sheets are state, not routes.

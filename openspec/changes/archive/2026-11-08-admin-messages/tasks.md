# Tasks

## 1. Keeping announcements on the server

- [x] 1.1 Add `V014__announcements.sql` (the table, the primary key and the cascading foreign key on `groups`), list it in `server/resources/db/migration/index.txt` and add an `AnnouncementsMigrationTest` in the style of `LettersMigrationTest`; verify the migration tests pass, including that deleting a group deletes its announcements
- [x] 1.2 In `core/domain` add `AnnouncementRepository`, `AnnouncementLimits.MAX_PER_GROUP = 200` and the `Announcement` model, and make `SendAnnouncement` take a `Clock` and a repository and keep the announcement after its checks and before any email or push plan; update `SendAnnouncementTest` and its fakes to cover: kept and numbered, kept when every email fails, not kept when refused, the 200th kept, the 201st refused with `InvalidState` and nothing sent; verify the domain tests pass
- [x] 1.3 Add `SqliteAnnouncementRepository` (one transaction taking the group's write lock, refusing at the limit, inserting the next number; a list ordered by number) and wire it and the new `SendAnnouncement` arguments in `ServerModules.kt`, with a repository test in the style of `SqliteLetterRepositoryTest`; verify the server tests pass

## 2. Reading announcements and the push mark

- [x] 2.1 Add `AnnouncementDto`, `ApiRoutes.GROUP_ANNOUNCEMENTS` and its builder to `core/contract`, and a `ListAnnouncements` use case with `forParticipant` (through `ParticipantGroupLoader`) and `forOwner` (through `OwnedGroupLoader`, a Draft refused with `InvalidState`), with a domain test for each case in `announcement-history`; verify the domain tests pass
- [x] 2.2 Add the `GET` routes for participants and for the owner (the existing admin path) in `AnnouncementRoutes.kt` and register the use case in `Application.kt` and `ServerModules.kt`; extend `AnnouncementRoutesTest` for: a participant reading in Pending, Active and Archived, someone who joined later, an empty list, 404 for a stranger and for a Draft, the owner outside the group, 404 for another administrator, 403 for a non-administrator, 409 for the owner on a Draft, the 201st refused over HTTP with nothing sent, and no route that edits or deletes; verify the server tests pass
- [x] 2.3 Add `target: String? = null` to `PushMessage` and `NotificationData.TARGET`, set it to `messages` in `planAnnouncement` only, and write it into the FCM data in `FcmPushSender` when it is set; extend `NotifierTest` and `FcmPushSenderTest` so that the owner's announcement carries the mark and "added to a group" and "names drawn" do not; verify those tests pass

## 3. The page and its data

- [x] 3.1 Add `groupAnnouncements` and `adminGroupAnnouncements` to `ApiClient`, `AnnouncementItem` with its mapping, and `AnnouncementsViewModel(groupId, forOwner, api)` registered in `AppModules.kt`, with a test in the style of `InboxViewModelTest` for: newest first, the time shown as the app shows times, an empty list, a reload, and 404 reading as gone; verify the shared tests pass
- [x] 3.2 Add `AnnouncementsPage` (a `LazyPullPage` with `loadItems`, a keyed `AnnouncementCard` showing the title, the message and the time, the empty text, the gone case with Back) and its strings, including "Group Notifications", the empty text and the close action; verify `./kotlin build -m shared` succeeds

## 4. The card and the icon

- [x] 4.1 Change `GroupInfoCard` so that the information card is always drawn with the three lines and `TBD` for a missing value (the date and time line only when `formatWhen` returns null), add the optional `onOpenMessages` that draws the `CardIconButton(Icons.Filled.Notifications, ...)`, a bell, at the end of the card, centered vertically, and keep the screen-reader text of the lines; add the pure parts (what each line shows) to `GroupFormatTest`-style tests and verify they pass and the build succeeds
- [x] 4.2 Pass `onOpenMessages` from the participant group page (`GroupPage.kt`) for a Pending, Active or Archived group and from the admin screen (`AdminGroupScreen.kt`) for the same states, none for a Draft; verify the tests that built these screens still pass and update any that assumed no card for a group with no details

## 5. Small screens, wide windows, addresses and notifications

- [x] 5.1 Add `AnnouncementsRoute` and `AdminAnnouncementsRoute`, their addresses in `WebAddresses` and their composables in `ShellScreen.kt` (Back on small screens, with the back arrow in the top bar, so both routes count as below a group; on a wide window the same route shows the group page with the pane open, at the route's address, and closing the pane goes back to the group's address), keep Overview selected in the sidebars for them, and extend `WebAddressesTest` for the two addresses, an admin address for a non-administrator, and a trailing slash; verify the tests pass
- [x] 5.2 Lay out the participant group page as a `Row` with the pane (the page centered at the readable width, the pane at `PANE_WIDTH` with its header and close button), add `GroupRoute`, `AnnouncementsRoute` and `AdminAnnouncementsRoute` to the spread routes in `ShellScreen.kt`, and give the admin screen the same pane, shown when its route says so (a rules or wishlist pane shows first and gives way when closed); verify the build succeeds and the admin screen tests pass
- [x] 5.3 Make `NotificationTarget.routesOf` take the `target` and add `AnnouncementsRoute` for `announcements` with the target `messages`, pass the key through `NotificationsAndroid.kt` and `MainActivity.kt` and, for the web app, `firebase-messaging-sw.js` and `push.js`, and extend `NotificationTargetTest` for the new screen, for the other announcements notifications still opening the group and for the address the service worker builds; verify the tests pass

## 6. Verify

- [x] 6.1 Run the full build and all tests (`./kotlin build` and `./kotlin test`) and verify they pass
- [x] 6.2 On a phone and on a wide window, with a group of each state: send announcements by email, by push and by both, see them on the page newest first with the right time, open the page from the icon, from the owner's overview, from a web address and from a tapped notification (and check that "added to a group" still opens the group), see the pane open and close beside the group page with the address following it (`#/groups/{id}/notifications`, and back to `#/groups/{id}`), going back to close it, check `TBD` on a group with no details, no icon on a Draft group, and the refusal text when the 201st announcement is sent (for example by temporarily lowering the limit); note anything that looks off in the change

## Workflow follow-up

- Archive the change after review, then propose `admin-messages-unread` on top of the archived specs.

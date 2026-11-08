# Tasks

## 1. Rename the page to Announcements

- [x] 1.1 Rename the strings (`GROUP_NOTIFICATIONS` to `ANNOUNCEMENTS` and its neighbours: the empty text, the two labels, the close text, the sent-at text) in `Strings.kt` and every use, and reword the comments and KDoc that say "Group Notifications" (`Routes.kt`, `NotificationTarget.kt`, `GroupInfoCard.kt`, `AnnouncementsPage.kt`, `AnnouncementsPane.kt`, `ShellScreen.kt`); verify `grep -rniE "group notifications|GROUP_NOTIFICATION" app core server` finds nothing and `./kotlin build -m shared` succeeds
- [x] 1.2 Change `notifications` to `announcements` in the four places of `WebAddresses.kt` and in `addressOf` of `app/webApp/resources/firebase-messaging-sw.js` (and its comment), and update `WebAddressesTest` and `NotificationTargetTest` to the new addresses; verify the shared tests pass and the service worker's mapping matches the one the test names

## 2. What each person has seen, on the server

- [x] 2.1 Add `V015__announcement_reads.sql` (the table with its primary key, the cascading key on `groups` and the key on `users`), list it in `index.txt`, add its table to the rows `deleteUnassigned` removes in `SqliteUserRepository`, and add an `AnnouncementReadsMigrationTest` (the columns, a group's deletion removing the marks) and a case in `DeleteUserRoutesTest` for deleting a person who has a mark; verify the server tests pass, with the schema version literals now 15
- [x] 2.2 In `core/domain` add `AnnouncementReadRepository` (`seenUpTo`, `markSeen` keeping the later time, `unreadCounts` for many groups), make `InMemoryAnnouncements` implement it over its rows, and add `GetMyAnnouncementReads` and `MarkAnnouncementsRead` (through `ParticipantGroupLoader`, the time clamped to now with the `Clock`, working in an Archived group); verify a domain test for each case of `announcement-read-marks` passes: nothing marked, some seen, each person on their own, Pending and Archived, 404 for a stranger, a Draft and the owner outside the group, a mark never going back, never after now
- [x] 2.3 Add `SqliteAnnouncementReadRepository` (an upsert keeping `MAX(seen_up_to)`, the count with `sent_at` after the mark or all with none, one grouped query for many groups) and wire it in `ServerModules.kt`, with a repository test in the style of `SqliteReadMarkRepositoryTest`, including a removed person keeping their mark; verify the server tests pass
- [x] 2.4 Add `ApiRoutes.GROUP_ANNOUNCEMENT_READS` and its builder, and the `GET` and `PUT` routes (reusing `ReadMarkDto` and `MarkReadRequest`, `400` naming `seenUpTo` for a missing or unreadable time), in `AnnouncementRoutes.kt`; extend `AnnouncementRoutesTest` (or a new route test beside it) for every scenario of the two read-mark requirements over HTTP; verify the server tests pass
- [x] 2.5 Add `unreadAnnouncements` (default 0) to `MemberGroupDto`, make `ListMyGroups` and `GetMyGroup` return `MemberGroup(group, unreadAnnouncements)` from `unreadCounts`, give `toMemberDto` the number and update `memberGroupRoutes`, `ServerModules.kt`, `LifecycleTest` and `GroupDtoSerializationTest`; add route tests for the scenarios added to `groups` (counted, seen not counted, never announced, the list and the detail); verify the domain and server tests pass
- [x] 2.6 Make `SendAnnouncement` take the read port and mark its sender as seen up to the send time when they take part in the group, a failure to mark never stopping the announcement; extend `SendAnnouncementTest` for the participant owner, the owner outside the group, a hand-over and a refused announcement, and `AnnouncementRoutesTest` for one end to end case; verify the tests pass

## 3. The client's data

- [x] 3.1 Add `announcementReads` and `markAnnouncementsRead` to `ApiClient`, add `unreadAnnouncements` to `GroupDetail` and `GroupRow` with their mappings, make `ReadTracker` take its two calls as functions (keeping its existing constructor so letters, answers and the wishlist are unchanged), and extend the fake server in `TestSession.kt` with the reads; verify the existing read-mark and view-model tests still pass
- [x] 3.2 Give `AnnouncementsViewModel` a tracker for a participant and none for the owner, `isNew` on each `AnnouncementItem` from the baseline, and `markSeen`; add tests for: some new, never looked, marks kept across a reload, a new arrival marked, an unreadable baseline marking nothing, the same time not sent twice, a failed mark retried on the next show, and the owner asking for nothing; verify the shared tests pass

## 4. The badges and marks on screen

- [x] 4.1 In `AnnouncementsPage` call `markSeen` once the list is shown and draw the "New" pill and the accent bar on a new announcement's card, as `LetterCard` does; in `GroupInfoCard` make the bell an `UnreadIconButton` with the count passed from the group page ("9+" above nine, spoken "Announcements, 3 new") and none on the owner's screen; verify the build succeeds and the pure parts (the count text and the spoken text) have tests
- [x] 4.2 Show a small bell with the count at the end of the name row and before the state on an entry of the Groups tab, only above zero, with the name keeping its one row and its ellipsis and the spoken text "3 new announcements"; verify the build succeeds and the mapping of the count into `GroupRow` has a test
- [x] 4.3 Give `groupItems` the count for the Overview item of the open group and `SidebarRow` the count and spoken text on each collapsed group's line (the open group's own line carrying none), and reload the group list in `ParticipantSidebar` when the route changes; extend `WideLayoutTest` for the Overview item's count and for no badge on the Groups item; verify the shared tests pass

## 5. Verify

- [x] 5.1 Edit the Purpose of the main spec `openspec/specs/client-announcement-history/spec.md` directly, since a delta cannot change it, to say "Announcements" and verify its Purpose line no longer says "Group Notifications"
- [x] 5.2 Run the full build and all tests (`./kotlin build` and `./kotlin test`) and verify they pass
- [x] 5.3 On a phone and on a wide window: send announcements to a group from the owner and check the bell, the group's entry in the Groups list and the sidebar (a collapsed line and the open group's Overview item) show the count; open the page and see the pill and the accent bar, reload it and see them stay, close it and see the badges go and the marks gone the next time; check an owner who is also a participant has none of their own new, a person who never looked sees all of them, nothing is badged on the admin screens, and the web addresses end in `/announcements` for a page, a pane and a tapped notification; note anything that looks off in the change

## Workflow follow-up

- Archive the change after review.

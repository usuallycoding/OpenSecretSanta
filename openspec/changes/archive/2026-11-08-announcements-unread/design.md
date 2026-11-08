# Design

## Context

Announcements are kept per group and numbered, newest last (`announcements`, `AnnouncementRepository`), read through `ListAnnouncements`, and shown by `AnnouncementsPage` and `AnnouncementsPane`, reached by the bell of `GroupInfoCard` (`AnnouncementsRoute` and `AdminAnnouncementsRoute`, `GroupEntry` and `AdminGroupEntry` in `ShellScreen.kt`). See proposal.md for why and what.

What a Santa has seen of their receiver is kept in `read_marks`, a table hanging off the Santa's assignment, with the kinds wishlist, answers and letters fixed by a `CHECK`. Announcements belong to the whole group and exist in a Pending group, which has no assignments, so those marks cannot be reused. The client side can be: `ReadTracker` asks for a baseline once per visit, remembers the newest time shown, marks it once and never marks while the baseline is unknown, `letterIsNew` compares an item's time to the baseline, and `UnreadIconButton`, `CountBadged` and `countBadgeText` draw the count. A page's marks stay while it is open and clear on the next visit; the badges clear after the person has left, because the group page refreshes on resume and the Groups list does too.

The group's data reaches the client as `MemberGroupDto`, built by `Group.toMemberDto()` from a `Group` alone, through `ListMyGroups` and `GetMyGroup`, which return plain groups. The Groups tab, the sidebar and the group page all read it. `ParticipantSidebar` refreshes the groups only when the app resumes, while the open group's own counts refresh whenever the route changes. `SqliteUserRepository.deleteUnassigned` deletes a user's rows from the tables that reference `users` without a cascade (`wishlist_items`, `group_delivery_info`, `login_attempts`) before deleting the user.

## Goals / Non-Goals

**Goals:**
- Show how many announcements are new on the bell, the group's entry in the list and the sidebar, and mark them on the page, with the existing patterns for baselines, pills and clearing.
- Rename the page to Announcements, in the app, the web address and the specs, before the new code uses the names.

**Non-Goals:**
- Totals on the Groups item or the Groups tab, unread for the owner, any push change, and changes to the Giving and Receiving badges.
- Real-time updates: the counts are as fresh as the last load, as for every other badge.

## Decisions

**The rename comes first and is mechanical.** `Strings.GROUP_NOTIFICATIONS` becomes `ANNOUNCEMENTS` ("Announcements") and its neighbours follow (`NO_ANNOUNCEMENTS` "No announcements yet.", `ANNOUNCEMENT_TITLE_LABEL`, `ANNOUNCEMENT_TEXT_LABEL`, `ANNOUNCEMENTS_CLOSE` "Close announcements", `announcementSentAt`). `WebAddresses` changes `notifications` to `announcements` in its four places, and the service worker's `addressOf` in the same way. The tests of both are updated with them, and so are the comments and KDoc that name the page. Tapping a notification needs nothing else: Android maps the category and target to a route, and the web service worker's mark `messages` selects the address. The Purpose of the main spec `client-announcement-history` still says the old name, and a delta cannot change a Purpose, so it is edited in the main spec directly, as a task of this change. The archived copy of `admin-messages` stays as it was.

**A table of its own, keyed by group and person.** `V015__announcement_reads.sql` adds `announcement_reads (group_id, username, seen_up_to, PRIMARY KEY (group_id, username))` with a foreign key on `groups` that cascades and one on `users`, and no foreign key to the participants, like `wishlist_items`: a person removed from a Pending group keeps their mark and every access checks that the caller still participates. Because the key to `users` has no cascade, `deleteUnassigned` also deletes the table's rows, or an old mark would stop a person from being deleted; a test covers it. `seen_up_to` is milliseconds.

**One port for marks and counts.** `AnnouncementReadRepository` has `seenUpTo(groupId, username)`, `markSeen(groupId, username, atMillis)` (keeping the later time, as `read_marks` does) and `unreadCounts(username, groupIds)`, which returns for each group the number of announcements with `sent_at` after the person's mark, or all of them with no mark, in one query for many groups. The SQLite repository implements it, and the in-memory announcement fake implements both ports over the same rows.

**Two small use cases and the count on the group.** `GetMyAnnouncementReads` loads the group with `ParticipantGroupLoader` (so a Draft, a stranger, the owner outside the group and a missing group are 404) and answers `ReadMarkDto(unread, seenUpTo)`, an existing contract type. `MarkAnnouncementsRead` clamps the time to now with the `Clock` and returns the same, working in an Archived group as `MarkRead` does. `ListMyGroups` and `GetMyGroup` now return `MemberGroup(group, unreadAnnouncements)`, filled by `unreadCounts`, and `toMemberDto` takes the number into the new field `unreadAnnouncements` of `MemberGroupDto`, which defaults to 0 so an older client or a stored value is not affected. Routes: `GET` and `PUT /groups/{id}/announcements/reads` (`ApiRoutes.GROUP_ANNOUNCEMENT_READS`), beside the announcements.

**The sender is marked when the announcement is kept.** After `announcements.add`, `SendAnnouncement` calls `markSeen(group, actor, sentAt)` when the actor is among `group.participants`. That covers an owner who also takes part, and a hand-over, with no stored sender: the new owner marks themselves and the previous owner has not seen it. It takes the read port as a new argument, and a failure to mark is not allowed to stop the announcement, so it is caught like the other best-effort steps and the announcement stays kept.

**The tracker is generalised, not copied.** `ReadTracker` takes the two calls it makes as functions: the baseline `suspend () -> ApiResult<ReadMarkDto>` and the mark `suspend (String) -> Unit`. Its existing constructor, which names the API and a kind, stays and builds the same functions, so the letters, the answers and the wishlist are untouched. `AnnouncementsViewModel` builds one for a participant with `api.announcementReads` and `api.markAnnouncementsRead`, and none for the owner, which gives the owner's page no marks and no requests. Each `AnnouncementItem` carries `isNew`, from `letterIsNew` on its sent time, and the page calls `markSeen` once the list is shown, as the Inbox does, so the same time is never sent twice.

**The count is drawn from the group's data in three places.** `GroupDetail` and `GroupRow` carry `unreadAnnouncements`. The bell is an `UnreadIconButton` in `GroupInfoCard` (new parameter `unreadAnnouncements`, spoken "Announcements, 3 new"), with no count on the owner's admin screen. The Groups entry shows a small bell with `countBadgeText` between the name and the state, in a row where the name keeps its ellipsis and its weight, only when the count is above zero, and says "3 new announcements" to a screen reader. The sidebar's collapsed line gets `count` and the same spoken text, and `groupItems` takes the count for the Overview item of the open group, whose own line then carries none. There is no total anywhere.

**When the counts are fetched.** The bell reads the group page's data, which reloads on resume, and the entry reads the list, which reloads on resume. The sidebar's list reloads only when the app resumes, so `ParticipantSidebar` also reloads it when the route changes, as the open group's own counts already do; the count therefore drops once the person has left the page or closed the pane, which is what the spec says, and a request per navigation is acceptable on a wide window. Alternative rejected: telling the sidebar at the moment the page marks. It needs a path from the page to the list's view model for a badge that may stay a little longer, as the Giving badge does.

## Risks / Trade-offs

- [The sidebar reloads the group list on every route change] → One small request on a wide window, and the open group's counts already reload this often. If it shows up, reload only when the route leaves the announcements.
- [`ListMyGroups` and `GetMyGroup` change their return type, which touches their tests and the routes] → The type is small and the change is mechanical; the tasks update `LifecycleTest` and the route tests with it.
- [Every existing announcement is new to every participant the first time, since nobody has a mark yet] → Wanted: a person who joined later, or before this change, sees what they have not read. Only a few announcements exist so far.
- [200 announcements of 2000 characters are marked as seen by opening the page, though a person may only have read the top] → As for the other lists; the new ones are at the top, shown first.
- [A failed mark leaves the badge up] → The marks come back the next time the page is shown, and the count is the server's.
- [The bell on the owner's screen and the new bell with a count on a list entry are both bells] → The tooltip, the spoken name and the count tell them apart, and the list entry is not a button.

## Migration Plan

The migration only adds a table and the field has a default, so the server can be deployed first. A client that does not know the field shows no badges, and one that does, talking to an old server, shows none either. Rolling back the code leaves an unused table, which can stay.

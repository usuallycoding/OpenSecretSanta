# Proposal

## Why

The owner's announcements can now be read in the app, but nothing says when there is a new one: a person has to open each group's page to find out. Letters, answers and wishlist changes already tell the Santa what is new with badges, a pill and an accent bar; announcements should do the same, on the bell, on the group's entry in the list and in the sidebar. The page is also renamed first, so that the new indicator is built on its final name: it is called "Announcements", which is what the notification switch in Settings and the "Send announcement" action already call them, and its web address ends in `/announcements`.

## What Changes

- **Rename.** The page, its bell tooltip and spoken name, the pane heading, the close button and the empty text say "Announcements" instead of "Group Notifications" ("No announcements yet."). The web addresses become `#/groups/{id}/announcements` and `#/admin/groups/{id}/announcements`, in the app and in the web push service worker's copy of the mapping. The API routes are not renamed.
- **The server keeps what each person has seen.** One time per person and group, the newest announcement time they have seen. Reading it gives the number of announcements sent after that time (all of them when the person has never looked) and the time itself. Marking keeps the later time and never one after now. A removed person keeps their mark; deleting the group deletes it.
- **The group carries the count.** `GET /groups` and `GET /groups/{id}` add the number of announcements the person has not seen, so the list, the sidebar and the bell need no extra request.
- **The sender has seen what they send.** When an announcement is kept, its sender, if they take part in the group, is marked as having seen it, so an owner who is also a participant, and a past owner after a hand-over, never find their own messages new. The owner who does not take part has no marks, and the admin views show no badges.
- **A badge on the bell** of the group's information card, counted as the group's data says it: the number, "9+" above nine, none for none, and the count in its spoken name.
- **A bell with a count on the group's entry in the Groups list**, at the end of the name row and before the state label, only when there are new announcements.
- **A badge in the sidebar** on each collapsed group's line and on the open group's Overview item. There is no total on the Groups item or on the Groups tab.
- **New announcements are marked on the page.** An announcement sent after what the person had seen when they opened the page carries a "New" pill and an accent bar, like a new letter. The page asks for that baseline once per visit and tells the server what it showed as seen once it is on screen. The pill and the bar clear the next time the page is opened. The bell's, the list's and the sidebar's badges clear once the person has left the page or closed the pane.
- Not changed: push notifications, the 200-announcement limit, what the owner can do with announcements, and the Giving and Receiving badges.

## Capabilities

### New Capabilities

- `announcement-read-marks`: what each participant has seen of a group's announcements and how many they have not seen, read and marked on the server.
- `client-announcement-read-marks`: the bell's badge, the new announcements on the page and what looking at them clears.

### Modified Capabilities

- `groups`: a participant's view of a group carries the number of announcements they have not seen.
- `group-announcements`: the sender of an announcement has seen it.
- `client-groups`: a group's entry in the list shows its new announcements, and the information card's icon is named Announcements.
- `client-layout`: the sidebar badges the new announcements of the groups.
- `client-announcement-history`: the page, icon and address are named Announcements (renamed and modified requirements).
- `client-admin-groups`: the card's icon is named Announcements.
- `client-notifications`: tapping an announcement opens the Announcements page.

## Impact

- `server`: a new migration (`V015`), a repository for the marks, the two routes, the count in the group list and detail, and `SendAnnouncement` marking its sender.
- `core/domain` and `core/contract`: the read-mark port, use cases for reading and marking, the `unreadAnnouncements` field of `MemberGroupDto`, and the new routes.
- `app/shared/src`: the strings and the web addresses (the rename), the bell with its badge, the list entry and the sidebar rows, the page's baseline and marks, and refreshes after leaving the page.
- `app/webApp/resources/firebase-messaging-sw.js`: the address the service worker builds.
- The main spec `client-announcement-history` also has the old name in its Purpose, which a delta cannot edit and is fixed in the main spec directly, as a task of this change.
- The API only grows: two routes and one field. A client without the field shows no badges.

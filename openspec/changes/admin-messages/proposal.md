# Proposal

## Why

When the owner of a group sends an announcement, by email, by push notification or both, it reaches people once and is gone: the server keeps nothing, so a participant who missed the notification, turned the switch off or joined later can never read what the owner said. The announcements need to be kept and shown in the app, so that everyone in the group can read them whenever they like. Whether a message is new to the person comes in a second change, `admin-messages-unread`, so this one stays reviewable.

## What Changes

- **The server keeps announcements.** Sending one stores its title, message and time, numbered from 1 within the group, once it has passed the checks and whatever becomes of the email and push notifications. Nothing is stored for a refused announcement. A group holds at most **200**; sending the 201st is refused with 409 and nothing is sent. Messages sent before this change were never kept and cannot be shown.
- **Reading them.** Participants read a group's announcements in a Pending, Active or Archived group, and the group's owner reads them from the admin overview, even when not in the group. Both get every announcement of the group, in the order sent.
- **The group information card changes.** The card on the group page and on the admin group screen now always shows three lines: the calendar icon with the date and time, the map pin icon with the location and the bank icon with the budget, each with `TBD` where the group has no value. The card is always there, so it can hold the new icon. This reverses "parts the group does not have are left out" and "a group with none of them shows no card". A date without a time still shows the date alone, and `TBD` replaces the date and time line only when both are missing. The description keeps its own optional card.
- **The "Group Notifications" icon.** A bell icon at the end of the card, centered vertically, with the tooltip "Group Notifications", in a Pending, Active or Archived group, for participants and for the owner. A Draft group has no messages and no icon.
- **The Group Notifications page.** A list, newest first (a lazy list, like the lists of letters and questions), with each message's title, message and the date and time it was sent, and a short text when there are none. It opens as a new page on small screens, with a web address, and, on a wide window, in a pane to the right of the group page when the icon is tapped, with a way to close it. Both have the web address `/notifications` of their group, which the browser shows while the page or the pane is open and drops once it is closed.
- **Tapping an announcement notification opens Group Notifications** (after the group page), instead of the group. The other notifications of the announcements kind, being added to a group and the names being drawn, still open the group.
- The owner still cannot edit or delete a sent announcement.

## Capabilities

### New Capabilities

- `announcement-history`: reading the announcements kept for a group, by its participants and by its owner.
- `client-announcement-history`: the "Group Notifications" icon, page and pane, their list, their web address and how a wide window lays them out.

### Modified Capabilities

- `group-announcements`: sending also keeps the announcement, and a group holds at most 200.
- `client-groups`: the information card is always shown, with the three lines and `TBD`, and holds the messages icon.
- `client-admin-groups`: the same card on the admin group screen, with the icon.
- `client-notifications`: tapping an announcement from the owner opens Group Notifications.
- `push-notifications`: an announcement notification says it is a message from the owner, so that it can be told from "added to a group" and "names drawn".

## Impact

- `server/resources/db/migration` (a new migration and `index.txt`), a new SQLite repository, the routes (`AnnouncementRoutes.kt` and a read route for participants), `Application.kt` and `ServerModules.kt`.
- `core/domain` (the announcement repository port, the limit, `SendAnnouncement` keeping what it sends and a use case to list them, and a field on `PushMessage`), `core/contract` (an announcement DTO, the two routes, the push data key).
- `app/shared/src`: the card (`GroupInfoCard`), the group page and admin screen (the icon, the pane), a new view model and page, the routes and web addresses, the shell (the group page spreads on a wide window), `NotificationTarget`, strings, and `app/androidApp` for the push data.
- The API only grows: the two read routes and a new response code (409 for the limit) on an existing route. Existing data is not touched.

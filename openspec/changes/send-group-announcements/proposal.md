# Proposal

## Why

When something changes after a group is set up (the time moves, the place changes, a reminder is due), the owner can only tell people outside the app. The app already has what it takes to reach participants, emails and push notifications, but both are used only for automatic events. An owner needs to be able to send their own announcement, to the right people, through the channel that reaches them.

## What Changes

- An administrator can **send an announcement** to the participants of a **Pending or Active** group they own: a title and a message, sent **as an email, as a push notification, or both** ("Send as", at least one).
- **Email**: one email per participant who has an address, laid out like the draw email (greeting, the group's description, date, time and place, the app link, "Keep it secret!") with the owner's message in place of the draw lines. The subject is `<title>: <group name>`.
- **Push notification**: carries only the message (cut at about 300 characters, with an ellipsis) under the title `<title>: <group name>`. It goes to participants who have a device registered and the "Announcements" switch on, and not to the sender. It is delivered like every other push, in the background.
- The answer reports **each channel separately**: for email, how many were sent, failed, or have no address; for push, how many people it was sent to and how many have notifications off or no device.
- The admin group page gets a **"Send announcement"** button on Pending and Active groups. It opens a bottom sheet with the "Send as" checkboxes, the title, the message and a preview of the subject, and a confirmation is asked before anything is sent.
- A channel the server has not set up is **shown but greyed out with its reason** ("Email is not set up on this server", "Push notifications are not set up on this server"); with neither available, Send is off. The server tells the app which channels it has through two new flags on the signed-in account's answer, and refuses a request that names a channel it does not have, sending nothing.
- The description of the "Announcements" switch in the notification settings now also covers messages from the organiser.
- Nothing is stored: there is no history of announcements.

## Capabilities

### New Capabilities
- `group-announcements`: the server side: who may announce and to which groups, the channels, the email and the push content and recipients, the limits, the per-channel results, and what happens when a channel fails or is not set up.
- `client-group-announcements`: the app side: the "Send announcement" button, the sheet with "Send as", title and message, the greyed-out channels, the push-length hint, the confirmation, and the result.

### Modified Capabilities
- `user-accounts`: `GET /me` also says whether the server can send email and push notifications.
- `client-notifications`: the "Announcements" toggle's description covers messages from the organiser.

## Impact

- **Contract**: a new route and request/response types; `AccountDto` gains `emailAvailable` and `pushAvailable` (false when absent, so older servers read as "not available").
- **Server**: a new use case and route; the email layout code shared with the draw email; a way to plan the push to a group's participants and report the counts, with the sending queued as for other pushes; the two flags on `GET /me`.
- **App (shared)**: a view model and a sheet for the announcement, a button on the admin group page, strings, and the reworded toggle description.
- **README**: a short paragraph next to the section on emails.
- **Not affected**: the draw email and its summary, who receives which automatic push, the notification switches themselves, and every other screen.
- **Limits to know**: Resend's free plan allows 100 emails a day, so an announcement to a large group uses a good part of it; a failed email is counted and not retried; replies go to the sender address, which is normally a no-reply one.

# Design

## Context

Today (`NotificationDispatcher`, `Notifier`): every wishlist change, question, answer and letter is *held* in memory per person, group and kind (60 s quiet, 5 min at most) and then pushed to the one person it concerns, on a category the person can switch off (five switches in `notification_prefs`, mirrored in `NotificationSettings`, `NotificationSettingsDto` and the Notifications sheet). Announcements go out immediately and are not held. Android shows a push only if its `category` is one of its notification channels (`Channel` in `NotificationsAndroid.kt`), and opens a tap from `EXTRA_GROUP_ID`; the web service worker already falls back to `#/groups` when a push has no group. See proposal.md for why and what changes.

## Goals / Non-Goals

**Goals:**
- One digest per person per day at 20:00 Bucharest time, only when something happened, secret by construction.
- Marks survive a restart; the digest never fails or slows the action that caused it.
- Existing settings carry over by the rule in the spec (Daily on if any of the four was on).

**Non-Goals:**
- No change to announcement pushes.
- No catch-up digest after downtime, no per-group or per-kind digest, no "quiet if the person already looked" (read marks are not consulted).
- No new API version, no change for iOS (it has no push).

## Decisions

**Persist a mark per person (`digest_marks`), not derive it from timestamps.** A mark is set when an event happens and cleared by the digest. Deriving "what changed since yesterday 20:00" from `createdAt`/`updatedAt` would miss deletions (a deleted wishlist item leaves no row) and would need a stored "last digest" time anyway. The recipient logic already in `Notifier` (`santaOf`, `receiverOf`, Active-group check) is reused to decide whom to mark; only the action changes from "send" to "mark".

**`DigestMarkRepository` in the domain with `mark(username, atMillis)` and an atomic `takeAll(): List<Username>` (select and delete in one transaction).** The digest job asks for everyone marked and clears them at once, so a mark set while the job runs lands in the next day's digest, never in both or neither. Marks are cleared even when the person has Daily off or push is off, so switching it on later does not release stale news.

**A daily trigger computed with `java.time` and the existing `Scheduler`.** After each run the next one is scheduled for the next 20:00 in `Europe/Bucharest` (`ZonedDateTime`), so summer time is handled by the zone rules, not by a fixed 24 h period. The clock and scheduler are the existing injectable ones, so tests drive the digest without waiting. Missed runs are not made up (spec: "Missed digest").
- *Alternative: a fixed 24 h period.* Rejected: drifts across the clock changes.
- *Alternative: an external cron.* Rejected: the server already owns scheduling and the deployment is a single container.

**`PushMessage.groupId` becomes nullable and the FCM sender omits the `groupId` field when it is null.** The digest has category `daily` and no group. `NotificationCategory` becomes `ANNOUNCEMENTS, DAILY`.

**Settings: two switches in the domain, the old four kept only at the edge.** `NotificationSettings(announcements, daily)`. `notification_prefs` gets a `daily` column; V018 backfills it with 1 when any of `wishlist`, `questions`, `answers`, `letters` is 1 and with 0 when all four are 0; people with no row stay at the default (on). Saving writes the four old columns with the Daily value so an older server reading the same file would still behave. The DTO keeps `wishlist`, `questions`, `answers`, `letters` (always equal to Daily when reported) and adds `daily`; the request makes `daily` optional and, when it is absent, requires the old fields as before and derives Daily from them (any on means on). `GET /me`-style availability reporting is unchanged.

**Remove the hold, not just stop using it.** `NotificationDispatcher` loses `Held`, `hold`, `QUIET_MILLIS` and `MAX_WAIT_MILLIS`; its question/answer/letter/wishlist entry points call `notifier.mark…` through the same `run { }` wrapper (so failures are logged, never thrown). Tests for the hold are replaced by tests for marks and for the digest.

**Clients.** `NotificationTarget.routesOf` takes a nullable group; for `daily` it returns no routes and `openNotification(emptyList())` leaves just the group list. `MainActivity.routesOf` must also treat `category == daily` without a group as a launch to the group list (today a tap with no group falls through to link handling and does nothing). Android channels become `announcements` and `daily`; `createNotificationChannels` deletes the channel ids that no longer exist. The sheet shows two toggles with the "Daily digest" description from the spec. The web service worker needs no change (no group already means `#/groups`); that is checked, not assumed.

## Risks / Trade-offs

- [Older Android apps do not know the `daily` category and show nothing for it, and the four old categories no longer arrive] → They lose all four kinds until updated. Accepted, as the app is distributed by the server's downloads folder and updated by hand; announcements keep working. The web app ships with the server.
- [The server is down at 20:00] → That day's digest is skipped; the marks stay and go out the next day.
- [Clocks change] → Next-run time computed from the zone, with a test for both transitions.
- [A burst of sends at 20:00 for many people] → `PushSender.send` already takes a list; the job runs on the notification executor, off the request path.
- [A stuck mark for a person who is never notified] → Marks are cleared every run whatever the outcome, so they cannot pile up.
- [The Purpose paragraphs of `push-notifications` and `client-notifications` still describe four or five kinds] → Edited by hand when the change is archived, since a delta cannot change a Purpose.

## Migration Plan

1. Deploy the server with V018 (adds `daily`, backfills it, creates `digest_marks`). Older apps keep working against the settings endpoint through the four old fields.
2. Publish the new Android build in the downloads folder and the new web build with the server.
3. Rollback: the old columns are still written, so an older server reads the same choices; `digest_marks` is simply unused.

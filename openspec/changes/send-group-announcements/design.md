## Context

Draw emails (`SendDrawEmails`, `drawEmail`) and push notifications (`Notifier`, `NotificationDispatcher`) already exist and are independent. An announcement is a third message that reuses both, written by the owner and valid in Pending and Active groups.

## Goals / Non-Goals

**Goals:** send an owner-written message by email and/or push; report per channel; reuse the draw email layout and the existing notification pipeline and switches.

**Non-Goals:** history, scheduling, retries, replies, per-participant targeting, rich text, attachments.

## Decisions

- **Contract.** `POST /admin/groups/{id}/announcements` with `AnnouncementRequest(title, body, email, push)` returns `AnnouncementResponse(emails: EmailSummaryDto?, push: PushSummaryDto?)`; `PushSummaryDto(reached, notReached)`. `AccountDto` gains `emailAvailable` and `pushAvailable` (default false).
- **Domain use case `SendAnnouncement`.** Loads the group with `OwnedGroupLoader`, requires Pending or Active (`InvalidState`), validates (`ValidationFailed`: `title`, `body`, `channels`), then checks availability (`InvalidState`) so order is 400, 409 state, 409 availability. Nothing is sent before every check passes.
- **Email layout shared.** `drawEmail` is split into helpers (details block, footer) used by it and by a new `announcementEmail`; the draw email text must stay byte-identical, covered by the existing tests. Subject is `"${oneLine(title)}: ${oneLine(group.name)}"`.
- **Push.** `Notifier.planAnnouncement(group, title, text, sender)` returns the `PushMessage`s plus reached/not-reached counts, using the ANNOUNCEMENTS category, so switches and devices are honoured. Text is cut at 300 characters with an ellipsis (Firebase data messages are limited to about 4KB). The route hands the messages to `NotificationDispatcher`, which sends in the background. Counts are therefore "queued for" at send time.
- **Availability.** Email is available when a mailer is enabled, push when a push sender is enabled. A small provider is injected into the `GET /me` mapper; elsewhere `User.toDto` keeps the flags false.
- **Failure handling.** Email failures are counted, never thrown, and never block push. Logs carry no addresses, tokens, titles or messages.
- **Client.** `ApiClient.announce`; an `AnnouncementViewModel` loads `account()` for the flags and holds title, body, channels, sending, result and error. Text fields use Compose state, not StateFlow. The sheet uses `SheetFrame`, `SheetTextField`, `MultiLineTextField`, with an `AlertDialog` for confirmation. The button goes in the `AdminGroupScreen` header `FlowRow`.

## Risks / Trade-offs

- Resend free tier allows 100 emails a day and a send is not retried, so failures are visible only in the counts.
- Emails come from a no-reply address, so replies go nowhere; the message should say where to ask.
- Push counts say who the message was queued for, not who received it.
- The availability flags cost no extra request: the sheet reuses `account()`.

## Open Questions

None.

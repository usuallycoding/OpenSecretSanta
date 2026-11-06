# Design

## Context

Questions are the model: a `questions` table hanging off the Santa's assignment, use cases over `ParticipantGroupLoader`, routes named by relationship (`/receiver/...` is called by the Santa, `/santa/...` by the receiver, no username in paths or bodies), and a SQLite repository that takes the write lock by touching the group row while it is Active. Notifications go through `Notifier` and `NotificationDispatcher`, which already hold questions, answers and wishlist changes by (kind, group, person); settings are four switches stored in `notification_prefs`, read and written whole by `PUT /notifications`. The app opens a notification by its category in `NotificationTarget`. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Letters that are one way, final and private to the pair, sent only while Active and readable after.
- The same shapes as questions wherever possible, so the code and the reading of it stay familiar.

**Non-Goals:**
- No replies, edits, deletes, attachments, unread counts or read receipts; no email of letters; the owner reads but never writes.

## Decisions

- **Routes by relationship, mirroring questions.** The receiver sends and reads their own sent letters at `/groups/{id}/santa/letters` (`POST`, `GET`); the Santa reads at `/groups/{id}/receiver/letters` (`GET`). The server finds the other person from the assignment, so no route or body names anyone. Alternative: one `/letters` route that returns a different view per caller; rejected, the explicit paths cannot be mixed up and match questions.
- **A `letters` table keyed like a questions thread.** `(group_id, receiver, number)` with `title`, `text`, `sent_at`, a foreign key to the group with `ON DELETE CASCADE`, and `number` counting from 1 within one receiver's letters. The repository checks the group is Active and takes the write lock exactly as `SqliteQuestionRepository` does, so numbering and the 30 cap cannot interleave. The Santa is looked up from `assignments` where `receiver` is the sender. Alternative: reuse the questions table; rejected, letters have a title and no answer and a different direction.
- **Limits in one object, `LetterLimits`.** Title 1 to 100 and text 1 to 2000 after trimming, 30 per receiver. Validation errors name `title` or `text`, as `ValidationFailed` does elsewhere; Draft and Pending refuse reading as well as sending (`invalid_state`) because there is no Santa yet, Archived refuses sending only.
- **The owner reads through one oversight route, like questions.** `GET /admin/groups/{id}/letters` returns, for every pair, the Santa, the receiver and the receiver's letters, owner-only (another admin is told the group does not exist, a non-admin is refused), only while Active or Archived (Draft and Pending are `invalid_state`), read-only and never recorded, exactly as `GET /admin/groups/{id}/questions` does. It is a use case over `OwnedGroupLoader` and the letter repository, so the owner can already see the whole draw and nothing about who is whose Santa is widened. A privacy test compares every participant response before and after repeated owner reads. Nothing is added that writes. Alternative: a route per participant like the wishlist's; rejected, one request per sheet is enough and the sheet filters to its pair, as it does for the questions thread.
- **The pair sheet gets a letters part.** `AdminPairLettersViewModel` loads the route once for the group and the sheet shows the chosen pair's receiver's letters as read-only `LetterCard`s under the questions, newest first, with its own loading, failure and "Try again" like the other parts. The letters are shown under "Letters from" and the receiver's username. Deleting a group removes letters through the cascade. A drawn group always has a draw, so the deletion confirmation already appears; letters are not counted in it separately.
- **A fifth switch, `LETTERS`, on by default and tolerant of old clients.** `NotificationCategory` and `NotificationSettings` gain `letters`; `notification_prefs` gets a `letters INTEGER NOT NULL DEFAULT 1` column in the same migration as the table, so existing people have it on. `NotificationSettingsDto` has the new field; `NotificationSettingsRequest` gives it a default of true, so an older client that sends four switches is accepted and re-saves letters as on. Alternative: require all five; rejected, it would break installed apps.
- **Letters are held like questions.** `NotificationDispatcher` gets a fourth `Kind`, `LETTER`, keyed by (group, receiver), with the same constants; `Notifier.letterSent(groupId, receiver, count)` finds the Santa when the timer fires and uses `NotificationTexts.letterSent(group, count)` ("a letter" for one, "letters" for several, never how many, never the title or text). Category `letters`.
- **App: two routes, two pages, one sheet.** `SendLetterRoute(groupId)` and `InboxRoute(groupId)` are plain typed routes with web addresses `#/groups/{id}/letters` and `#/groups/{id}/inbox`. Two small view models load their list with the existing `Loader` and reload on resume; the Send page's also sends. A `LetterCard` (a `SnowCard`) shows title, text and time. The "+" uses the existing `AppFab`; its sheet is a new `LetterSheet` with two fields (the existing `TextSheet` has one) that checks the same limits before sending and shows each problem next to its field. The page's own title does not name the Santa; the Inbox title uses the receiver's username once known, from the same call the Secret Santa card already makes.
- **Buttons where the role is.** "Send letter" goes into `YourStuffCard` on its own row under the first row, and "Inbox" into `SecretSantaCard` on its own row above "Delivery Info", both from Active on. "Inbox" is disabled while the receiver is not known, like the other buttons of that card.
- **Notification target.** `NotificationTarget` maps `letters` to `InboxRoute` over the group page, as `questions` maps to the Receiver page.

## Risks / Trade-offs

- [Old clients re-save the switches without letters] → they send four, the default makes letters on; the person's earlier "off" for letters can only be set by a client that knows it, which is the one that has the switch.
- [A receiver can send up to 30 long letters] → capped, and the Santa's push is held so a burst is one notification.
- [A letter's title or text could identify the sender to people who see a screen] → they are shown only to the pair and to the owner, who already sees the whole draw; push texts never carry them.
- [The owner can read private-feeling letters] → chosen explicitly, read-only and never recorded; it is the same trust the owner already has over questions and answers.
- [Held letter notifications are forgotten on a restart] → as for questions: the letter itself is stored and shows in the Inbox.
- [A drawn group's deletion dialog does not mention letters] → the dialog already warns about the draw and the rest; extending its counts is out of scope.

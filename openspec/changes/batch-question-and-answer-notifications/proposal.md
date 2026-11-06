# Proposal

## Why

A wishlist being filled in already makes one notification, but every question asked and every answer given sends its own push at once. A Santa who asks five questions, or a receiver who answers five, sends five notifications in a minute. Notifications that pile up like that feel like noise, and noise is why people turn them off, which defeats the point of having them.

## What Changes

- Questions and answers are held back like wishlist changes: the push is sent once the person has been quiet for a minute, and at the latest five minutes after the first one of the batch, so a burst makes one notification and a long session makes one every five minutes.
- A batch is kept per kind, per person and per group: questions and answers are separate from each other and from wishlist changes, and different people or groups never merge.
- When a batch holds several questions or several answers the text is plural: "Your Secret Santa has questions for you in <group>" and "Your receiver answered questions in <group>". A single one keeps today's singular wording.
- The texts still name only the group, never a Santa, a question or an answer.
- Not changed: the wishlist's own timing and wording, the four switches, announcements, the draw notification, and what opens when a notification is tapped.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `push-notifications`: "Questions and answers" is modified (batching, timing, plural wording, apart by kind, person and group) and "Notifications keep the Santa secret" is modified only so its question scenario allows the plural wording.

## Impact

- Server and domain. `NotificationDispatcher` holds questions and answers like wishlist changes, counting what it holds; `Notifier` and `NotificationTexts` take that count for the wording. The README's push section says it. No client, contract or database change. Held notifications live in memory, so a restart forgets what was waiting.

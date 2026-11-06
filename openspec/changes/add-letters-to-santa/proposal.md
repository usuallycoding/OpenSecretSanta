# Proposal

## Why

A receiver can answer their Santa's questions, but cannot say anything of their own: a thank-you, a hint, or a note about what they like. Letters give the receiver a one-way voice to their Santa that does not reveal who the Santa is, and give the Santa something warm to read, which suits a gift exchange.

## What Changes

- A receiver can send their Santa letters, each with a title and a description (text), both required, while the group is Active. Letters are one way and final: no replies, edits or deletes.
- Two new buttons, placed where each person's role is: **Send letter** in the "Your stuff:" card (the receiver's card, below Edit Wishlist and Respond to Santa) and **Inbox** in the "You're Secret Santa for:" card (the Santa's card, above Delivery Info), both from Active on.
- A **Send letter** page lists the letters the person has sent, newest first, with a round "+" button that opens a bottom sheet with a title field and a description field, both required. An **Inbox** page lists the letters the person's receiver sent them, newest first, read-only. Both stay readable in an Archived group; sending ends with Active.
- Limits like the questions': a title of 1 to 100 characters, a description of 1 to 2000, and up to 30 letters per receiver per group.
- A new notification switch, **Letters from your receiver**, on by default: when the receiver sends letters their Santa is notified, held like questions (one push after a minute of quiet, at most five minutes after the first, plural "letters" for several). The text names only the group, never the title or the text, and tapping it opens the Inbox.
- Letters are private to the pair and the group's owner: no other participant and no other admin can read them. The owner reads them read-only, in a new oversight route and in the pair sheet next to the questions, and nothing about the read is visible to anyone. Letters go with the group when it is deleted.

## Capabilities

### New Capabilities

- `letters`: the server's letters: sending, reading your sent letters, reading the letters from your receiver, one-way and private to the pair and the owner.
- `client-letters`: the Send letter and Inbox pages, the letter sheet, how letters are shown and their web addresses.

### Modified Capabilities

- `admin-oversight`: one added requirement, the owner reads all letters.
- `client-admin-oversight`: one added requirement, the pair sheet shows the letters.
- `push-notifications`: "Four notification categories" is replaced by "Five notification categories", "Letters from your receiver" is added, and "Notifications keep the Santa secret" gains a letter scenario.
- `client-notifications`: "Four toggles" is replaced by "Five toggles", and "Tapping a notification opens its screen" opens the Inbox for a letter.
- `client-group-pages`: "The Secret Santa card" gains the Inbox button and "The Your stuff card" the Send letter button.

## Impact

- Server, contract and client. A migration adds a `letters` table (cascading with the group) and a `letters` column on the notification preferences (default on); `core/domain` gets the letter limits, four use cases (send, read what you sent, read what your receiver sent, and the owner's read of all letters) and a repository; `core/contract` gets the DTOs and routes; the server gets the routes, the SQLite repository and the fifth switch; the notifier and dispatcher get the letter kind; the server gets a read-only owner route for letters and the app's pair sheet a letters part; the app gets two pages, a sheet, two buttons, two routes, web addresses, a notification target and a fifth toggle. README and tests on both sides. The group deletion confirmation is unchanged: a group that has letters has been drawn, so it already asks.

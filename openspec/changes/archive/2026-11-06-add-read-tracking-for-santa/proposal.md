# Proposal

## Why

A Santa has three places where their receiver's side changes over time: the wishlist, the answers to their questions and the letters. Nothing says which of it is new, so the Santa re-reads everything or misses a change. Badges on the buttons and a clear mark on what is new tell them where to look, and clear themselves once they have looked.

## What Changes

- The server keeps, per Santa and group, the newest time seen of three kinds: the receiver's wishlist, the answers to the Santa's questions and the letters. `GET /groups/{id}/receiver/reads` gives the number of unread changes and the time seen up to for each; `PUT /groups/{id}/receiver/reads/{kind}` records what the Santa has seen. Kept on the server so that every device of the Santa agrees.
- Unread means: a wishlist item created or changed after the time seen (once each), an answer given or changed after it, a letter sent after it. A kind never marked counts everything.
- The "See Wishlist", "Ask Questions" and "Inbox" buttons of the "You're Secret Santa for:" card carry tertiary badges with those counts ("9+" above 9, nothing at 0).
- On entering the Gifter page's wishlist tab, new items show a "New" pill and edited items an "Edited" pill; on the questions tab new answers show "New" beside the answer's time; in the Inbox new letters show "New". Every marked card also has an accent bar on its start edge, and the pills and bars are tertiary.
- The marks hold while the page is open and are gone the next time it is opened, because the app marks what it showed as seen. Each tab is marked only when it is shown.
- The marks are private to the Santa: nothing a receiver, another participant or the owner gets contains a mark, a count or a time of reading, and the receiver's responses do not change. This keeps "Reading a wishlist reveals nothing about the reader". Marks go with the group when it is deleted.
- Not changed: the receiver's own pages and buttons ("Respond to Santa" keeps its unanswered count), notifications, and anything the owner sees.

## Capabilities

### New Capabilities

- `read-tracking`: the server's read marks of a Santa: what is unread, marking as seen, privacy to the Santa, and going with the group.
- `client-read-marks`: the badges, pills and accent bars in the app and how looking clears them.

### Modified Capabilities

None. `gift-visibility`'s "Reading a wishlist reveals nothing about the reader" is unchanged and remains true.

## Impact

- Server, contract and client. A migration adds a `read_marks` table that cascades with the Santa's assignment; `core/domain` gets the read kinds, a repository and two use cases that count with the existing wishlist, question and letter repositories; `core/contract` gets the DTOs and routes; the server gets the routes and the SQLite repository; the app gets the reads call, pure mark functions, badges, pills and bars, and marks pages as seen. README and tests on both sides, including a privacy test and the migration's schema-version assertions.

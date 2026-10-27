# Proposal

## Why

A Santa can already read their receiver's shipping details and ask them questions, but there is no standing place where someone says what they would actually like. A wishlist answers that up front, saves the Santa from asking the same things, and gives a receiver something to keep current as their wishes change. What someone wants depends on the exchange (an office party is not a family gathering), so each participant keeps a separate wishlist in each group they take part in.

## What Changes

- Each participant keeps one wishlist per group: add items, edit them, delete them and put them in the order they prefer. An item has a title, an optional note and an optional link (`http` or `https` only). A person in several groups has a different list in each.
- A list is available once the group has left Draft: it can be read and edited while the group is Pending or Active, and is read-only once the group is Archived.
- New items go to the end of the list. Reordering sends the complete ordered list of item ids, so it is atomic and a stale second device gets an error instead of scrambling the list.
- A list holds at most 50 items; titles are 1 to 200 characters, notes up to 1000 and links up to 2000.
- A Santa can read their receiver's wishlist for that group (read-only) while the group is Active or Archived. The Santa's view carries no item ids and each item's timestamps, and reads are never recorded, so a receiver cannot learn who looked.
- A wishlist is private to its owner and to that owner's Santa in that group; admins get no access in this change.
- Migration `V006` adds the `wishlist_items` table, tied to the group so that deleting a group removes its lists.

Out of scope:
- Notifications. They are planned as a follow-up: every wishlist add or change, and every answer created or edited, will notify the other party in the app. When that is built, a notification sent to a receiver must never reveal who their Santa is. Nothing for it is built or stored here beyond the timestamps already kept.
- Admin access to wishlists (a separate `admin-oversight` change follows), marking an item as bought, copying a list between groups, client screens.

## Capabilities

### New Capabilities
- `wishlist`: a participant's own list of wished items in one group: adding, editing, deleting, reordering, the limits and link rules, when the list is editable, and who may read it.

### Modified Capabilities
- `gift-visibility`: gains a requirement that a participant can read their own receiver's wishlist for the group in an Active or Archived group, without that read disclosing anything about the reader.

## Impact

- Code: new wishlist model, repository port and use cases in `core/domain`; DTOs and routes in `core/contract`; a wishlist repository, `V006` migration and routes in `server`; the README.
- APIs: new `/groups/{id}/wishlist...` routes and `/groups/{id}/receiver/wishlist`; `/health` reports schema version 6.
- Dependencies: none.
- Systems: no new environment variables.

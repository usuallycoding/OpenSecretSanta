# Proposal

## Why

The draw is a pure shuffle, so an admin cannot say "Ana must give to Bo" (a gift already promised) or "Cy must never give to Di" (a couple, or a feud). Admins work around it by redrawing outside the app, which the app cannot do: the draw is fixed once made. Rules let the admin say it up front, while the group is Pending and the roster is still open.

## What Changes

- A Pending group gets **draw rules**, owned and edited only by the group's owner. A rule is one-way: *giver must give to receiver* (**must**) or *giver must not give to receiver* (**never**).
- The owner's add form has a "both ways" toggle that stores two rules (A to B and B to A); it is only a shortcut and is not stored as such.
- A rule set is checked when saved and **rejected** if it cannot be satisfied: a giver or receiver with two musts, a must that is also a never, a must that closes a small circle (the draw must stay one closed chain through everyone), or nevers that leave no valid chain. The message names the conflict.
- Activation honours the rules: the draw is a random search for one closed chain that contains every must and no never, still using the secure source of randomness. If the roster changed in a way that makes the rules impossible, activation answers `409` with a new code and the group stays Pending.
- Removing a participant in Pending deletes the rules that name them and tells the owner which rules went.
- Active and Archived groups keep their rules, which the owner can read but not change.
- The owner's group page gets a gavel icon button in Pending, Active and Archived. On a narrow screen it opens a new page; on a wide screen it opens the same content in the pane beside the group page. The page lists the rules, and in Pending adds and removes them. The page says the rules reveal part of the draw to an owner who also takes part, and warns when the rules leave only a few possible draws.
- Participants never see that rules exist: no participant endpoint or view carries them.
- The gavel is a small icon drawn in the app (an `ImageVector`), not a new icon dependency.
- The 4-participant minimum is unchanged.

## Capabilities

### New Capabilities

- `draw-rules`: the server's rules of a group: what a rule is, who can read and change it, when, how a rule set is validated, how a roster change cleans them up, and that they are hidden from participants.
- `client-draw-rules`: the owner's rules page in the app: the gavel entry point, the list, the add form with the "both ways" toggle, deleting, read-only after Pending, the wide-screen pane and the notices.

### Modified Capabilities

- `gift-assignment`: the draw honours the group's rules (musts present, nevers absent) and keeps every other guarantee, including the single cycle.
- `group-lifecycle`: activation can be refused with `409` when the rules cannot be satisfied.
- `groups`: removing a participant in Pending also removes the rules that name them.
- `client-admin-groups`: the group page header gains the gavel icon button.
- `client-app`: the rules page has a readable web address, `#/admin/groups/{id}/rules`.

## Impact

- `core/domain`: new rule types and use cases; `AssignmentPlanner` becomes a constrained search; `ActivateGroup` and `SetParticipants` change; `Ports.kt` gains a rules repository.
- `core/contract`: new DTOs and routes (`/admin/groups/{id}/rules`).
- `server`: a new `draw_rules` table and migration, a SQLite repository, rule routes, activation and participant-change wiring, tests.
- `app/shared`: the rules page and view model, the gavel icon, a new route and its web address, a `GroupSheet` case for the wide pane, strings, tests.
- Draws made before this change are unaffected; groups already Pending simply start with no rules.
- Rules are not offered in Draft (the roster is still only names) and no participant view changes.

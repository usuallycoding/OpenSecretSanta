# Proposal

## Why

Twenty-two requirements in ten specs are over 500 characters, each stating several behaviours. `openspec validate --strict` flags requirement text over 500 characters, so those specs validate only with warnings. Splitting them so that each requirement states one behaviour clears the warnings without changing what the app or the server does. (`client-admin-oversight` and `client-app-links` were split earlier and are not part of this.)

## What Changes

- Each long requirement keeps its header, its first behaviour and every one of its scenarios. Every other behaviour it stated moves into its own new requirement, with its own scenario, worded as the original sentence was.
- The sentences are kept as written, apart from naming what they refer to ("This list" becomes "The pair list", "the sheet" names the sheet) where a sentence now stands alone.
- 22 requirements are shortened and 45 requirements are added. No scenario is removed or reworded.
- No change to behaviour, code or any other spec.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-admin-groups`: "Group detail shows who takes part", "Marks on participants", "Duplicating a group" and "Deleting a group" are split.
- `client-admin-users`: "User detail" is split.
- `client-app`: "Session storage per platform", "Views by role", "Readable web addresses", "Opening a web address", "Changing your own password" and "Staying signed in on the web" are split.
- `client-group-pages`: "Pair rows", "User details sheets", "Receiver page", "Gifter page" and "Page loading and refreshing" are split.
- `client-groups`: "Group page" is split.
- `client-notifications`: "Showing and opening notifications" is split.
- `client-times`: "Participants see when things were made" is split.
- `draw-emails`: "Participants are emailed their draw" is split.
- `groups`: "Delete a group" is split.
- `server-runtime`: "Fail fast on invalid configuration" is split.

## Impact

- Spec text only, in the ten specs above, at archive time.
- `design.md` is skipped: a wording split has no technical choices to explain.

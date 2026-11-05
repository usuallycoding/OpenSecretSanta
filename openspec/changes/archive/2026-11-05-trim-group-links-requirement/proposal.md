# Proposal

## Why

The requirement "Group links open in the Android app" in `client-app-links` is 506 characters long and states two behaviours: which addresses open in the app, and how the app opens them. `openspec validate --strict` flags requirement text over 500 characters, so the spec validates only with a warning. Splitting it so that each requirement states one behaviour clears the warning without changing what the app does.

## What Changes

- "Group links open in the Android app" keeps the behaviour of its header (a group address of the web app opens in the installed app once verified) and all of its scenarios; the sentence about how the screen is opened moves out.
- A new requirement, "A link opens its screen as a notification does", holds that sentence: the group list is behind the screen, and sign-in comes first when the person is signed out.
- No change to behaviour, code or any other spec.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app-links`: "Group links open in the Android app" is cut to one behaviour, and the behaviour taken out becomes the new requirement "A link opens its screen as a notification does".

## Impact

- Spec text only: `openspec/specs/client-app-links/spec.md` at archive time.
- `design.md` is skipped: a wording split of one requirement has no technical choices to explain.

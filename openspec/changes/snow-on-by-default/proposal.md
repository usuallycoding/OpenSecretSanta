# Proposal

## Why

The snow is part of the app's character, and few people find the switch to turn it on. It should be falling the first time the app is opened.

## What Changes

- "Make it snow" is on by default. Anyone who has never chosen sees snow from the first screen; anyone who switched it off, or on, keeps their choice.
- A stored value that cannot be read, and a device whose storage cannot be read, now count as the default, which is snow on.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-appearance`: "Make it snow" is on by default, and "The choice is remembered on the device" says a value that cannot be read counts as on and that an off choice is kept.

## Impact

- `app/shared/src/ui/theme/Appearance.kt` (the default) and `app/shared/src/data/settings/AppearanceSettings.kt` (reading a missing or unreadable value), and `AppearanceSettingsTest`.
- No server, contract or domain change. People who already switched it on or off keep what the device stored; people who never chose get snow after the update.

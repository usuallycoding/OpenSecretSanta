# Proposal

## Why

The Appearance sheet lets people choose light, dark and (on Android) the wallpaper's colours. For a Secret Santa app a seasonal touch fits, and it costs nothing for people who do not want it: an optional "Make it snow" switch that fills the app with falling snow.

## What Changes

- The Appearance sheet gets a "Make it snow" switch on every platform, off by default. It applies at once.
- While it is on, snow falls over the whole app, in front of the content, on every screen including sign-in. The flakes are small, soft and fairly faint, of different sizes, speeds and sway so that the snow looks deep, and they start again at the top when they reach the bottom.
- The snow never gets in the way: taps, scrolls and the keyboard work as if it were not there, and a screen reader does not see it.
- The snow is visible against both themes: white on the dark theme and a pale blue-grey on the light one.
- Bottom sheets, dialogs and pickers that the platform shows in their own window appear over the snow.
- The snow stops with the app when it is not shown and uses no more flakes on a big window than its size calls for.
- The choice is kept on the device, like the rest of the appearance: across restarts, signing out and signing in as someone else, and it is read before the first screen is drawn.
- The system's "reduce motion" setting is not consulted: switching the snow on is the person's own choice.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-appearance`: three new requirements (the switch, how the snow looks, and that it stays out of the way) and "The choice is remembered on the device" now includes the switch.

## Impact

- `app/shared/src/ui/theme/Appearance.kt` and `data/settings/AppearanceSettings.kt`: a `snow` field, its setter and its stored key.
- `app/shared/src/ui/shell/AppearanceSheet.kt` and `Strings.kt`: the switch and its text.
- A new snow overlay in `app/shared/src/ui` (a canvas and a pure step function) and `App.kt`, which draws it above the content.
- Tests: the stored choice (`AppearanceSettingsTest`) and the movement of the flakes.
- No server, contract or platform-specific code: the same drawing runs on Android, iOS and the web.

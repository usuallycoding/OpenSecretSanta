# Proposal

## Why

Appearance, notifications, changing the password and signing out are hidden behind a `⋮` menu in the top bar, where people look last and where four unrelated things share one small popup. A Settings item in the bottom bar, with its own page, puts them where the app's other main places are and gives them room.

## What Changes

- Both bottom bars get a third item, **Settings**, with a settings icon: Groups, Profile and Settings for a participant; Groups, Users and Settings for an administrator. It opens one Settings page, shared by both views.
- The Settings page lists **Appearance** (showing the current choice), **Notifications** (not on iOS, as before), **Change password**, and, set apart at the bottom, **Sign out**. Each row does what the menu item did: the first three open the same bottom sheets, the last signs out.
- The top-bar `⋮` menu is **removed**. For an administrator the top bar keeps one control, an icon that switches between the admin and the participant view (announced as "Switch to participant view" or "Switch to admin view"); anyone else's top bar has no actions.
- Opening Settings does not change the view: an administrator who opens it from the admin view stays in the admin view, and a participant stays in the participant view.
- On the web the page has the address `#/settings`.

## Capabilities

### New Capabilities
- `client-settings`: the Settings tab in both bottom bars, the Settings page and its rows, that opening it keeps the view, and its web address.

### Modified Capabilities
- `client-app`: the participant and admin views' bars and top bar ("Views by role"), the web addresses (adds `#/settings`), and where "Change password" is chosen.
- `client-appearance`: the Appearance choice is reached from Settings, not the top-bar menu.
- `client-notifications`: the Notifications entry is in Settings, not the top-bar menu (its requirement is renamed to match).

## Impact

- **Shared client code**: a new `SettingsRoute` and its web address; a new Settings screen; both tab bars; the top bar and its menu in `ShellScreen`; the shell's view-following logic, which must treat Settings as belonging to neither view; the sheets' open/closed state and back handling move from the shell into the Settings screen; a small drawn "switch view" icon (the core icon set has none); strings for the new labels.
- **No server, API or data change.**
- **Tests**: the address tests gain `#/settings`. Everything else is looks and navigation, checked by running the app on Android and the web.
- **Not affected**: what the three sheets contain and how they behave, sign-out itself, the groups, profile and users screens, and notifications' delivery.

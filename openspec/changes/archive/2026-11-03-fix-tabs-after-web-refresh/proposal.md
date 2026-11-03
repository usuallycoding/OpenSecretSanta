# Proposal

## Why

In the web app, after refreshing on the Profile, Settings or (for an administrator) Users page, the Groups tab can no longer be opened: choosing it does nothing, in the participant view and in the admin view alike. The address bar does not change, no request is made and the console shows no error, so the tap is being swallowed somewhere between the tab bar and the navigation, and reading the code has not shown where. The cause was found with temporary logging and is fixed.

## What Changes

- Temporary logging found the cause, which is written into `design.md`: after a refresh Navigation had nothing filed under the view's first screen, so the first tap on its tab restored the tab that had just been left. The logging has been removed.
- A screen that is a tab (Groups, Profile, Settings, and the admin Groups and Users) named by an address, whether the page was refreshed on it or it was typed, is opened the way the tab bar opens it, with the first screen of the view the app started in as the root. Every other address is opened as before.
- After a refresh on any tab screen, every tab of the view works as it does without a refresh, including Groups, and going back from a tab still returns to Groups first.
- Nothing changes for Android and iOS, for the addresses themselves, or for what a notification opens.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: "Opening a web address" gains the guarantee that tabs keep working after a refresh, with scenarios for each tab screen and for the admin view.

## Impact

- `app/shared/src/ui/shell/ShellScreen.kt`: `openTab` is public.
- `app/shared/src/ui/navigation/TabScreens.kt` (new) and `app/webApp/src/main.kt`: which screens are tabs, the root of the view the app started in, and the web host opening a tab address through `openTab`.
- No server, contract or domain change. The behaviour is Navigation and the browser's history working together, so it was checked by hand in a browser.

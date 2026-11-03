# Proposal

## Why

In the web app, after refreshing on the Profile, Settings or (for an administrator) Users page, the Groups tab can no longer be opened: choosing it does nothing, in the participant view and in the admin view alike. The address bar does not change, no request is made and the console shows no error, so the tap is being swallowed somewhere between the tab bar and the navigation, and reading the code has not shown where. This change finds the cause first, with temporary logging, and then fixes it.

## What Changes

- Temporary logging records what a tap on a tab does after a refresh: whether the tap arrives, which screens are on the back stack before and after, and which navigation controller is used by the tap and by the address that was opened. It is removed again before the change is done.
- The cause that the log shows is written into the design and fixed. Two changes that remove the known differences between a refreshed tab and a chosen one are made whatever the log shows:
  - a screen that is a tab is opened from an address the way the tab bar opens it, so the back stack and what each tab remembers are the same;
  - the address is opened only once the first screen has been shown, not while the app is still starting.
- After a refresh on any tab screen, every tab of the view works as it does without a refresh, including Groups, and going back from a tab still returns to Groups first.
- Nothing changes for Android and iOS, for the addresses themselves, or for what a notification opens.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-app`: "Opening a web address" gains the guarantee that tabs keep working after a refresh, with scenarios for each tab screen and for the admin view.

## Impact

- `app/shared/src/ui/shell/ShellScreen.kt` and `app/webApp/src/main.kt`: the tab function is shared with the web host, the address is opened through it and after the first screen, and temporary logging is added and removed.
- Whatever the log shows to be wrong, which is not yet known and is recorded in `design.md`.
- No server, contract or domain change. The behaviour is Navigation, the browser history and the first frame working together, so it is checked by hand in a browser.

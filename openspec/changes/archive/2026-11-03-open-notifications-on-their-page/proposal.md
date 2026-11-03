# Proposal

## Why

Every notification already carries the group it is about and its category, but tapping one always opens the group's page. A person told that their Santa asked a question still has to find the Receiver page and then its Questions tab, and the same for a changed wishlist or an answer. The category is already in the message, so the app can take them straight there.

## What Changes

- Tapping a notification opens the page and tab it is about, with the group page and the group list behind it so Back goes up one level at a time:
  - a question from your Santa: Groups, the group, the Receiver page, its **Respond to Secret Santa** tab;
  - an answer from your receiver: Groups, the group, the Gifter page, its **Questions for <receiver>** tab;
  - a change to your receiver's wishlist: Groups, the group, the Gifter page, its **<receiver>'s Wishlist** tab;
  - an announcement (a message from the organiser, being added to a group, names drawn): Groups, the group.
- The Receiver and Gifter pages can open on a given tab. In the web app this is part of the address: `#/groups/2/receiver/questions` and `#/groups/2/gifter/questions`. The wishlist tab stays the default and keeps `#/groups/2/receiver` and `#/groups/2/gifter`.
- The tab in the address is only where the page opens. Choosing or swiping to another tab does not rewrite the address.
- A notification of an unknown category, or one with no category, opens the group as before.
- When the group is no longer available the person sees the usual "no longer available" message with the way back, from whichever screen the tap opened.
- No server change: the push messages already carry `category` and `groupId`, and the privacy rules for what a notification may say are unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-notifications`: "Showing and opening notifications" says which page and tab each kind of notification opens, and what is behind it.
- `client-app`: "Readable web addresses" gains the two addresses that name a tab.
- `client-group-pages`: a new requirement that the Receiver and Gifter pages can open on the tab they are asked to.

## Impact

- `app/shared/src/ui/navigation/Routes.kt` and `WebAddresses.kt`: a tab on `GifterRoute` and `ReceiverRoute`, and its addresses; `app/shared/test/ui/WebAddressesTest.kt`.
- `app/shared/src/ui/groups/GroupPages.kt` and `shell/ShellScreen.kt`: the pager starts on the requested tab.
- A small pure mapping from a notification's category and group to the screens to open, in `app/shared` with tests.
- `app/androidApp/src/NotificationsAndroid.kt` and `MainActivity.kt`: the tap carries the category and the activity builds the back stack.
- `app/webApp/resources/firebase-messaging-sw.js`: the click picks the address from the category.
- iOS has no notifications and is unaffected.

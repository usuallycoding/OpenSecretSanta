# Proposal

## Why

On iPhones running iOS 27, tapping a text field in the web app highlights it as focused but the keyboard never opens, and tapping it a second time does not help, so nobody on iOS 27 can sign in on the web. The app uses Compose Multiplatform 1.12.1. Its 1.12.2 patch release (15 October, the latest stable one) lists "Adapt backing text input focus behaviour for iOS Safari 27.0" as its web fix.

## What Changes

- Build with Compose Multiplatform 1.12.2 instead of 1.12.1, in every module that uses Compose.
- Keep the back-handler library pinned in `libs.versions.toml` at the same release, as its comment says it must follow the version Amper builds with.
- No behaviour of the app is specified differently: this is a dependency update, so there is no spec delta.

## Capabilities

### New Capabilities

### Modified Capabilities

## Impact

- `app/shared/module.yaml`, `app/webApp/module.yaml`, `app/androidApp/module.yaml` and `app/iosApp/module.yaml`: the Compose version setting.
- `libs.versions.toml`: the `compose-ui-backhandler` version.
- Every platform is rebuilt on the new release, so Android, iOS and the web are all re-checked, not only the iPhone.
- No server, API or data change.

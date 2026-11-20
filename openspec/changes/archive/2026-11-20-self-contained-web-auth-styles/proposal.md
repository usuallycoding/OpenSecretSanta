# Proposal

## Why

On the web, the native HTML sign-in and password-change forms currently depend on `.oss-auth-*` rules in external `app/webApp/resources/styles.css`. If a CDN or browser serves a stale `styles.css` alongside a fresh `webApp.wasm`, `.oss-auth-overlay` loses `position: fixed; inset: 0; z-index: 10`, pushing the login form below the `overflow: hidden` canvas and leaving users with a blank background.

## What Changes

- Inject the `.oss-auth-*` stylesheet once into `document.head` directly from `app/shared/src@wasmJs/AuthFormHosts.wasmJs.kt` (idempotent by element id) whenever a web auth form mounts.
- Keep `app/webApp/resources/styles.css` strictly for the pre-load `html, body` sizing and light/dark background so the auth forms are completely self-contained inside `webApp.wasm`.
- Add a WasmJS unit test verifying that mounting a web auth form injects the stylesheet into `document.head` once.

## Capabilities

### New Capabilities
<!-- None — pure implementation hardening; no spec-level behavior changes (`skip_specs: true`). -->

### Modified Capabilities
<!-- None -->

## Impact

- `app/shared/src@wasmJs/AuthFormHosts.wasmJs.kt`: Embeds and injects `<style id="oss-auth-styles">` into `document.head`.
- `app/webApp/resources/styles.css`: Restored to minimal `html, body` pre-load background rules.
- `app/shared/test@wasmJs/WebAuthFormsTest.kt`: Verifies stylesheet injection and idempotency.

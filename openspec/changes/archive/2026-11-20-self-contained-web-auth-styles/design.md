# Design

## Context

See `proposal.md` for motivation. Because `webApp.wasm` and `styles.css` are fetched as separate HTTP resources, any stale CDN/browser copy of `styles.css` breaks the fixed-overlay positioning of the HTML `<form>` elements mounted by `AuthFormHosts.wasmJs.kt`.

## Goals / Non-Goals

**Goals:**
- Guarantee that the web sign-in and password-change HTML forms always have their exact layout and theming styles regardless of external CSS caching.
- Avoid duplicate `<style>` tags when mounting/unmounting forms repeatedly.

**Non-Goals:**
- Changing the visual appearance or behavior of the auth forms.
- Moving the pre-load `html, body` background out of `styles.css` (that must still load before Wasm starts to prevent a white/dark flash).

## Decisions

### 1. Idempotent `<style id="oss-auth-styles">` injection in `AuthFormHosts.wasmJs.kt`
Add `private fun ensureAuthStyles()` called by both `mountWebLoginForm` and `mountWebChangePasswordForm`:
- Checks `document.getElementById("oss-auth-styles") != null`; if absent, creates a `<style id="oss-auth-styles">` element with the `.oss-auth-*` CSS and appends it to `document.head`.
- Leaves the `<style>` element in `document.head` across screen transitions (tiny ~4 KB static stylesheet, zero flicker).

**Why**: Zero external file sync dependency, works identically in production, local dev, and WasmJS unit tests.

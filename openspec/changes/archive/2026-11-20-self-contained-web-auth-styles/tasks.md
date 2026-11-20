# Tasks

## 1. Self-Contained Web Auth Styles

- [x] 1.1 Add idempotent `ensureAuthStyles()` injecting `<style id="oss-auth-styles">` into `document.head` in `app/shared/src@wasmJs/AuthFormHosts.wasmJs.kt` (called from `mountWebLoginForm` and `mountWebChangePasswordForm`), and remove the `.oss-auth-*` rules from `app/webApp/resources/styles.css` so it only holds the pre-load `html, body` styles.
- [x] 1.2 Add a WasmJS unit test in `app/shared/test@wasmJs/WebAuthFormsTest.kt` verifying `<style id="oss-auth-styles">` is injected once into `document.head`, and run `./kotlin test -m shared -p wasmJs` to verify all tests pass.

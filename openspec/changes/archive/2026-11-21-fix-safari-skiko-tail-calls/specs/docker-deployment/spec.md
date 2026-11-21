# Spec Delta

## ADDED Requirements

### Requirement: Built web container avoids WebKit tail-call corruption in Skiko exports
The web container build SHALL post-process `skiko.mjs` so its lazy WebAssembly export wrappers do not invoke `loadedWasm._[...]` in tail position, preventing Safari and iOS WebKit browsers from corrupting arguments and rendering a blank white canvas. The build SHALL fail if `skiko.mjs` is missing or if no expected wrappers are found when the file has not already been patched.

#### Scenario: Safari and iOS browsers render the Compose canvas
- **WHEN** the web container image is built and opened in macOS Safari or an iOS browser
- **THEN** `skiko.mjs` serves non-tail-call export wrappers (wrapped in a `try { return ... } catch (e) { throw e }` block) and the Compose canvas renders the themed background, snow overlay, and post-login screens instead of a white screen

#### Scenario: Unexpected Skiko bundle format fails the build loudly
- **WHEN** a future Skiko upgrade changes the export wrapper format so neither unpatched tail-call wrappers nor already-patched wrappers match
- **THEN** the Docker image build exits non-zero with a clear diagnostic message instead of silently publishing an unpatched bundle

# Design

## Context

The link filter in `AndroidManifest.xml` and the constant `WEB_APP_HOST` in `AppLinks.kt` need the web app's host, which is the owner's own address and must not be committed. The release APK is built by `scripts/distribute-android.sh`, which already edits `module.yaml` for the version and signing and puts it back, and which reads the keystore and its passwords from the git-ignored `secrets/`. Debug builds come from Android Studio, without the script. This change records what commit `08ce0af` built; see proposal.md.

## Goals / Non-Goals

**Goals:**
- The real host is in no commit, file or message of the repository.
- A release built by the script always carries the real host, and the script says so when it cannot.
- Builds from Android Studio keep working without any extra setup.

**Non-Goals:**
- Verifying the host on a debug build, or making debug builds open the links.
- Reading the host in any build other than the release script's.
- Generalising to several hosts.

## Decisions

**A placeholder in the checkout and a substitution in the script.** Both files name `santa.example.com`, and the script writes the real host into copies for the build and restores them, the way it already treats `module.yaml`. Builds from Android Studio compile with no extra file and simply do not open the links. Alternatives rejected: a git-ignored resource or Kotlin file holding the host, which every build, including Android Studio's, would then need to have or fail to compile; local uncommitted edits to the two files, which are easy to forget and to commit; `skip-worktree` on those files, which hides the edit and makes pulls awkward.

**The host lives in `secrets/web-app-host`, one line.** A separate file next to the keystore and the signing properties, so the existing "files git ignores, on this machine" rule and `SECRETS_DIR` cover it, and the README lists it with the other files. It is not a secret in the cryptographic sense, but the owner does not want it committed. Alternative rejected: a key in `android-signing.properties`, which mixes an address in with passwords.

**Checks before the build, so nothing is half-done.** The file must exist, hold only letters, digits, dots and hyphens (so it is safe in a `sed` replacement and cannot be a URL), and not be the placeholder; the manifest and `AppLinks.kt` must have no uncommitted changes and name the placeholder on exactly one line, so the substitution has exactly one target in each. These run before anything is changed, like the existing checks.

**A check of the built APK.** The script reads the APK's manifest with `aapt2 dump xmltree` and requires the link filter to name the real host and not the placeholder, so a wrong substitution cannot produce a distributable APK. Alternative rejected: trusting the `sed` alone, which would not notice a build that ignored the edit.

## Risks / Trade-offs

- [A build made without the script silently has the placeholder and never opens links] → Documented in the README and the spec; only releases are meant to open the links.
- [The script being killed without a chance to clean up leaves the two files edited] → The same exposure `module.yaml` already has; the pre-checks refuse to start on uncommitted changes, so the leftover is noticed at the next run.
- [The real host is no longer in any spec, so a reader cannot see which address the release opens] → Specs and README say "the web app the release was built for"; the value is in `secrets/web-app-host`.

## Migration Plan

Already in place: create `secrets/web-app-host` on the build machine, then distribute as before. Rollback is reverting `08ce0af`, which restores the need to edit the two files by hand.

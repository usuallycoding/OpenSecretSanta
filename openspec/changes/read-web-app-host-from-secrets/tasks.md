# Tasks

Everything here was built in commit `08ce0af`; this change only records it, so each task is done and says how it was checked.

## 1. The release script

- [x] 1.1 Read `secrets/web-app-host` in `scripts/distribute-android.sh`, check it before anything is changed (exists, a plain host name, not the placeholder; the manifest and `AppLinks.kt` clean and naming the placeholder on exactly one line); verified by running the script with the file missing, holding `https://santa.example.org/`, and holding `santa.example.com`, each stopping with its own message
- [x] 1.2 Write the host into copies of the manifest and `AppLinks.kt` for the build and restore both afterwards; verified by a real `--no-upload` release build after which both files named the placeholder again and `git status` was unchanged
- [x] 1.3 Check the built APK's link filter with `aapt2 dump xmltree` (the real host, not the placeholder); verified by the same build, whose manifest showed `autoVerify`, scheme `https` and the real host

## 2. Docs and checks

- [x] 2.1 Describe `secrets/web-app-host` in the README's distribution setup and say that other builds keep the placeholder; verified by reading the README sections "Distribute the Android app to testers" and "Opening links in the Android app"
- [x] 2.2 Run `openspec validate read-web-app-host-from-secrets --strict`; verified when it passes

## Workflow follow-up

- Archive the change after review so `android-app-distribution` and `client-app-links` pick up the new requirements.

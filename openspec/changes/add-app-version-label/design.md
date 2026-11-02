# Design

## Context

The sign-in and Settings screens are Compose files in `app/shared/src/ui`, rendered by Android, iOS and the web, and their user-facing text lives in the `Strings` object. There is no version in the code today: Android's `versionName` is injected at build time by `scripts/distribute-android.sh`, which defaults it to `git describe --tags --always --dirty` and writes it into `module.yaml` for the build only. See proposal.md for why.

## Goals / Non-Goals

**Goals:**
- One hand-edited place for the version, read by the screens and by the script.
- The same string on every platform and in the installed APK.

**Non-Goals:**
- Generating the version from git, CI or the build tool.
- Showing a build number, commit or environment next to the version.
- Reading the version on iOS or the web from platform metadata.

## Decisions

**A Kotlin `const val` in `app/shared/src/AppVersion.kt`.** It sits beside `Platform.kt`, in `commonMain`, so every target compiles the same value with no `expect/actual`. Alternative: Android's `versionName` through a platform lookup, rejected because iOS and the web have no equivalent without a code generator.

**The script reads the constant from the source file with `sed`.** It matches the single line `const val APP_VERSION = "<value>"` and takes the quoted value. The script already edits files with `awk`, and a plain text read avoids running a build just to learn the version. If the line is missing, matches more than once, or the value is empty, the script dies before building. The existing character check (letters, digits, `. _ + -`) stays and applies to the constant too. An explicit `--version` skips the read.

**The label text is built in `Strings`.** A `Strings.version(value)` function returns "Version <value>", like the other formatted strings, so the screens do not assemble it.

**Placement.** Login: a last item of its `Column` below the button, using `bodySmall` and `onSurfaceVariant`. Settings: after the "Sign out" row inside the same scrolling `Column`, with padding, so it sits at the end of the page and scrolls with it. The label has no click handler and is not selectable.

**Clean version only.** `versionName`, and so the APK name, are the constant alone; the commit and `-dirty` marker that `git describe` added are dropped, as decided in exploration. `versionCode` stays the minute timestamp, so two builds of one version stay distinct to Android.

## Risks / Trade-offs

- [Forgetting to bump the constant] → Two different builds then share a version name. `versionCode` still differs, and `--version` can override it for one run.
- [A built APK no longer says which commit it came from] → Accepted. The distribute notes default to the last commit's subject.
- [Renaming or reformatting the constant breaks the script's read] → The script fails loudly before building, and a task keeps the line format documented in the README.

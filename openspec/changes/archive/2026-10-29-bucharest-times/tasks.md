# Tasks

## 1. The conversion

- [x] 1.1 Make `formatInstant` convert to Bucharest time (no suffix), trying `TimeZone.of("Europe/Bucharest")` in the wasm tests first and using the dependency-free EU rule on every platform because the zone was not available in one wasm runner, and cover winter, summer, both clock changes at the minute, the next-day roll-over, seconds and milliseconds, and unreadable input; update the existing test that expected "UTC"

## 2. The models

- [x] 2.1 Add the formatted times to `WishItem`, `SharedWishItem` and `Question` (changed and answer-changed only when they differ), fill them in the mappers, and cover each mapping (never edited, edited, unanswered, answered, answer edited) in the view model tests

## 3. The screens

- [x] 3.1 Add the shared `TimesLine`, show it on the person's own items, the receiver's items and the questions of both pages, move the owner's two sheets to it and replace the `admin`-prefixed strings; verify in headless Chrome with an edited item, an unedited one, an unanswered question, an answered one and an edited answer, as a participant and as the owner, checking the times against the database's UTC values

## 4. Check and document

- [x] 4.1 Update the README (the times in Bucharest time and where participants see them), run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (phones, a device set to another time zone)

## Workflow follow-up

- Archive the change after review and sync the delta spec into `openspec/specs`.

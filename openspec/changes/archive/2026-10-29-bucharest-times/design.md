# Design

## Context

The server sends every time as an ISO-8601 UTC string (`createdAt`, `updatedAt`, `askedAt`, `answeredAt`, `answerUpdatedAt`) in the DTOs of both the owner's and the participants' endpoints. `formatInstant(raw)` in `ui/groups/GroupFormat.kt` matches the string with a regular expression and prints it unchanged with a "UTC" suffix; only `OversightModels.kt` uses it (the owner's wishlist and thread sheets, which print "Added/Changed/Asked/Answered/Answer changed" as separate lines). The participant models drop the times: `WishItem(id, title, note, url)`, `SharedWishItem(title, note, url)` and `Question(number, text, answer)`. The app depends on `kotlinx-datetime` 0.8.0 through the domain module.

## Goals / Non-Goals

**Goals:**
- One conversion and one format for every time the app shows, in Bucharest time.
- Participants see the times they already receive, with little extra height.

**Non-Goals:**
- A choice of time zone, the device's zone, relative times ("2 hours ago"), changing what the server sends, showing times for anything but items and questions.

## Decisions

1. **One function, one zone.** `formatInstant(raw)` stays the single entry point and changes to convert to Bucharest time and drop the suffix; the rule lives next to it. The output is `d MMM yyyy, HH:mm` (`1 Dec 2026, 12:00`), month names from the existing `MONTHS` list; a string that is not an instant is returned as it came.
2. **How the conversion is done.** The EU rule, on every platform, with no dependency: Bucharest is UTC+2, and UTC+3 from the last Sunday of March 01:00 UTC until the last Sunday of October 01:00 UTC; the instant is parsed, shifted by that offset and read as a UTC date and time. The first choice, `TimeZone.of("Europe/Bucharest")` from `kotlinx-datetime`, was tried: it worked on the JVM but was unavailable in one of the two web test runners (the time-zone database is not always there on wasm), and the app must show one time everywhere. The tests pin the result: winter, summer, both clock changes at the minute, the next-day roll-over, millisecond and second parts, unreadable input; they pass in every runner.
3. **Models carry formatted text.** As the owner's models already do, the participant models get the finished strings: `WishItem` and `SharedWishItem` gain `added: String` and `changed: String?` (set only when `updatedAt` differs from `createdAt`), and `Question` gains `asked: String`, `answered: String?` and `answerChanged: String?` (set only when `answerUpdatedAt` differs from `answeredAt`). The mappers (`toItem`, `toQuestion`) do the formatting, so screens never see an instant, and the view models' own optimistic updates use the server's answer (which carries the times), not a locally made one.
4. **One small line.** A shared composable `TimesLine(vararg parts)` shows the non-null parts joined with " · " in `labelMedium` and the muted colour, wrapping when it must: "Added 29 Oct 2026, 14:05 · Changed 30 Oct 2026, 09:12". It sits under the item text (own list, Gifter page) and under the question and under the answer (both question pages); the owner's sheets switch to it from their separate lines. The labels are the existing words ("Added", "Changed", "Asked", "Answered", "Answer changed"), now generic `Strings` functions; the `admin`-prefixed ones go.
5. **No behaviour change.** The line is plain text outside `ReadOnlyText`, so tapping it does nothing, and the item card's controls (edit, move) stay where they are. The cards grow by one small line.
6. **Server and contract unchanged.** The DTO comments that say "ISO-8601 UTC" stay true.

## Risks / Trade-offs

- A fixed zone is wrong for someone who travels; everyone in the group shares it, which is the point.
- Bucharest's daylight-saving rule is hard-coded (it is the EU rule); a change to it would need an update.
- The cards of items and questions get one line taller; accepted.

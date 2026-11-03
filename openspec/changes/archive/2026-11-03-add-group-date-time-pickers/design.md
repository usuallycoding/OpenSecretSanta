# Design

## Context

`AdminGroupFormViewModel` keeps `date` and `time` as strings, validates them with `GroupDetails.of` (the same code the server uses) and sends them as ISO strings. The screen shows them with a text `Field`. The app already uses Compose Multiplatform Material 3 and `kotlinx-datetime`, which provide `DatePicker` and `TimePicker` on Android, iOS and wasm. See proposal.md for motivation and the delta spec for the requirements.

## Goals / Non-Goals

**Goals:**
- Replace typing with pickers without touching what the view model, the API or the server see.
- Keep the conversions and defaults in pure functions that unit tests can reach.

**Non-Goals:**
- No change to how dates and times are displayed elsewhere, to the server's validation or to the stored format.
- No limit on which dates can be picked (past dates stay allowed).

## Decisions

- **The picker writes the same strings.** The form state, `edit(GroupField, String)`, validation and the request stay as they are; a confirmed picker calls `edit` with `2026-12-24` or `18:30`, and clearing calls it with an empty string. Alternative: store `LocalDate` and `LocalTime` in the form. Rejected: it changes the view model, its tests and the error handling for no gain, and the strings already round-trip.
- **Keep the view model's checks.** A picker cannot produce an invalid value, but the same code still guards a value that did not come from one, and `client-admin-groups` keeps requiring it.
- **The field is read-only and the whole field opens the dialog**, with a trailing clear (x) while it has a value. The field keeps its label, shows the value as text, and is announced as a button that opens the picker; the clear control has its own label.
- **Dates are converted in UTC.** Material's `DatePicker` speaks UTC milliseconds at midnight. The picker's initial value is the local date at 00:00 UTC and the chosen millis are read back as a UTC date, so the person sees and saves the same calendar date in every timezone. Reading the millis in the device's zone would shift the date by a day for some people.
- **Defaults are computed, not chosen.** An empty date field opens on December 25 of the current year, or of the next once that day has passed; an empty time opens on 00:00. They are only the picker's starting point: the field changes only when the person confirms. The default is a pure function of today's date so a test can fix it.
- **24-hour time:** the time picker is created with 24-hour mode forced on, so the label of the field and the picker agree and the saved `HH:mm` is what the person saw.
- **No typed-entry mode:** the date picker's mode toggle is turned off, which keeps the field picker-only; the typed format would otherwise follow the device's locale.
- **Dialogs are plain confirm/cancel:** "OK" confirms, "Cancel" closes without changing the field. Both reuse the existing strings.

## Risks / Trade-offs

- [The Material pickers are experimental APIs and differ a little between Android, iOS and web] → Opt in where the shell already does, and check the dialogs on all three targets before archiving.
- [Without typing, an owner who knows the date cannot paste it] → Accepted by the requester; the picker is the single way in.
- [A day-off error from timezone conversion] → The UTC conversions are pure functions with tests that include a negative-offset zone's midnight.

## Migration Plan

None: no data or API change, and existing groups load their stored date and time into the same fields. Rollback is reverting the app code.

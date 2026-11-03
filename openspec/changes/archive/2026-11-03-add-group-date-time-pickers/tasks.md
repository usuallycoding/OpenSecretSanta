# Tasks

## 1. Date and time logic

- [x] 1.1 Add a small file beside `AdminGroupFormScreen.kt` with the pure functions the pickers need: the default date (25 December of this year, or of next year after it has passed) from today's date, a date to and from the picker's UTC milliseconds, and a time to and from hour and minute; add tests in `app/shared/test/ui` for the default before, on and after 25 December, and for round-trips of the dates `2026-12-24` and `2026-01-01` including the case where the device's zone is behind UTC, and verify them with `./kotlin test -m shared`

## 2. The form

- [x] 2.1 Add a read-only picker field with a trailing clear (x) while it has a value, announced as a button that opens its picker, and use it for Date and Time in `AdminGroupFormScreen.kt`, writing the chosen value or an empty string through `viewModel.edit`; verify that typing into either field is impossible and that clearing empties it
- [x] 2.2 Show the date picker (`DatePickerDialog` with the typed-entry toggle off) starting on the current value or the default, and the time picker forced to 24-hour starting on the current value or 00:00, each confirming with OK and leaving the field untouched on Cancel; verify opening and cancelling on an empty field leaves it empty
- [x] 2.3 Remove `GROUP_DATE_HINT` and `GROUP_TIME_HINT` from `Strings.kt` and add the dialog and clear-control labels; verify the app still builds and `AdminGroupFormViewModelTest` still passes unchanged
- [ ] 2.4 Check the form on Android, iOS and the web app: pick, clear, cancel, edit an existing group's date and time, and a device in a timezone behind UTC; verify each shows and saves the same date and time

## 3. Specs and integration

- [x] 3.1 Run `openspec validate add-group-date-time-pickers --strict` and verify it passes, then run `./kotlin test -m shared` and `./kotlin test -p jvm` and verify they are green

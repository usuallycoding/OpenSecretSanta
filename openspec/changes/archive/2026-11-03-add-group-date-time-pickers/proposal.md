# Proposal

## Why

The group form asks the owner to type the date as `2026-12-24` and the time as `18:30`. It is easy to get wrong, needs a format hint, and fails with an error the person then has to read. Both fields are optional and the form only needs a calendar date and a time of day, which a picker gives without any typing.

## What Changes

- The Date and Time fields of the group form become picker-only: the field is read-only and choosing it opens a date picker or a time picker. The text stays in the same forms as before (`2026-12-24`, `18:30`).
- A field that has a value shows a clear (x) control that empties it, since both are optional. An empty field is saved as no date or no time, as before.
- When a field is empty the picker opens on Christmas Day for the date (this year's, or next year's once this year's has passed) and on 00:00 for the time. Nothing is chosen until the person confirms, so opening and cancelling leaves the field empty.
- When a field has a value, its picker opens on that value.
- The time picker is always 24-hour, whatever the device uses. The date picker has no typed-entry mode.
- The date and time stay local values with no timezone: the date the person picks is the date that is saved, in any timezone.
- The format hints under the fields go away.
- What is sent to the server, the server's checks and the way dates and times are shown elsewhere do not change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `client-admin-groups`: the group form gains the date and time pickers and clearing, and no longer asks for typed dates and times.

## Impact

- `app/shared/src/ui/admin/AdminGroupFormScreen.kt`: the two fields become picker fields with a clear control.
- A new small file beside it for the picker dialogs and the pure date and time conversions, with tests in `app/shared/test`.
- `app/shared/src/ui/Strings.kt`: the two hint strings are removed; labels for the dialogs and the clear control are added.
- `AdminGroupFormViewModel` keeps its string fields and its checks; the picker writes the same strings.
- No server, contract or domain change. Android, iOS and the web app all use the same Compose Multiplatform pickers.

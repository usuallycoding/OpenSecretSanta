# Design

## Context

The receiver's details travel through four layers: `GetMyReceiver` builds `ReceiverDetails` from the receiver's profile, `GiftRoutes.kt` maps it to `ReceiverDto`, the app's `ReceiverDto.toInfo()` maps that to `ReceiverInfo`, and `PairSheets.kt` renders it. `Profile.email` is already stored, validated and optional, so the data exists and nothing in storage changes. See proposal.md for motivation; the requirement changes are in the three delta specs.

## Goals / Non-Goals

**Goals:**
- Carry `email` through the existing receiver path with the same shape as `phone` (nullable, cleaned, shown only when set).
- Keep the contract backward compatible for clients that do not know the field yet.

**Non-Goals:**
- No consent toggle, separate "delivery email" field or database migration.
- No change to the Santa reveal, the draw or announcement emails, the admin endpoints, or what the receiver can learn about their Santa.

## Decisions

- **Add `email` to the existing receiver response instead of a new endpoint.** The receiver sheet already loads `GET /groups/{id}/receiver` once, with the same authorization (participant, Active or Archived) that the email needs. A new endpoint would duplicate that and add a second request. Like the other fields it is read live, so edits show straight away.
- **`ReceiverDto.email` is `String? = null`.** Matches the sibling fields, so old clients ignore it and old servers deserialize fine.
- **Email counts in `hasPostalDetails`.** The empty-state message exists to say the Santa has nothing to deliver with; an email alone is something the Santa can use, so showing the "not added yet" message beside it would be wrong.
- **Display order: name, email, phone, address, delivery information** in the sheet, label "Email", rendered with the existing `DetailField`. The order is cosmetic; the spec only requires it to be shown when set.
- **Profile note reworded, not given a new element.** `PROFILE_WHY` is the one place that says what the email is for, so it is the disclosure.

Alternative considered: an opt-in "share my email with my Santa" setting. Rejected by the requester: phone and address are already shared on the same basis, and a toggle adds a field, storage and UI for little gain.

## Risks / Trade-offs

- [Existing users' emails become visible to their Santa without a new consent step] → The Profile note now says so; users can clear the field at any time, and the response is live so clearing takes effect at once.
- [A Santa keeps the email after the group ends] → Accepted; the same is true of the address and phone, which are readable after archive too.
- [The email leaks more than intended through another path] → The change touches only the receiver response; password data and the admin flag stay out of `ReceiverDetails`, and the participant-only and 404/409 rules are unchanged.

## Migration Plan

No data migration. Deploy server and apps in any order: a new server with an old app simply sends a field the app ignores, and a new app with an old server shows no email. Rollback is reverting the code.

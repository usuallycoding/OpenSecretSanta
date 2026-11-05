# Design

## Context

A group's editable details travel as `GroupDetails` (domain), `GroupDetailsRequest` / `CreateGroupRequest` / `AdminGroupDto` / `MemberGroupDto` (contract) and the `groups` table, with the admin form (`GroupForm`) and the shared `GroupInfoCard` on the client. `location` follows that whole path, so the budget does too. See proposal.md for the scope.

## Goals / Non-Goals

**Goals:**
- Two optional fields, `budget` and `currency`, carried on every path `location` is, with one place that decides the `RON` default.
- Older clients and existing rows keep working.

**Non-Goals:**
- No currency list, formatting of thousands, or conversion. The currency is plain text.
- No budget on the group list rows and none in the search.

## Decisions

- **Integer in the contract.** `budget` is `Int?` in the JSON, unlike `date` and `time`, which are strings the server parses. A number needs no parsing there, and a string would let `"1e3"` or `"  100"` in. `GroupDetails.of` takes `Int?` and checks 50..10000; the form, which holds text, parses digits itself and shows the same message the server gives.
- **The default is applied when saving, not when showing.** `GroupDetails.of` turns a blank currency into `RON` when there is a budget, and drops the currency when there is none. The table then never holds a budget without a currency, and neither the cards nor the emails need a fallback. Alternative: store null and fall back at every place that shows it, which repeats the constant in the app and the emails.
- **Two nullable columns, no CHECK.** A new migration adds `budget INTEGER` and `currency TEXT` to `groups`; existing rows stay NULL. The range and length are checked in the domain like the other fields, which keeps one source for the limits and one error shape (`ValidationFailed` with the field name, which the route answers as 400).
- **Duplicate copies both fields** through the request it already builds. No new endpoint.
- **The card builds the line as the others.** `GroupInfoCard` takes the budget text (`"100 RON"`) already joined, as it takes `whenText`, so the model owns the formatting and the card only labels it.
- **Clearing the budget clears the currency.** In the form, an empty budget is sent as no budget and no currency, so a leftover currency is never saved.

## Risks / Trade-offs

- [A client sends a currency without a budget] → the server drops it, so the data stays consistent.
- [The form's `RON` prefill is kept when editing a group with another currency] → the form starts from the group's own currency when it has a budget, and from `RON` only when it has none.
- [Free-text currency gives `eur`, `EUR` and `Euro`] → accepted; the owner types it, and the app only shows it.

## Migration Plan

Deploy the server with the new migration (it only adds columns; it runs on start). Old clients ignore the new fields; new clients against an old server get no budget and simply show no line. Rollback: the extra columns can stay unused.

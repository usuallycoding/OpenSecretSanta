# Design

## Context

`AssignmentPlanner` shuffles the participants and chains them in that order, so every draw is one closed chain by construction (see `gift-assignment`, "Single cycle"). There is nowhere to put a constraint. Draws are stored in `assignments` at activation, in the same transaction as the state change (`GroupRepository.activate`). The owner's roster can still change in Pending (`SetParticipants`), so rules must follow it. The admin group page already opens a participant's wishlist or a pair in a pane on wide windows (`GroupSheet`, `opensInPane`, `GroupPane`).

## Goals / Non-Goals

**Goals:**
- Rules the planner can honour, with the draw staying one chain and staying random.
- Saving a rule set that cannot be drawn is impossible, so activation almost never fails because of rules.
- One piece of UI content, shown as a page or as a pane.

**Non-Goals:**
- Rules in Draft, rules that name groups of people, "at least one of" rules, or rules a participant can see.
- Changing the 4-participant minimum or the stored shape of assignments.

## Decisions

**Rules are directed edges, `must` and `never`.** A draw is a Hamiltonian cycle through the participants; a `must` is an edge it has to contain, a `never` an edge it must not. One-way storage keeps the model one table (`draw_rules`: `id`, `group_id`, `giver`, `receiver`, `kind`, unique on `group_id, giver, receiver`), and "both ways" is only the add request writing two rows in one transaction. Alternative: a symmetric "pair" rule; rejected because "never" is directional in the user's examples and a pair rule cannot say "A must give to B".

**One solver, used for planning, validating and counting.** A depth-first search builds the chain: musts are contracted first into fixed paths (chain pieces), then the pieces and free participants are ordered by a search that tries the next candidates in a shuffled order and skips `never` edges and any edge that closes a circle too early. Alternatives: (a) shuffle and retry until valid, which is simple but can loop for ever when the rules are tight; (b) a general constraint library, which is a new dependency for a small problem. Groups are small, so the search is cheap, and a step limit turns a pathological case into "no draw found" rather than a hang.
- **Plan** returns the first valid cycle found with candidates shuffled by the existing `Shuffler`, so the secure source still decides. With no rules it reduces to today's behaviour (same result shape; the existing planner tests keep passing).
- **Validate** runs the search to find any cycle; no cycle is a `rules_conflict`. Cheap checks run first, to give precise messages: two musts for one giver or receiver, must and never together, a duplicate, a circle closed by musts alone.
- **Count (few draws)** runs the same search without the shuffle and stops at 10 found.
- Uniformity: a depth-first search with shuffled candidates is not exactly uniform among valid cycles. That is accepted: it is random and secure, and an exactly uniform draw would need counting cycles for large groups. This is recorded so no one assumes more.

**Validation at save, again at activation.** The save check runs against the current participants. Activation runs the planner on the participants as they are at that moment, and a failed search throws the new `RulesConflict` (code `rules_conflict`, 409) before anything is stored, so the transaction never starts. Because the roster can change, and because deleting a participant removes their rules, a conflict at activation is rare but possible, hence the error and the client's link to the rules.

**Domain shape.** `DrawRule(giver, receiver, kind)` and `RuleKind` in `core/domain`; `DrawRules` is the pure rule-set logic (validate adding a rule, find conflicts and name them); `AssignmentPlanner.plan(participants, rules = emptyList())`; use cases `ListDrawRules`, `AddDrawRules` (one or two rules), `DeleteDrawRule`, each loading the group through the existing owner loader. A `DrawRuleRepository` port keeps the repository interface of groups from growing. `SetParticipants` calls the repository to delete rules of removed participants inside the same transaction as `replaceParticipants` and returns them in `GroupChange`.

**Transactionality.** Deleting a participant's rules happens in `replaceParticipants`' transaction, so a roster change and its rule cleanup are one step. For this, `replaceParticipants` returns the deleted rules, or the rules repository shares the transaction; the implementation picks whichever matches how `SqliteGroupRepository` already shares its connection, and a test proves the all-or-nothing behaviour.

**Contract.** `GET/POST /admin/groups/{id}/rules`, `DELETE /admin/groups/{id}/rules/{ruleId}`. DTOs: `DrawRuleDto(id, giver, receiver, kind)`, `DrawRulesDto(rules, fewDraws)`, `AddDrawRuleRequest(giver, receiver, kind, bothWays)`. The participants response gains `deletedRules: List<DrawRuleDto>`, empty by default so older clients still parse it.

**Privacy.** Rules appear in no participant DTO and no participant route; a test asserts the group views a participant gets have no rules field. Rules are listed in the owner's admin endpoints only.

**Client: one content, two shells.** `RulesContent(groupId)` with a `DrawRulesViewModel` (rules, `fewDraws`, form state, busy, error, `canEdit` from the phase). On narrow windows a new `AdminGroupRulesRoute(id)` shows it as a page with a back action, and its readable address is `#/admin/groups/{id}/rules`. On wide windows the gavel sets `GroupSheet.Rules`, which `opensInPane()` includes, so `GroupPane` renders `RulesContent` beside the group page; a deep link to the route on a wide window pops to the group page with that sheet open. The page and the pane share the view model, not the shell, as the wishlist content already does.

**Gavel icon.** A small `ImageVector` in `ui/form/GavelIcon.kt`, drawn with the same helper style as `GiftIcon` and `KeyIcon`, tinted by `LocalContentColor`. `material-icons-extended` is not added: it is large for the wasm bundle.

**Notices.** Deleted rules after a roster change come back in the participants response and are shown as a notice with the page's sentences. Activation refused by `rules_conflict` shows the server message with a button that opens the rules.

## Risks / Trade-offs

- [The search is slow for a large group with tight rules] → step limit and a clear "no draw found" outcome; the realistic group is under 30 people.
- [A save-time check says satisfiable, activation says not, after a roster change] → the check runs again at activation and nothing is stored on failure; the owner is pointed to the rules.
- [Not exactly uniform] → recorded above; acceptable for a gift draw, and still unpredictable without the secure source.
- [Rules weaken secrecy for an owner who also takes part] → the page says so; the owner already sees every pair.
- [Few allowed draws make the draw guessable] → the `fewDraws` warning; no hard limit by decision.
- [Clients that do not know `deletedRules`] → the field defaults to empty, and clients and server ship together in this repo.

## Migration Plan

A new migration adds `draw_rules` (with `ON DELETE CASCADE` from the group, like the other group tables) and is listed in `server/resources/db/migration/index.txt`. Existing groups have no rules, so nothing needs backfilling. Rolling back means ignoring the table; no existing table changes.

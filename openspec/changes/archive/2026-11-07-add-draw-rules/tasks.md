# Tasks

## 1. Domain: rules and the constrained draw

- [x] 1.1 Add `DrawRule` and `RuleKind` (`must`, `never`) in `core/domain/src/group`, with the rule-set logic that checks a rule to add: not oneself, no duplicate, no second must per giver or receiver, no must with a never on the same pair, no circle closed by musts that leaves someone out. Each refusal names the rules in conflict.
- [x] 1.2 Add `RulesConflict` (`rules_conflict`, 409) to `DomainException` and map it in the server's error handling and the contract's error codes.
- [x] 1.3 Make `AssignmentPlanner.plan(participants, rules)` a depth-first search over the rules: musts contracted into fixed pieces, shuffled candidates through the `Shuffler`, `never` edges and early circles skipped, a step limit, `RulesConflict` when no chain is found; with no rules it behaves as before.
- [x] 1.4 Add the search's counting form (stops at 10) for the few-draws flag, and the validation that a whole rule set still has a chain.
- [x] 1.5 Add the `DrawRuleRepository` port and the use cases `ListDrawRules`, `AddDrawRules` (one rule or both ways), `DeleteDrawRule`, all owner-only, Pending-only for changes, `invalid_state` in Draft.
- [x] 1.6 Make `ActivateGroup` pass the rules to the planner, and `SetParticipants` delete the rules of removed participants in the same step and return them in `GroupChange`.
- [x] 1.7 Domain tests: every validation case, the planner with musts, nevers and both (many seeds, always one chain), the no-chain case, the step limit, the count, activation refused with the group untouched, rule cleanup on removal.

## 2. Server

- [x] 2.1 Add migration `V013__draw_rules.sql` (`draw_rules`: id, group_id with cascade, giver, receiver, kind, unique on group, giver, receiver) and list it in `server/resources/db/migration/index.txt`.
- [x] 2.2 Add the SQLite repository for rules, and delete a removed participant's rules inside `replaceParticipants`' transaction, so the roster change and the cleanup are all-or-nothing; wire the repository in `ServerModules`.
- [x] 2.3 Add the contract: `DrawRuleDto`, `DrawRulesDto` (rules, `fewDraws`), `AddDrawRuleRequest`, `deletedRules` on the participants response (empty by default), the three routes in `ApiRoutes`.
- [x] 2.4 Add the rule routes (list, add, delete) with owner checks, state checks and the 400, 403, 404, 409 answers of the spec; make activation answer `rules_conflict`.
- [x] 2.5 Route and repository tests: add, both ways (and the all-or-nothing of a conflicting pair), delete, each state, other admin 404, non-admin 403, conflicts, removal cleanup listed in the response, cascade on group delete, activation honouring and refused by rules, and that no participant view or endpoint carries rules.

## 3. App: data and view model

- [x] 3.1 Add the API calls and models for rules, with the sentence text for a rule ("<giver> must give to <receiver>", "... must not give to ...") and the sorting (musts first, by giver).
- [x] 3.2 Add `DrawRulesViewModel`: load, add (one way or both), delete, form state with the same-person check, busy, the server's conflict message, `canEdit` from the phase, `fewDraws`, whether the owner takes part.
- [x] 3.3 Show the deleted rules as a notice after saving participants, and the `rules_conflict` message with a way to the rules after a refused draw, in `AdminGroupViewModel`.
- [x] 3.4 View model and API tests for all of it, including read-only phases and the notices.

## 4. App: screens

- [x] 4.1 Draw `GavelIcon` as an `ImageVector` in `ui/form`, tinted by the content colour.
- [x] 4.2 Add `RulesContent` (list, empty line, warnings, add form with participant pickers, kind choice and "Both ways" toggle, delete icon buttons, read-only line) and the strings, in English like the rest.
- [x] 4.3 Add `AdminGroupRulesRoute` with the page and its back action, and register it in the navigation.
- [x] 4.4 Add the gavel icon button to the group page's action row for Pending, Active and Archived; on narrow windows it navigates to the route, on wide windows it sets `GroupSheet.Rules`, which `opensInPane()` includes and `GroupPane` renders, replaced by other panes.
- [x] 4.5 Add the web address `#/admin/groups/{id}/rules` to `WebAddresses` (admins only), and on a wide window open the group page with the rules pane; update `WebAddressesTest` and the README address table.
- [x] 4.6 Keep every participant page free of anything about rules; test that the participant models carry no rules.

## 5. Verification

- [x] 5.1 Run the shared and server tests and `openspec validate --specs`.
- [x] 5.2 Try it by hand on a narrow and a wide window: add a must and a never (one way and both ways), see a conflict refused, remove a participant with rules and read the notice, draw, and read the rules in Active and Archived.

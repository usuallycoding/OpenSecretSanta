## Purpose

Defines the draw rules an owner sets on a Pending group (who must give to whom, who must not), who can read and change them, how a rule set is checked when saved, how a change of roster cleans them up, and that participants never learn of them.

## ADDED Requirements

### Requirement: A rule is one-way and of one of two kinds
A draw rule SHALL name a giver and a receiver, who MUST both be participants of the group and MUST differ, and SHALL be of kind `must` (the giver gives to the receiver) or `never` (the giver does not give to the receiver). A rule SHALL apply only in the direction named: a rule about A giving to B says nothing about B giving to A.

#### Scenario: One direction only
- **WHEN** the owner adds the rule `never`, giver `cy`, receiver `di`
- **THEN** the draw may still make `di` give to `cy`

#### Scenario: Not a participant
- **WHEN** the owner adds a rule naming someone who is not a participant of the group
- **THEN** the response is 400 naming `receiver` or `giver` and no rule is stored

#### Scenario: Oneself
- **WHEN** the owner adds a rule whose giver and receiver are the same person
- **THEN** the response is 400 naming `receiver` and no rule is stored

### Requirement: Only the owner manages rules, and only in Pending
The server SHALL expose `GET /admin/groups/{id}/rules`, `POST /admin/groups/{id}/rules` and `DELETE /admin/groups/{id}/rules/{ruleId}`, usable only by the group's owner; anyone else SHALL be answered as if the group did not exist. Adding and deleting SHALL be allowed only while the group is Pending. Reading SHALL be allowed in Pending, Active and Archived.

#### Scenario: Add in Pending
- **WHEN** the owner adds a valid rule to a Pending group
- **THEN** the response is 201 with the stored rule and the rule is listed by `GET`

#### Scenario: Not in Draft
- **WHEN** the owner reads, adds or deletes rules of a Draft group
- **THEN** the response is 409 with code `invalid_state`

#### Scenario: Frozen after Pending
- **WHEN** the owner adds or deletes a rule of an Active or Archived group
- **THEN** the response is 409 with code `invalid_state` and the rules are unchanged

#### Scenario: Read after Pending
- **WHEN** the owner reads the rules of an Active or Archived group
- **THEN** the response lists the rules the group was drawn with

#### Scenario: Other admin or non-admin
- **WHEN** another admin requests the rules, or a user who is not an admin does
- **THEN** the response is 404 for another admin's group and 403 for a user who is not an admin

### Requirement: Both ways adds two rules
The add request MAY ask for both ways. The server SHALL then store two rules, one for each direction, as ordinary independent rules, and return both.

#### Scenario: A couple
- **WHEN** the owner adds a `never` rule for `cy` and `di` asking for both ways
- **THEN** the response holds two rules, `cy` to `di` and `di` to `cy`, and each can be deleted on its own

#### Scenario: One of the two conflicts
- **WHEN** a both-ways add would make one of its two rules conflict with the stored rules
- **THEN** the whole add is refused and neither rule is stored

### Requirement: A rule set must be satisfiable
The server SHALL refuse to store a rule when the resulting rule set could not be satisfied by any draw that keeps the draw's guarantees (one chain through all participants, nobody giving to themself). The refusal SHALL be 409 with code `rules_conflict`, a message naming the rules in conflict, and the stored rules unchanged. The scenarios below are the cases that must be caught; a rule identical to a stored one is refused as a duplicate.

#### Scenario: Two musts for one giver
- **WHEN** `ana` must give to `bo` and the owner adds that `ana` must give to `cy`
- **THEN** the response is 409 with code `rules_conflict` naming both rules

#### Scenario: Two musts for one receiver
- **WHEN** `ana` must give to `cy` and the owner adds that `bo` must give to `cy`
- **THEN** the response is 409 with code `rules_conflict`

#### Scenario: Must and never together
- **WHEN** `ana` must give to `bo` and the owner adds that `ana` must not give to `bo`
- **THEN** the response is 409 with code `rules_conflict`

#### Scenario: A small circle
- **WHEN** `ana` must give to `bo` and the owner adds that `bo` must give to `ana`, in a group of 4
- **THEN** the response is 409 with code `rules_conflict` because the two would only ever give to each other

#### Scenario: No chain left
- **WHEN** the owner adds `never` rules that leave some participant with nobody to give to
- **THEN** the response is 409 with code `rules_conflict`

#### Scenario: A duplicate
- **WHEN** the owner adds a rule the group already has
- **THEN** the response is 409 with code `rules_conflict` and nothing new is stored

### Requirement: Few possible draws are reported
The response to reading the rules SHALL say whether fewer than 10 different draws satisfy them, so that an owner can be warned that the rules leave little room. A group with no rules, and a group that is no longer Pending, SHALL never be reported as having few.

#### Scenario: Plenty of room
- **WHEN** the owner reads the rules of a Pending group of 8 with a single rule
- **THEN** the response says there are not few possible draws

#### Scenario: No rules
- **WHEN** the owner reads the rules of a Pending group of 4 that has none
- **THEN** the response says there are not few possible draws

#### Scenario: Almost decided
- **WHEN** the rules of a Pending group of 4 leave exactly one valid draw
- **THEN** the response says there are few possible draws

### Requirement: Removing a participant removes their rules
When the owner replaces the participant list of a Pending group and a removed participant is the giver or the receiver of a rule, the server SHALL delete those rules in the same step and SHALL list the deleted rules in the response, so that the owner can be told.

#### Scenario: Rules go with the person
- **WHEN** the owner removes `bo` from a Pending group in which `ana` must give to `bo` and `bo` must not give to `cy`
- **THEN** both rules are deleted and the response lists them

#### Scenario: Other rules stay
- **WHEN** the owner removes `bo` and a rule names only `ana` and `cy`
- **THEN** that rule is unchanged

#### Scenario: Nothing removed, nothing listed
- **WHEN** the owner only adds participants
- **THEN** the response lists no deleted rules

### Requirement: Rules are the owner's alone
No participant endpoint and no participant view SHALL carry or hint at the rules of a group, including whether it has any. Only the group's owner, through the endpoints above, SHALL be able to read them.

#### Scenario: Participant view
- **WHEN** a participant views a Pending, Active or Archived group that has rules
- **THEN** the response has no data about rules

#### Scenario: A participant asks for the rules
- **WHEN** a user who is not the owner requests `/admin/groups/{id}/rules`
- **THEN** the response is 403 or 404 as for any admin endpoint

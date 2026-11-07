## ADDED Requirements

### Requirement: The draw honours the group's rules
When a group is activated, the assignment SHALL contain every `must` rule of the group and none of its `never` rules, while keeping every other guarantee: one receiver and one Santa for everyone, nobody drawing themself, and one closed chain through all participants. Among the assignments that satisfy the rules, the choice SHALL still be random, drawn from a cryptographically secure source.

#### Scenario: A must is honoured
- **WHEN** a group of 6 with the rule that `ana` must give to `bo` is activated, repeatedly
- **THEN** `ana` always gives to `bo`

#### Scenario: A never is honoured
- **WHEN** a group of 6 with the rule that `cy` must not give to `di` is activated, repeatedly
- **THEN** `cy` never gives to `di`

#### Scenario: Still one chain
- **WHEN** a group with rules is activated
- **THEN** following the assignments from any participant visits everyone exactly once before returning

#### Scenario: Still random
- **WHEN** the same group with the same rules is activated many times
- **THEN** the assignments vary, within what the rules allow

#### Scenario: No rules, no change
- **WHEN** a group without rules is activated
- **THEN** the draw is as it was before rules existed

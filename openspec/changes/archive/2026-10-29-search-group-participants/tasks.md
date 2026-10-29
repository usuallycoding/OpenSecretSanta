## 1. Full names in the owner's view of a group

- [x] 1.1 Add `fullName` to `ParticipantStatus` and fill it in `GroupViewer`; add it (optional) to `ParticipantDto` and the server mapper
- [x] 1.2 Domain and server tests: a participant with a name carries it; one without a name or without an account carries none

## 2. Client filter and state

- [x] 2.1 Carry the full name in `AdminParticipant` and in the pair rows
- [x] 2.2 Add the participant and pair filter (words in any order over username and full name, a pair matches on either side) with tests
- [x] 2.3 Keep the typed search as Compose state in `AdminGroupViewModel` with its clear action; tests for the state, a blank search and the count staying whole

## 3. Client UI

- [x] 3.1 Add the search field above the participants and the pairs, the "no participant matches" message and the strings
- [x] 3.2 Check on the Android device: Pending list, Active pairs, a name with several words, no match, clearing

## 4. Integration

- [x] 4.1 Run the full test suites and `openspec validate search-group-participants --strict`
- [x] 4.2 Docker build and the person's web check

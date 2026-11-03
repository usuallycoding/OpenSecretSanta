## 1. The card

- [x] 1.1 Add `WHEN_LABEL` and `WHERE_LABEL` to `Strings.kt` and a pure function for the "When? - ..." and "Where? - ..." text, with a test for the date-and-time, date-only and location lines
- [x] 1.2 Add `GroupInfoCard` in `app/shared/src/ui/groups/`: a plain `Card` with the description, the "When?" line and the "Where?" line, semi-bold labels, drawing nothing when all three are missing

## 2. Use it

- [x] 2.1 Use it in the participant `Header` in `GroupPage.kt`, below the name and the state
- [x] 2.2 Use it in the admin `Header` in `AdminGroupScreen.kt`, below the state and above the action buttons

## 3. Check

- [x] 3.1 Run `./kotlin test -m shared` and `./kotlin build`
- [x] 3.2 Check on a device or the web, light and dark, on the participant page and the admin screen: all three present, only a location, nothing, a long description and a long location

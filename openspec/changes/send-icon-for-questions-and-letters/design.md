# Design

## Context

`AppFab(icon, description, onClick)` draws a floating button on a narrow window and, on a wide one, passes the same icon and description to the heading button (`PageActionEffect`), so a screen's icon is chosen once for both widths. The question's screen passes `QuestionIcon` (a drawn help circle in `form/AppFab.kt`); the letters' screen chooses `Icons.AutoMirrored.Filled.Send` on a wide window and `Icons.Filled.Add`, named `Strings.WRITE_LETTER`, on a narrow one (`LetterScreens.kt`). See proposal.md for why.

## Goals / Non-Goals

**Goals:**
- One icon, Send, and one name per action on every width.

**Non-Goals:**
- The server's order and the owner's pair thread; renumbering the questions; any other button's icon, the sheets' own "Send" and "Save" buttons, the cards' icons (already Send) and the narrow floating button's place.

## Decisions

**Choose the icon once, not by width.** `LetterScreens.kt` calls `AppFab(Icons.AutoMirrored.Filled.Send, Strings.SEND_LETTER, ...)` for both widths, and `ReceiverScreen.kt` passes the same Send icon. Alternative rejected: keeping the width check and changing only its narrow branch, which leaves the two widths free to drift again.

**Unify the name by using the existing text.** The narrow button takes `Strings.SEND_LETTER` ("Send letter"), which the wide button already shows; `WRITE_LETTER` has no other use and goes. "Ask a question" keeps its name.

**Remove what no longer has a use.** `QuestionIcon` and its path are deleted from `AppFab.kt`.

**Newest first on the client, as the letters do.** The server keeps answering oldest first (the owner's pair thread keeps that order too). The view models that build the two lists (`ReceiverViewModel` for the questions to the receiver and `SantaQuestionsViewModel` for the questions from the Santa) sort by number descending, as `LetterModels.newestFirst` does, and a question just asked is put first in the list instead of last. Numbers are kept, so "Question 3" is at the top. The number is the order asked, so it is the date order. Alternative rejected: reversing in the screens, which would leave the data in one order and the view in another for every later change.

## Risks / Trade-offs

- [A Send icon on "Ask a question" can read as sending something at once, not opening a sheet] → The cards on the group page already use it for the same action, and the button keeps its name on a wide window and for assistive technology.
- [An arrow that points right on a left-to-right layout] → It is the auto-mirrored icon, as the other uses are.

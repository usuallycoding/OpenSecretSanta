# Proposal

## Why

Once the names are drawn a Santa needs to know where to post the gift and may need to ask what the receiver would like, and the receiver needs to see and answer those questions without learning who asked. The server already supports all of it: a receiver's postal details, a numbered question thread from each Santa, answers, and the reveal of the Santa once the group is archived. The app shows none of it, so after the draw the group page leads nowhere. This is the last participant piece (1c) after the groups and profile screens and the wishlists.

## What Changes

- **"Your gift" section on the group detail.** Once the group is Active or Archived, below the wishlists: "Your receiver" and "Questions from your Santa", and once it is Archived also "Your Santa". In a Pending group the section is absent.
- **Your receiver.** One screen with the receiver's username and the postal details the server returns (full name, address, phone, delivery information), leaving out what the receiver has not set and saying so when there is nothing to post to. Below it the person's own questions to the receiver, numbered, each with its answer or "Not answered yet", and an "Ask a question" button that opens a bottom sheet. Asking is only possible while the group is Active, up to 30 questions; a question cannot be changed or deleted once asked.
- **Questions from your Santa.** The questions the person's Santa asked, numbered, with the person's own answers. "Answer" or "Change answer" opens a bottom sheet with the current text. Answers can be changed while the group is Active; in an Archived group the thread is read-only. Nothing on this screen says who asked.
- **Your Santa.** Only in an Archived group: the Santa's username and full name.
- **Rules the app shares with the server.** Text is trimmed and checked before sending (a question 1 to 1000 characters, an answer 1 to 2000); the server's field error is shown next to the field.
- **Concurrent changes.** If the group was archived while a sheet was open, the app closes the sheet, reloads and says the thread can no longer change. If a question the person answers is gone, the app reloads and says so.

The server needs no change.

Out of scope:
- Notifications when a question or an answer arrives; people see new text when they open the screen, press Refresh or return to the app.
- Editing or deleting a question, copying an address, marking a gift as sent, and all admin screens.

## Capabilities

### New Capabilities
- `client-receiver-and-questions`: how a participant sees their receiver's details, asks and reads questions, answers their Santa's questions and sees who their Santa was once the group is archived.

### Modified Capabilities
- `client-groups`: the group detail gains the "Your gift" section with its state-dependent entries.

## Impact

- Code: six calls in `ApiClient` (`receiver`, `receiverQuestions`, `askQuestion`, `santaQuestions`, `answerQuestion`, `santa`), three view models and three screens with the shared bottom sheet in `app/shared`, three typed routes, strings and the README.
- APIs: none new. The app uses the existing `/groups/{id}/receiver`, `/receiver/questions`, `/santa/questions`, `/santa/questions/{n}/answer` and `/santa` routes with the contract types that already exist.
- Dependencies: none.
- Systems: none.

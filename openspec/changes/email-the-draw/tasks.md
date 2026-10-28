# Tasks

## 1. Domain

- [x] 1.1 Add `EmailMessage`, `EmailSummary`, the `Mailer` port, the pure message builder and `SendDrawEmails` in `core/domain` with a fake mailer; cover who gets which receiver, the details and the missing ones, nothing private in subject or body, participants without an account or address, a disabled mailer (null), failures counted, and that no email is built for a draw that was not stored

## 2. Server

- [x] 2.1 Add the email settings to `ServerConfig` with their validation and a `toString` that hides the key; cover each scenario of the `server-runtime` delta in the config tests
- [x] 2.2 Add `ResendMailer` (batch of at most 100, bearer key, 10 second timeout, one retry on 429, fallback per message) and `DisabledMailer`, wire them in `ServerModules`, and test the adapter against a local HTTP server (request shape and headers, chunking at 100, 2xx, 4xx, 5xx, a timeout, nothing about the key in logs)
- [x] 2.3 Add `EmailSummaryDto` and `GroupChangeResponse.emails`, call `SendDrawEmails` from the activation route after the draw (any unexpected failure becomes "all failed"), and cover every scenario of `draw-emails` in route tests (summary, mail failure still 200 and Active, refused activation sends nothing, second activation 409 sends nothing, not configured has no `emails`)

## 3. App

- [x] 3.1 Keep the activation's summary in `AdminGroupViewModel` (`emails`, `dismissEmails`), show it once on the group's page with `Strings.emailSummary`, add the Profile sentence, and cover the view model and the strings in tests
- [x] 3.2 Verify in headless Chrome against a throwaway server whose `RESEND_API_KEY` points at a local mock of Resend (set through a test-only base URL variable or a stub on localhost): the draw succeeds, the note reads correctly with sent, failed and no-address participants, and nothing is shown when email is off

## 4. Deployment and docs

- [x] 4.1 Add the three optional variables to `.env.example` and `docker-compose.yml` (server only), write the README section (setup with Resend and a verified domain, the 100 a day limit, what the email contains and what it leaves out), run `./kotlin build` and `./kotlin test` for the whole repo and note what could not be tried (a real send through Resend)

## Workflow follow-up

- Archive the change after review and sync the delta specs into `openspec/specs`.

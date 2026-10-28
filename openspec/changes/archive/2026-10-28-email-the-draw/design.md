# Design

## Context

`POST /admin/groups/{id}/activate` runs `ActivateGroup`, which stores the draw in one transaction (`groups.activate`) and returns a `GroupView`; `LifecycleRoutes` answers with a `GroupChangeResponse(group, createdAccounts)`. Profiles carry an optional validated `email`. The domain is blocking (routes call it through `blocking {}`) and has ports for everything outside (repositories, hasher, clock). The server module has no HTTP client dependency (`ktor-client-js` exists only for the web app) but runs on a JDK that has `java.net.http.HttpClient`. Configuration is a `ServerConfig` read from the environment, with a failing `ConfigurationException` per bad variable. The owner has a Resend account and a verified sending domain (`mail.plm360.org`); the free plan allows 100 emails a day and 3,000 a month.

## Goals / Non-Goals

**Goals:**
- Each participant with an address gets their own draw by email when the group becomes Active.
- The draw never fails or waits on email; the owner learns how many were sent.
- Off by default, no new dependency, key never exposed.

**Non-Goals:**
- Emails for any other event (Pending, Archive, questions, answers), resending, a per-person opt-out, HTML emails, translations, storing a record of what was sent, a retry queue.

## Decisions

1. **Port and use case in the domain.** `Mailer` (`val enabled: Boolean`, `fun send(messages: List<EmailMessage>): List<Boolean>`, one result per message, never throws for ordinary failures) and `SendDrawEmails(users, groups, mailer, from the app address)`: `invoke(group: Group): EmailSummary?` returns null when the mailer is disabled, otherwise loads the participants' accounts and the assignments, builds one `EmailMessage(to, subject, text)` per participant with an address, sends them and returns `EmailSummary(sent, failed, noAddress)`. The route calls it after `ActivateGroup` returned, so nothing runs for a refused activation, and wraps it so that any unexpected exception becomes "all failed" (logged), never a 500.
2. **Message.** Subject `Your Secret Santa draw: <group name>` (the receiver is not in the subject, which shows on lock screens). Plain text body: greeting with the username, "The names for "<group>" have been drawn.", `You are Secret Santa for: <receiver>` plus ` (<full name>)` when set, then the description, `When:` and `Where:` lines for what is set, the app address when configured, and a closing "Keep it secret!". No address, phone or delivery information. Built by a pure function, so it is unit tested.
3. **Resend adapter.** `ResendMailer(apiKey, from, http)` in `server/src/data/email`, using `java.net.http.HttpClient` with a 10 second timeout. It posts to `https://api.resend.com/emails/batch` with `Authorization: Bearer <key>` and a JSON array of `{from, to, subject, text}` objects, in chunks of at most 100 (the batch limit), one request at a time, which also respects Resend's per-second request limit. A 2xx answer marks the whole chunk sent; any other answer, a timeout or an I/O error marks the whole chunk failed; one retry after the `Retry-After` delay (at most 2 seconds) on a 429. Failures are logged with the status and Resend's error name, never the key, the addresses or the text. The endpoint, the batch limit and the rate limit are to be checked against Resend's current documentation when implementing; if the batch endpoint behaves differently (for example it fails a whole batch for one bad address), the adapter falls back to one request per message with the same counting.
4. **Configuration.** `ServerConfig` gains `resendApiKey: String?`, `emailFrom: String?` and `appUrl: String?`, read from `RESEND_API_KEY`, `EMAIL_FROM` and `APP_URL` (blank means unset). Validation in `fromEnv`: a key without `EMAIL_FROM` fails naming `EMAIL_FROM` (never printing the key), `EMAIL_FROM` must match `Name <a@b.c>` or `a@b.c`, `APP_URL` must be an http or https URL. With a key and a sender the Koin module provides `ResendMailer`; otherwise a `DisabledMailer` (`enabled = false`). `ServerConfig`'s generated `toString` would print the key: the config class overrides `toString` (or the key lives in a wrapper whose `toString` is masked) so no log line can contain it.
5. **Contract.** `GroupChangeResponse` gains `emails: EmailSummaryDto? = null` (`sent`, `failed`, `noAddress`); the activation route sets it from the use case's result, every other route leaves it null, and older clients ignore it.
6. **App.** `AdminGroupViewModel.confirmStep` keeps the summary of an activation in `ui.emails` (a one-time note); the page shows it under the notice row with an OK button (`dismissEmails()`), in words built by a pure `Strings.emailSummary(sent, failed, noAddress)` ("Emails sent to all 4 participants." / "Emails sent to 8 of 11 participants. 1 failed, 2 have no email address."). No summary, no note.
7. **Profile text.** `Strings.PROFILE_WHY` gains a sentence: "Your email is used to send you your draw when a group you are in is drawn."
8. **A test hook.** `RESEND_API_URL` (optional, default `https://api.resend.com`) points the adapter at a stand-in; it is read by `ServerConfig`, used by the tests and the browser check, listed in the README as test-only and not passed through the compose file. Route tests inject a fake `Mailer` through `module(..., mailer = ...)`.
9. **Deployment.** `.env.example` documents `RESEND_API_KEY=`, `EMAIL_FROM=` and `APP_URL=` as optional, `docker-compose.yml` passes them to the server only (`${RESEND_API_KEY:-}`), and the README explains the domain setup in one paragraph and the 100 a day limit.
10. **Ordering and atomicity.** Activation commits first; mail goes out afterwards in the same request. A crash between the two means a drawn group whose emails were never sent, which the owner sees as no summary; participants still see the draw in the app. Accepted.

## Risks / Trade-offs

- The request waits for Resend (typically a second, at most about 10 seconds plus one short retry) before the owner's screen updates; the draw itself is already stored.
- Emails reveal each person's receiver to a third party (Resend) and sit in mailboxes; the subject stays neutral and the Profile text says what the email is for. People who set an email before this change will start receiving mail, which the text tells them about only afterwards.
- The free plan's 100 emails a day can be exceeded by drawing several large groups in a day; the extra emails fail and are counted. No retry is built.
- A mistaken `EMAIL_FROM` domain that is not verified makes every send fail (counted and logged); the owner sees "N failed".

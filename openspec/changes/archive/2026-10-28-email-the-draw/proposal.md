# Proposal

## Why

When the owner draws the names, nobody is told: each person has to open the app to learn who they are Secret Santa for. Profiles already hold an optional email address, and the owner has a Resend account with a verified sending domain. Sending each person their own draw by email when the group becomes Active saves everyone the trip and makes sure the news reaches them.

## What Changes

- When the owner moves a group to Active ("Draw the names"), the server emails every participant who has an email address on their account: the group's details, that the names have been drawn, and who that person is Secret Santa for. Each person receives only their own receiver; no postal details are in the email.
- The emails are sent through Resend after the draw is stored. The draw never depends on them: a failure is counted and logged, and the group is Active either way.
- The activation response says how many emails were sent, how many failed and how many participants had no email address, and the owner's page shows that once ("Emails sent to 9 of 11 participants. 1 failed, 2 have no email address.").
- The feature is off unless configured: `RESEND_API_KEY` and `EMAIL_FROM` (and optionally `APP_URL`, a link to the web app for the email) come from the environment and the Docker `.env`. Without them nothing is sent and the response has no email summary.
- The Profile tab says the email is used to send the person their draw.
- No database change.

## Capabilities

### New Capabilities

- `draw-emails`: emailing each participant their draw when a group becomes Active.

### Modified Capabilities

- `server-runtime`: the optional email settings and their validation.
- `docker-deployment`: the email settings in `.env` and the compose file.
- `client-profile`: the note on what the email is used for.
- `client-admin-groups`: saying who was emailed after the draw.

## Impact

- Domain: a `Mailer` port and `SendDrawEmails` use case in `core/domain`; the contract gains an optional `emails` summary on `GroupChangeResponse`.
- Server: a Resend adapter (`java.net.http.HttpClient`, no new dependency), `ServerConfig` and `ServerModules`, `LifecycleRoutes`, tests with a local mock server.
- App: `AdminGroupViewModel` and `AdminGroupScreen` (the one-time summary), the Profile tab text, `Strings.kt`, tests.
- Deployment: `.env.example`, `docker-compose.yml`, README.

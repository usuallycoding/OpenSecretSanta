This is a Kotlin Multiplatform project targeting Android, iOS, Web, Server, built with the [Kotlin Toolchain](https://kotlin-toolchain.org/dev/).

- [/app/androidApp](./app/androidApp) contains the Android application.
- [/app/iosApp](./app/iosApp) contains the iOS application.
- [/app/shared](./app/shared) holds the code shared across your applications — Compose UI, business logic, and platform-specific implementations. [src](./app/shared/src) is for common code; the sibling `src@<platform>` folders (for example `src@android`) hold code compiled only for the platform named in the folder.
- [/app/webApp](./app/webApp) contains the web application, compiled to WebAssembly with Kotlin/Wasm.
- [/core/domain](./core/domain) is the pure-Kotlin domain layer (entities, use cases, repository interfaces). It has no serialization, Ktor or platform dependencies.
- [/core/contract](./core/contract) holds the DTOs and route constants shared by the server and the apps. It depends on `core/domain`.
- [/server](./server) contains the Ktor server application.

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts in the project root are self-bootstrapping wrappers for the Kotlin Toolchain: they download the pinned toolchain version on first use, so no separate installation is required. Build the whole project with `./kotlin build`.

### Running

Use the run configurations provided by the run widget in your IDE's toolbar. You can also run each module from the command line:

- Android app: `./kotlin run -m androidApp`
- iOS app: `./kotlin run -m iosApp`
- Web app: `./kotlin run -m webApp`
- Server: `./kotlin run -m server`

### Server configuration

The server reads its configuration from environment variables and refuses to start if one is invalid:

| Variable              | Required                  | Default              | Meaning                                                              |
|-----------------------|---------------------------|----------------------|----------------------------------------------------------------------|
| `JWT_SECRET`          | yes                       | -                    | Secret used to sign access tokens                                    |
| `DATA_ENCRYPTION_KEY` | yes                       | -                    | Base64 of a 32-byte key; encrypts temporary passwords stored for admins |
| `DB_PATH`             | no                        | `opensecretsanta.db` | SQLite database file (created if missing)                            |
| `PORT`                | no                        | `8080`               | Port the server listens on (1-65535)                                 |
| `ADMIN_USERNAME`      | no                        | `admin`              | Username of the first administrator                                  |
| `ADMIN_PASSWORD`      | only on an empty database | -                    | Initial password of the first administrator (at least 8 characters)  |
| `CORS_ALLOWED_ORIGINS`| no                        | empty (no CORS)      | Comma-separated web origins allowed to call the API from a browser, for example `https://app.example.org,http://localhost:8081` |
| `RESEND_API_KEY`      | no                        | -                    | Resend API key; with `EMAIL_FROM` it turns on the emails sent when a group is drawn (see below) |
| `EMAIL_FROM`          | with `RESEND_API_KEY`     | -                    | The sender, `santa@mail.example.org` or `Name <santa@mail.example.org>`, on a domain verified at Resend |
| `APP_URL`             | no                        | -                    | Where the web app is, linked from the emails (`https://app.example.org`) |
| `RESEND_API_URL`      | no                        | `https://api.resend.com` | Only for tests: points the server at a stand-in for Resend |
| `FCM_SERVICE_ACCOUNT_FILE` | no                   | -                    | Path to a Firebase service-account key (JSON); with it the server sends push notifications to Android and web (see below), without it none |
| `FCM_API_URL`         | no                        | `https://fcm.googleapis.com` | Only for tests: points the server at a stand-in for FCM |

#### Emails when the names are drawn

When the owner draws a group's names the server can email every participant who has an email address on their profile: the group's details, that the names have been drawn and who they are Secret Santa for (their own receiver only; no postal details are in the email, and the subject does not name the receiver). It uses [Resend](https://resend.com), is off unless `RESEND_API_KEY` and `EMAIL_FROM` are both set, and never decides whether the draw happens: the group is Active even when no email can be sent, and the owner's page says how many went out (`Emails sent to 8 of 11 participants. 1 failed, 2 have no email address.`). Participants see on their Profile tab that their email is used for this.

To set it up, add a domain to Resend (a subdomain such as `mail.example.org` keeps its DNS records apart from any mail you already receive on the main domain), create the DNS records Resend lists (DKIM, SPF and MX, normally TXT and MX records named `resend._domainkey.mail`, `send.mail` and `send.mail`) at your DNS provider, wait for the domain to show as verified, create an API key with sending access to that domain and put it in `RESEND_API_KEY`, with a sender on the domain in `EMAIL_FROM`. The free plan allows 100 emails a day and 3,000 a month with one domain; an email that does not go out because the limit is reached is counted as failed and not retried. The key is never logged or returned by the API.

#### Announcements

The owner of a Pending or Active group can send its participants a message of their own, such as a change of time or a reminder, from the **Send announcement** button on the group's page, as an email, as a push notification or both (`POST /admin/groups/{id}/announcements`). The email has the draw email's layout with the message in place of the draw lines and the subject `<title>: <group name>`; the push notification has the same title and only the message, cut at 300 characters, and respects each person's Announcements switch. A channel the server has not set up (see above) is shown greyed out in the app and refused by the server. The title is one line of up to 100 characters and the message up to 2000. Nothing is stored: there is no history, failed emails are counted and not retried, and the emails come from the sender address, so replies go nowhere. The 100 emails a day of Resend's free plan apply to announcements too.

#### Push notifications

Android and the web app can notify people when something happens to them: they are added to a group or its names are drawn (**announcements**), the person they give to changes their wishlist (**wishlist**), their Santa asks them a question (**questions**) or the person they give to answers one (**answers**). Each person chooses which of the four they want, and which devices, in **Notifications** in the top-bar menu. The texts name only the group, never a Santa, a question, an answer or a wishlist item, because they can be read on a locked screen. iOS is not supported (it needs a paid Apple developer account).

Push goes through [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) (free, no billing needed). It is off unless the server has a service-account key; a notification that cannot be sent never affects the action that caused it.

To set it up:

1. In the [Firebase console](https://console.firebase.google.com) create a project (Analytics is not needed).
2. **Android:** add an Android app with the package name `com.usuallycoding.opensecretsanta` and put the `google-services.json` it offers in `app/androidApp/`. The build applies Google's plugin to it by itself. The file holds public client settings; it is git-ignored here so that each checkout uses its own project.
3. **Web:** add a web app and note the `apiKey`, `projectId`, `messagingSenderId` and `appId` of its config. Under *Project settings, Cloud Messaging, Web Push certificates* generate a key pair and note the public key.
4. **Server:** under *Project settings, Service accounts* generate a private key. This JSON file is a secret: it can send push messages as your project. Keep it in the git-ignored `secrets/` folder, never inside `app/webApp/resources` (which is served to every visitor).
5. Put the settings in `.env` (see `.env.example`): `FCM_KEY_FILE` is the name of the key file inside `secrets/`, and `PUBLIC_FIREBASE_API_KEY`, `PUBLIC_FIREBASE_PROJECT_ID`, `PUBLIC_FIREBASE_MESSAGING_SENDER_ID`, `PUBLIC_FIREBASE_APP_ID` and `PUBLIC_FIREBASE_VAPID_KEY` are the public web settings. Without `FCM_KEY_FILE` the server runs with push off and the sheet says notifications are not available.
6. Restrict the API keys in the Google Cloud console to your app and your web address once you have a real domain.

Browsers allow push only on `https://` addresses and on `localhost`, so the web side works on localhost as it is and needs TLS in front of a real address. Run on its own (`./kotlin run -m server`), the server takes the key from `FCM_SERVICE_ACCOUNT_FILE=secrets/<file>.json`.

Generate the encryption key once and keep it with your other secrets. Losing it makes stored temporary passwords unreadable (an admin can simply reset them), and rotating it has the same effect:

```
openssl rand -base64 32
```

First boot:

```
JWT_SECRET=change-me \
DATA_ENCRYPTION_KEY="$(openssl rand -base64 32)" \
ADMIN_PASSWORD=choose-a-password \
./kotlin run -m server
curl localhost:8080/health   # {"status":"ok","schemaVersion":<n>}
```

`CORS_ALLOWED_ORIGINS` is only needed when the web app is served from a different origin than the server. Each entry is `scheme://host[:port]` with no path, no trailing slash and no wildcard (`*` is refused at startup). When it is empty the server adds no cross-origin headers at all; when it is set, a browser request from an origin that is not listed is refused with `403`, while same-origin requests and native apps (which send no `Origin`) are unaffected.

On an empty database the server creates the administrator from `ADMIN_USERNAME` / `ADMIN_PASSWORD`, who must change that password at first login. Once any user exists, `ADMIN_USERNAME` and `ADMIN_PASSWORD` are ignored, so later restarts do not need them (but still need `JWT_SECRET` and the same `DATA_ENCRYPTION_KEY`).

Login is limited to 20 requests per minute per client address, and a username is locked for 15 minutes after 5 consecutive failed logins. The limiter keys on the connection's remote address, so behind a reverse proxy every client would appear as the proxy; forwarding the real address (`X-Forwarded-For`) is not configured yet.

Pending database migrations are applied automatically at startup. They live in `server/resources/db/migration` and are listed, in order, in that folder's `index.txt`.

### Run with Docker

`docker-compose.yml` builds and runs the server and the web app, with nothing but Docker on the machine. Every setting comes from a `.env` file next to it, which is not committed:

```
cp .env.example .env        # then fill it in: at least JWT_SECRET, DATA_ENCRYPTION_KEY and ADMIN_PASSWORD
openssl rand -base64 32     # a value for DATA_ENCRYPTION_KEY
docker compose up --build
```

The first build downloads the Kotlin toolchain and the dependencies and takes several minutes; later builds are much faster. Then the web app is at `http://localhost:8080` and the server at `http://localhost:8081`. Sign in as `admin` with `ADMIN_PASSWORD`; you are asked to choose your own password straight away.

| `.env` variable | Default | Meaning |
|---|---|---|
| `JWT_SECRET` | required | the server's `JWT_SECRET` |
| `DATA_ENCRYPTION_KEY` | required | the server's `DATA_ENCRYPTION_KEY` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | `admin`, none | the first administrator; only used while the database has no users, so the password can be removed after the first start |
| `RESEND_API_KEY`, `EMAIL_FROM`, `APP_URL` | none | optional: the emails sent when a group is drawn (see above); they reach the server only |
| `WEB_PORT`, `SERVER_PORT` | `8080`, `8081` | the ports published on this machine |
| `PUBLIC_SERVER_URL` | `http://localhost:` and `SERVER_PORT` | where the browser finds the server; written into the web app's `config.json` each time the web container starts, so changing it needs a restart (`docker compose up -d`), not a rebuild |
| `PUBLIC_WEB_ORIGIN` | `http://localhost:` and `WEB_PORT` | the address the web app is opened at; the server's `CORS_ALLOWED_ORIGINS` |

Compose stops and names the variable when a required one is missing, and the server container exits naming a variable it finds invalid. The database lives in the named volume `data`, so removing or rebuilding the containers keeps your users and groups (`docker compose down -v` deletes it). To back it up, copy the file out with `docker compose cp server:/data/opensecretsanta.db .`; to start from an existing database, copy it in the same way (to `server:/data/opensecretsanta.db`), give the file to the server's user with `docker compose exec -u root server chown santa /data/opensecretsanta.db`, and restart the server. The server runs as an unprivileged user and has a healthcheck on `/health`; the web container starts once it is healthy.

#### Run on a Raspberry Pi (or any machine) without building

GitHub Actions builds the server and web images on every push to `main` and publishes them for `linux/arm64` (a Raspberry Pi with a 64-bit OS) and `linux/amd64` as `ghcr.io/usuallycoding/opensecretsanta-server` and `ghcr.io/usuallycoding/opensecretsanta-web`. A machine only needs Docker and three things from this repository and your own settings, so nothing has to be carried over on a USB stick and no toolchain is installed:

```
docker-compose.images.yml   the file in this repository that has no build in it
.env                        your settings, made from .env.example
secrets/                    only if you use push: the Firebase service-account key file
```

```
docker compose -f docker-compose.images.yml pull     # fetches the published images
docker compose -f docker-compose.images.yml up -d    # starts (or restarts) the stack on them
```

Use `docker-compose.images.yml` there, not `docker-compose.yml`: the latter also has a `build:` for each service, which needs the whole repository next to it. (Adding `COMPOSE_FILE=docker-compose.images.yml` to `.env` saves typing `-f`.)

In Portainer, create a stack with the web editor, paste the contents of `docker-compose.images.yml`, and add the variables of your `.env` as the stack's environment variables (or load the file with "Load variables from .env file"). Portainer runs the stack from a folder of its own, so for push set `SECRETS_DIR` to the full path of the folder on the Pi that holds the key file (for example `/home/pi/santa/secrets`); without push nothing else is needed.

Run the same two commands to update (in Portainer, "Pull and redeploy" on the stack): the data volume is kept and a database migration, if there is one, runs when the server starts. `IMAGE_TAG` in `.env` picks the image: `latest` is the newest `main`, a commit (for example `0e3f2a1`) or a version tag (`1.2.0`, or `1.2` for its newest fix) pins one, so putting an earlier one there and running the two commands again rolls back. Push a tag such as `v1.2.0` to publish a version.

The images contain no `.env`, no key and no database, and the web image reads its server address when it starts, so `PUBLIC_SERVER_URL` and `PUBLIC_WEB_ORIGIN` in `.env` are all it needs to be reached at the Pi's address (for example `http://raspberrypi.local:8081` and `http://raspberrypi.local:8080`).

One thing is done once by hand: GitHub creates a new package as private, so after the first run of the workflow open each of the two packages under your profile's Packages, then Package settings, and change its visibility to public. Until then the Pi would have to run `docker login ghcr.io` first.

Things this setup does not do. It does not provide TLS: a browser refuses an `https` page that calls an `http` server, and the Android and iOS apps only accept plain `http` for development hosts, so a deployment that is not just for your own network puts TLS in front of both services and sets the two public addresses to `https`. The login limit keys on the client's address, which with Docker Desktop is the same for everyone, so it is shared by all clients there. `docker compose up --build` builds the images for the machine's own architecture, and there is no hot reload; use `./kotlin run` for development.

### Groups and their lifecycle

An admin owns the groups they create and nobody else can see or change them (other admins get 404). A group moves strictly forward, one step at a time:

```
Draft --> Pending --> Active --> Archived
```

| State    | What it means                                                                                         |
|----------|-------------------------------------------------------------------------------------------------------|
| Draft    | Being planned. Participants are just usernames and need not have accounts yet. Participants cannot see it. |
| Pending  | Every participant has an account (new ones were created with a temporary password) and can see the group. People can still be added or removed. |
| Active   | The roster is frozen and the Secret Santa assignments have been drawn (one random cycle, nobody draws themself). Needs at least 4 participants: with only 3, everyone could work out their own Santa. |
| Archived | Finished and read-only.                                                                               |

| Method and path                        | Who   | Purpose                                                              |
|----------------------------------------|-------|----------------------------------------------------------------------|
| `POST /admin/groups`                   | admin | Create a Draft (name required; description, location, `date` as `2026-12-24`, `time` as `18:30`, participants optional) |
| `GET /admin/groups`, `GET /admin/groups/{id}` | owner | List / view, with a per-participant `hasAccount` and `profileComplete` flag |
| `PUT /admin/groups/{id}`               | owner | Edit details (any state but Archived)                                |
| `PUT /admin/groups/{id}/participants`  | owner | Replace the participant list (Draft and Pending only)                |
| `DELETE /admin/groups/{id}`            | owner | Delete a group in any state, with its draw, wishlist items, questions and answers; a group that has any of that answers `409 confirmation_required` with the counts until the call is repeated with `?confirm=true` |
| `POST /admin/groups/{id}/pending`      | owner | Draft to Pending; creates missing accounts and returns their temporary passwords |
| `POST /admin/groups/{id}/activate`     | owner | Pending to Active; draws the assignments                             |
| `POST /admin/groups/{id}/archive`      | owner | Active to Archived                                                   |
| `GET /admin/groups/{id}/assignments`   | owner | Every Santa and receiver pair (Active and Archived)                  |
| `GET /groups`, `GET /groups/{id}`      | any user | The non-Draft groups you participate in: details and participant usernames only |

`profileComplete` means the participant's account has both a full name and an address. It is only a flag: activation is never blocked by it. A move that the current state does not allow answers `409` with the code `invalid_state`; activating with fewer than 4 participants answers `400` naming `participants`.

An admin can view and reset the accounts of people who participate in a non-Draft group they own, so naming someone in a Draft gives no access to their account.

### Receiver, questions and the Santa reveal

Once a group is Active every participant is both a Santa (for their receiver) and a receiver (of their Santa). The routes are named by relationship, never by username, so a receiver can never read who their Santa is before the group is archived.

| Method and path                                   | Who      | Purpose                                                                       |
|---------------------------------------------------|----------|-------------------------------------------------------------------------------|
| `GET /groups/{id}/receiver`                       | the Santa | Your receiver's username, full name, address, phone and delivery information (Active and Archived) |
| `GET /groups/{id}/receiver/questions`             | the Santa | The questions you asked and any answers                                       |
| `POST /groups/{id}/receiver/questions`            | the Santa | Ask a question (`{"text": ...}`, 1-1000 characters, at most 30 per group, Active only) |
| `GET /groups/{id}/santa/questions`                | the receiver | The questions your Santa asked, with your answers, without any sign of who asked |
| `PUT /groups/{id}/santa/questions/{n}/answer`     | the receiver | Answer or change your answer to question `n` (`{"text": ...}`, 1-2000 characters, Active only) |
| `GET /groups/{id}/santa`                          | the receiver | Your Santa's username and full name, only once the group is Archived          |

Questions are numbered from 1 within each Santa's own thread, so the numbers say nothing about other threads. A question cannot be edited or deleted once asked, an answer can be changed as often as you like while the group is Active, and once the group is Archived both threads stay readable but nothing changes. Only the group's owner can read them, through the oversight routes below. Asking in a group that is not Active, answering in one, or asking the 31st question answers `409` with the code `invalid_state`.

A group needs at least 4 participants to be activated. With only 3, everyone could work out who their Santa is from who their receiver is; with 4 a participant's Santa is one of two people.

### Wishlists

Every participant keeps a separate wishlist **per group**, so someone in two groups has two lists. An item has a title (1-200 characters), an optional note (up to 1000) and an optional link (up to 2000, `http` or `https` only), and a list holds at most 50 items. Lists are available once a group has left Draft; they can be changed while the group is Pending or Active, so people can prepare before the draw, and are read-only once it is Archived.

| Method and path                                  | Who       | Purpose                                                                     |
|--------------------------------------------------|-----------|-----------------------------------------------------------------------------|
| `GET /groups/{id}/wishlist`                      | you       | Your items for this group, in your order, with ids and times                |
| `POST /groups/{id}/wishlist/items`               | you       | Add an item at the end (`{"title": ..., "note": ..., "url": ...}`)          |
| `PUT /groups/{id}/wishlist/items/{itemId}`       | you       | Replace an item's title, note and link; it keeps its place                  |
| `DELETE /groups/{id}/wishlist/items/{itemId}`    | you       | Remove an item (204)                                                        |
| `PUT /groups/{id}/wishlist/order`                | you       | Reorder: send every item id of your list in the order you want (`{"itemIds": [...]}`) |
| `GET /groups/{id}/receiver/wishlist`             | the Santa | Your receiver's list for this group (Active and Archived), without ids      |

Reordering must name exactly your current items, each once; if another device added or removed an item since you last looked, the call answers `400` naming `itemIds` and nothing moves, so re-read the list and try again. Links that are not plain `http`/`https` addresses (for example `javascript:` or `file:`) are refused, and internationalized host names must be written in their `xn--` form. Reading a receiver's list is never recorded, so a receiver cannot learn who looked. Only the group's owner can read wishlists, through the oversight routes below.

### Owner oversight

The owner of a group can read what is going on inside it, even when they also take part. The routes are read-only, answer `404` to any admin who does not own the group (and `403` to someone who is not an admin), and are never recorded: nothing a participant can see changes when the owner reads.

| Method and path                                              | Who   | Purpose                                                                                      |
|--------------------------------------------------------------|-------|----------------------------------------------------------------------------------------------|
| `GET /admin/groups/{id}/participants/{username}/wishlist`    | owner | That participant's wishlist for the group (Pending, Active and Archived), without item ids   |
| `GET /admin/groups/{id}/questions`                           | owner | Every thread (Active and Archived): the Santa, the receiver and all questions with answers and times |
| `GET /admin/groups/{id}/assignments`                         | owner | Every Santa and receiver pair (Active and Archived)                                          |

A Draft group has no wishlists yet and a Draft or Pending group has no threads; asking answers `409` with the code `invalid_state`. A username that is not a participant answers `404`. An owner who is also a participant sees their own Santa through these routes before the group is archived; the participant routes (for example `GET /groups/{id}/santa`) still refuse until then. The owner cannot change any wishlist, question or answer.

### The app

One Compose Multiplatform UI (`app/shared`) runs on Android, iOS and the web. It signs in, makes a person replace a temporary password, keeps the session, and shows a participant view for everyone plus an admin view for administrators, who switch between the two from the menu at the top right (each switch opens the other view on its first screen). The same menu has **Change password** for everyone, in either view: a sheet over the current screen asks for the current password and a new one twice, keeps you signed in on this device and signs you out everywhere else (the server ends every older sign-in when a password changes); closing the sheet forgets what was typed.

The participant view has two tabs:

- **Groups** lists the groups you are in (name, state, date and time, location, number of participants) in the order the server returns them. Tap one to see its details and who takes part; your own username is marked. A group shows as Pending until the organiser draws the names, then Active, then Archived. Nothing on these screens says who gives to whom. Draft groups are never shown. On Android and iOS pull a screen down to refresh it, or just come back to the app: the screens reload when the app returns to the foreground. The web app has no refresh control, because the browser's refresh reloads the page and keeps you signed in on the same screen.

  A group's name is one row with an ellipsis in the lists and on its page, where tapping a cut name opens a dialog with all of it. Below its details a group's page shows who gives to whom, from your side. While the group is Pending it is one row with your username and a gift icon that opens your wishlist. Once the names are drawn it is two rows: `you -> your receiver` and `Secret Santa -> you`, the name after each arrow at the end of its row. Tap a username for a sheet with that person's details: yours, or your receiver's (full name, address, phone and delivery information, as far as they have set them). **Secret Santa** can be tapped only once the group is Archived, and then reveals their username and name. The arrow of the first row opens the **Gifter** page, the arrow of the second row (and the gift icon) opens the **Receiver** page. At the end of the page everyone in the group is a chip (yours highlighted), wrapping over as many lines as needed; the whole page scrolls as one and can be pulled down to refresh. On a wide window (the web, a tablet) the whole app, bars and buttons included, stays within one centered column 640dp wide.

  Both pages have two tabs, **Wishlist** and **Questions**, which you can tap or swipe between (a touch swipe on a phone, a sideways scroll or trackpad swipe on the web), each scrolling and refreshing by itself (a Pending group's Receiver page has no questions yet, so no tabs). The **Wishlist** tab of the **Receiver** page (you receive) has your items in your order, the round **+** button opens a bottom sheet to write an item, and the pencil at the end of an item opens the same sheet to change its title, note and link, with a Delete button; the arrows under the pencil move the item up or down, every change being saved at once. Tapping the item itself does nothing, and an item's title is one row, with a dialog for the whole of it. Its **Questions** tab (once the names are drawn) lists what your Santa asked, with "Answer" or "Change answer" opening a bottom sheet; nothing on it says who the Santa is. The **Gifter** page (you give) has your receiver's wishlist in its **Wishlist** tab, read-only and in the order your receiver chose (a link opens a dialog first), and your questions in its **Questions** tab: the round **?** button opens a bottom sheet, you can ask up to 30, and a question cannot be changed or deleted; each shows your receiver's answer or "Not answered yet". Once the group is Archived everything stays readable but nothing changes. A list holds at most 50 items. Each part of a page loads by itself, so one failing shows "Try again" while the rest stays; pulling a page down reloads all of it.
- **Profile** shows your username and lets you edit your full name, email, phone, address and delivery information. A Santa uses your name and address to post your gift, so the screen asks for them while either is empty. Edits are kept when you switch tabs, and the email and the 500-character limit are checked before anything is sent. The address and the delivery information take several lines: Return starts a new line. Every multi-line field in the app (these two, a wishlist note, a question or an answer, a group description) grows with its text up to three rows and then scrolls inside itself. On a phone the forms lift above the keyboard and scroll, so the Save button can always be reached, and dragging a form closes the keyboard; dragging inside a multi-line field only scrolls its text.

The admin view starts on the administrator's own groups (never another administrator's). Each group on the list has a copy button (**Duplicate**), for a group in any state: after a confirmation it creates a Draft called "Copy of <name>" (cut to 100 characters when needed) with the same description, location, date, time and participants, and opens it; nothing of the draw, wishlists or questions is copied. The round **+** button (**New group**) opens a form for name, description, location, date and time; the date is typed as `2026-12-24` and the time as `18:30`, and everything is checked before sending exactly as the server checks it. A group's page shows its state and what it means, its participants (a Draft group shows them as chips, a Pending one as rows; a person without an account or with an incomplete profile is followed by `*`, explained by one legend under the list) and what can be done in that state:

| State    | What the page offers                                                                                          |
|----------|---------------------------------------------------------------------------------------------------------------|
| Draft    | Edit details, edit participants, **Make pending**, **Delete**                                                 |
| Pending  | Edit details, edit participants (people added now get an account), **Draw the names** (needs at least 4 people), **Delete** |
| Active   | Edit details, the roster read-only, **Archive**, **Delete**                                                   |
| Archived | Everything read-only, except **Delete**                                                                       |

The buttons of a group (Edit details, the next step and Delete) are in one row under its details, wrapping on a narrow screen; below them the participants, or once the names are drawn the pairs, fill the rest of the page in a box that scrolls inside itself. Each step asks for a confirmation that says what it does. **Delete** asks once for a Draft and for a Pending group nobody has added a wishlist item to; for any group that has a draw, wishlist items, questions or answers (every Active and Archived group) a second dialog follows that says how many of each will be deleted with the group, and that it cannot be undone. Cancelling at either dialog deletes nothing, and there is no undo. When a step creates accounts, a dialog shows each new username with its temporary password once, selectable so it can be copied; the people must change it when they first sign in, and the app keeps none of it after the dialog is closed. The **Users** tab (the admin view's second tab) lists the people you manage: everyone in your Pending, Active and Archived groups, and yourself. A user's page shows the profile they have set and, for as long as they have not changed it, their temporary password, selectable so you can pass it on. **Reset password** (not offered on your own account) asks for a confirmation, then shows the new temporary password once; the person's old password and sign-ins stop working and they must change the new one when they next sign in. There is no way to make anyone an administrator: the first administrator comes from `ADMIN_USERNAME` and `ADMIN_PASSWORD` and the server has no promote route.

Owner oversight is read-only and never recorded. A Pending group lists its participants as rows: choosing a name opens that person's details sheet and the gift icon at the end of the row opens their wishlist (neither does anything for a person who has no account yet) in a bottom sheet, in their order, with a link that opens in the browser and when each item was added or last changed. Once the names are drawn (an Active or Archived group) the participants list is replaced by one list of pairs, `santa -> receiver`, in the order the server returns them, the receiver at the end of the row. Your own side is marked "(you)" even when you take part, and a username whose profile still lacks a name or an address is followed by `*`, with the legend "* profile incomplete" under the list. Choosing a username opens a bottom sheet with that person's details, their temporary password while it is temporary and **Reset password** (not offered on your own name), the same as on the Users tab. Choosing the arrow opens a sheet for the pair: the receiver's wishlist and, below it, the questions the Santa asked them with the receiver's answers, "Not answered yet" where there is none, and when each was asked, answered and last changed. A Draft group offers none of this. If the group or person is gone, or the group has moved to a state where the read no longer applies, the sheet says so and offers to close; if the server cannot be reached each part of a sheet says so with its own "Try again" and keeps what it already showed. The sheets are part of the group's screen, not screens of their own, so the web address stays on the group while one is open.

The app uses its own colour palette and type (`app/shared/src/ui/theme`: `Color.kt`, `Theme.kt`, `Type.kt`; each platform's default font) and follows the device's light or dark setting. The top-bar menu's **Appearance** opens a sheet to choose **Follow device** (the default), **Light** or **Dark**, applied at once; on Android 12 and later it also has a **Dynamic colours** switch (off by default) that takes the colours from the wallpaper. The choice belongs to the device, not to an account: it stays across restarts and signing out (`SharedPreferences` on Android, `NSUserDefaults` on iOS, `localStorage` on the web), and the Android window and the web page background follow the device's light or dark setting while the app loads.

Every time the app shows (when a wishlist item was added or changed, when a question was asked, answered or its answer changed) is in Bucharest time, summer time included, such as `1 Dec 2026, 12:00`, whatever time zone the device is set to; the server still sends and stores them in UTC. Participants see these times in a small line under their own wishlist items, under the receiver's items on the Gifter page and under the questions on both pages, and the owner sees the same in the oversight sheets.

Read-only text that other people wrote is shown in at most three lines, with an ellipsis; choosing text that was cut opens a dialog with all of it (text that fits does nothing). Every such value (a user's or receiver's profile fields and username, a wishlist item's title, note and link, a question and its answer, a group description, the Santa reveal and a temporary password) ends with a copy icon that puts the whole text on the clipboard and shows a check mark for a moment. Choosing a wishlist link opens a dialog with the whole link, **Open in browser** and **Cancel**, instead of leaving the app at once. On the web the clipboard only works on a secure page (`https` or `localhost`); on a plain `http` address such as `http://192.168.1.20:8080` the icon turns red and nothing is copied, so put TLS in front of a deployment that people reach by address.

Back works the platform's way: the system back button or gesture on Android (from the Groups list it leaves the app; from Profile it goes back to Groups first), the edge swipe on iOS and the browser's back button on the web, where the address bar also follows the screens with short readable addresses (see below). After a page refresh the web app keeps you signed in and opens the screen named in the address; so does a new tab or window when you signed in with "Stay signed in" ticked, while with it unticked a new tab or window, and any visit once the server no longer accepts the session, starts at the sign-in screen. A bottom sheet on a group closes first: the system back button on Android closes it and a second back leaves the group. On the web the browser's back button does not close a sheet but leaves the group, because the browser history belongs to the screens and a sheet is not one; the Escape key closes a sheet there. The iOS edge swipe on a sheet has not been tried.

The web addresses are the part after the `#`, so the web host needs nothing special. An address that names no screen, the old form with class names, or an admin address opened by someone who is not an administrator shows the first screen; a bottom sheet is never part of an address. Typing an address into the address bar shows its screen without reloading the page.

| Address | Screen |
|---|---|
| `#/groups`, `#/profile` | the Groups and Profile tabs |
| `#/groups/{id}` | a group |
| `#/groups/{id}/gifter`, `#/groups/{id}/receiver` | the Gifter page (your receiver's wishlist and your questions) and the Receiver page (your wishlist and your Santa's questions) |
| `#/admin/groups`, `#/admin/groups/new`, `#/admin/groups/{id}`, `#/admin/groups/{id}/edit` | the administrator's groups, a new group, a group, its details |
| `#/admin/users`, `#/admin/users/{username}` | the users, one user |

Run it with `./kotlin run -m androidApp`, `./kotlin run -m iosApp --platform=iosSimulatorArm64 -d <simulator id>` or `./kotlin run -m webApp` (the web dev server listens on `http://localhost:8080`).

| Platform | Session is kept | Server address |
|----------|-----------------|----------------|
| Android  | Yes, encrypted with a key in the Android Keystore (excluded from backups) | a debug build: `http://10.0.2.2:8080` (the emulator's alias for this machine), `DEVELOPMENT_SERVER` in `ServerAddress.kt`; a release build always `https://santa-api.plm360.org` |
| iOS      | Yes, in the Keychain (this device only)                                   | `http://localhost:8080`, set in `ViewController.kt` |
| Web      | With "Stay signed in" ticked (the default): `localStorage`, so closing and reopening the tab or the browser keeps you signed in until the token (7 days) is no longer accepted; unticked: the tab's `sessionStorage`, so a refresh keeps you signed in and closing the tab signs you out | `baseUrl` in `config.json` next to the app, else the page's own origin |

On the web the token is in `localStorage` ("Stay signed in" ticked on the sign-in screen, the default) or in the tab's `sessionStorage` (unticked), in one of them at a time and never in a cookie; signing out removes it from both. A script running on the page can read it, which is why a shared computer should use the unticked box and sign out; the app draws to a canvas and builds no HTML from what people type, which keeps that exposure small. If the web app is served to people you do not trust to keep scripts off the page, serve it with a Content-Security-Policy; an httpOnly cookie (which the server would have to issue and read) would take the exposure away and is the stronger design for later.

The web app reads `config.json` every time it starts, so one build works against any server. The sample in `app/webApp/resources/config.json` points at `http://localhost:8081`, because the web dev server already uses 8080. When the web app is served from a different origin than the server, list that origin in the server's `CORS_ALLOWED_ORIGINS`:

```
PORT=8081 CORS_ALLOWED_ORIGINS=http://localhost:8080 ... ./kotlin run -m server
```

Plain `http` is only allowed to development hosts: `10.0.2.2` and `localhost` on Android, and local networking on iOS. Any other server must use `https`.

**Android 17 and later** requires the `ACCESS_LOCAL_NETWORK` permission before an app may reach a private-network address, which includes `10.0.2.2` and a server on your home or office network. When the server address is such an address, the app explains this before its first request and shows Android's own "nearby devices" request. If you say no it tells you the server cannot be reached and offers "Try again"; once Android will no longer ask it offers "Open settings" instead. A public `https` server, and any Android before 17, never see the prompt. During development you can skip it, or reset it to test the prompt:

```
adb shell pm grant com.usuallycoding.opensecretsanta android.permission.ACCESS_LOCAL_NETWORK
adb shell pm revoke com.usuallycoding.opensecretsanta android.permission.ACCESS_LOCAL_NETWORK
adb shell pm clear-permission-flags com.usuallycoding.opensecretsanta android.permission.ACCESS_LOCAL_NETWORK user-set user-fixed
```

A physical device needs your machine's LAN address instead of `10.0.2.2`: change `DEVELOPMENT_SERVER` in `app/androidApp/src/ServerAddress.kt` (and allow that address in `app/androidApp/res/xml/network_security_config.xml`). Only debug builds use it.

#### Distribute the Android app to testers

`scripts/distribute-android.sh` builds a signed release APK and sends it to a [Firebase App Distribution](https://firebase.google.com/docs/app-distribution) tester group. It runs on your own machine and nothing of it is stored on GitHub: the key, its passwords and the Firebase files stay on your disk, in places git ignores.

```
scripts/distribute-android.sh --notes "What changed"      # to the group dincolo
scripts/distribute-android.sh --version 1.4.0 --group beta
scripts/distribute-android.sh --no-upload                 # build and sign only, and print the APK's path
```

Every run also leaves the signed APK at `build/distribution/opensecretsanta-<version>-<code>.apk`, with or without `--no-upload`, and prints its path as the last line, so you can send it to someone by hand (it installs like any APK once "install unknown apps" is allowed for the app that opens it). The version name is `--version` (default: `git describe`) and the version code is the minutes since 1970, so every upload is newer than the one before. The notes default to the last commit's subject. The script stops before building when something it needs is missing, says what, and leaves your working tree as it found it (it refuses to start if `app/androidApp/module.yaml` has uncommitted changes, because it edits that file for the build and puts it back). A release build always talks to `https://santa-api.plm360.org`.

Set it up once:

1. **A release key**, which you must back up (an APK signed with another key cannot be installed over this one; testers would have to uninstall first):
   ```
   keytool -genkeypair -v -keystore secrets/android-release.keystore -alias release -keyalg RSA -keysize 4096 -validity 10000
   ```
2. **Its passwords**, in `secrets/android-signing.properties`:
   ```
   storePassword=...
   keyAlias=release
   keyPassword=...
   ```
3. **`app/androidApp/google-services.json`**, downloaded from the Firebase console (Project settings, Your apps, the Android app). Git ignores it, and `secrets/` and any `*.keystore` too.
4. **Firebase access**, either of two:
   - `npx firebase-tools login` once, with a Google account that may use App Distribution in the project (nothing is stored in this repository), or
   - a service-account key in `secrets/firebase-service-account.json`, for an account with the role "Firebase App Distribution Admin"; the script uses it when the file exists.
5. **The tester group**: open the project's App Distribution page, then Testers & Groups. The script wants the group's *alias* (under the group's name, normally the name in lower case, such as `dincolo`), not the display name. Testers get an email invitation the first time.

Node is needed for the upload (`npx firebase-tools`), and the Android SDK build-tools for `apksigner`, which the script finds under `ANDROID_HOME` or the usual SDK folders.

#### The app's icons

The icons of all three platforms are made from one drawing, `assets/secret-santa-icon.svg`, by `scripts/generate-icons.sh`, which writes them in place:

- **Android:** the launcher icons (`ic_launcher.png` rounded and `ic_launcher_round.png`, at the five densities) and the adaptive icon (`drawable-nodpi/ic_launcher_foreground.png` on the solid colour of `drawable/ic_launcher_background.xml`).
- **iOS:** `AppIcon.appiconset/app-icon-1024.png`, 1024×1024 without transparency, as the App Store needs.
- **Web:** `favicon.ico`, `apple-touch-icon.png`, `icon-192.png`, `icon-512.png` and `manifest.webmanifest` in `app/webApp/resources/` (`index.html` links them).

```
scripts/generate-icons.sh                          # from assets/secret-santa-icon.svg, on the app's light theme colour
scripts/generate-icons.sh --background "#8F4C38"   # another background colour
scripts/generate-icons.sh --source other.svg       # another drawing
```

To change the artwork, replace the SVG and run the script, then commit what it wrote: the results are in the repository, so building the apps and the Docker images needs neither the script nor its tools. The script needs `rsvg-convert` and ImageMagick (`brew install librsvg imagemagick`), stops before changing anything when one is missing, and gives the same files when run again. The hat is transparent in the drawing, so the script puts it on the background colour wherever a platform wants an opaque icon. The current SVG holds a bitmap (614×680 pixels) rather than vector paths: it is sharp at the 1024 canvas, and a true vector drawing would allow bigger or crisper icons. The status-bar notification icon (`drawable/ic_notification.xml`) is a plain white shape and is not made by the script.

### Testing

Run the project's tests with `./kotlin test`, or `./kotlin test -m <module>` for a single module.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Kotlin Toolchain](https://kotlin-toolchain.org/dev/), [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/), [Kotlin/Wasm](https://kotl.in/wasm/), [Ktor](https://ktor.io/).

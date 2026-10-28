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
| `DELETE /admin/groups/{id}`            | owner | Delete a Draft                                                       |
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

One Compose Multiplatform UI (`app/shared`) runs on Android, iOS and the web. It signs in, makes a person replace a temporary password, keeps the session, and shows a participant view for everyone plus an admin view for administrators, who switch between the two from the menu at the top right (each switch opens the other view on its first screen).

The participant view has two tabs:

- **Groups** lists the groups you are in (name, state, date and time, location, number of participants) in the order the server returns them. Tap one to see its details and who takes part; your own username is marked. A group shows as Pending until the organiser draws the names, then Active, then Archived. Nothing on these screens says who gives to whom. Draft groups are never shown. On Android and iOS pull a screen down to refresh it, or just come back to the app: the screens reload when the app returns to the foreground. The web app has no refresh control, because the browser's refresh reloads the page and keeps you signed in on the same screen.

  A group's page also has a Wishlists section. **My wishlist** is there from the moment the group is Pending: it lists your items in your order, and "Add item" or a tap on an item opens a bottom sheet to write or change its title, note and link, with a Delete button for an existing item. The arrows on each row move it up or down, and every change is saved at once. Once the names are drawn, **Your receiver's wishlist** appears beside it, read-only and in the order your receiver chose; a link opens in the browser. Once the group is Archived both lists are read-only. A list holds at most 50 items.

  Once the names are drawn the page also has a **Your gift** section. **Your receiver** shows who you give to and whatever postal details they have set (full name, address, phone, delivery information), then the questions you asked them, numbered, each with their answer or "Not answered yet"; "Ask a question" opens a bottom sheet, you can ask up to 30, and a question cannot be changed or deleted. **Questions from your Santa** lists what your Santa asked, with "Answer" or "Change answer" opening a bottom sheet; nothing on it says who the Santa is. When the group is Archived both threads stay readable but nothing changes, and a third entry, **Your Santa**, reveals their username and name.
- **Profile** shows your username and lets you edit your full name, email, phone, address and delivery information. A Santa uses your name and address to post your gift, so the screen asks for them while either is empty. Edits are kept when you switch tabs, and the email and the 500-character limit are checked before anything is sent. The address and the delivery information take several lines: Return starts a new line. Every multi-line field in the app (these two, a wishlist note, a question or an answer, a group description) grows with its text up to six rows and then scrolls inside itself. On a phone the forms lift above the keyboard and scroll, so the Save button can always be reached, and dragging a form closes the keyboard; dragging inside a multi-line field only scrolls its text.

The admin view starts on the administrator's own groups (never another administrator's). **New group** opens a form for name, description, location, date and time; the date is typed as `2026-12-24` and the time as `18:30`, and everything is checked before sending exactly as the server checks it. A group's page shows its state and what it means, its participants (marked "no account yet" or "profile incomplete" where that is so) and what can be done in that state:

| State    | What the page offers                                                                                          |
|----------|---------------------------------------------------------------------------------------------------------------|
| Draft    | Edit details, edit participants, **Make pending**, **Delete**                                                 |
| Pending  | Edit details, edit participants (people added now get an account), **Draw the names** (needs at least 4 people) |
| Active   | Edit details, the roster read-only, **Archive**                                                               |
| Archived | Everything read-only                                                                                          |

Each step asks for a confirmation that says what it does. When a step creates accounts, a dialog shows each new username with its temporary password once, selectable so it can be copied; the people must change it when they first sign in, and the app keeps none of it after the dialog is closed. The **Users** tab (the admin view's second tab) lists the people you manage: everyone in your Pending, Active and Archived groups, and yourself. A user's page shows the profile they have set and, for as long as they have not changed it, their temporary password, selectable so you can pass it on. **Reset password** (not offered on your own account) asks for a confirmation, then shows the new temporary password once; the person's old password and sign-ins stop working and they must change the new one when they next sign in. There is no way to make anyone an administrator: the first administrator comes from `ADMIN_USERNAME` and `ADMIN_PASSWORD` and the server has no promote route.

Owner oversight is read-only and never recorded. A Pending group lists its participants, and **Read wishlist** next to a name opens that person's list in a bottom sheet, in their order, with a link that opens in the browser and when each item was added or last changed. Once the names are drawn (an Active or Archived group) the participants list is replaced by one list of pairs, `santa -> receiver`, in the order the server returns them. Your own side is marked "(you)" even when you take part, and a username whose profile still lacks a name or an address is followed by `*`, with the legend "* profile incomplete" under the list. Choosing a username opens a bottom sheet with that person's details, their temporary password while it is temporary and **Reset password** (not offered on your own name), the same as on the Users tab. Choosing the arrow opens a sheet for the pair: the receiver's wishlist and, below it, the questions the Santa asked them with the receiver's answers, "Not answered yet" where there is none, and when each was asked, answered and last changed. A Draft group offers none of this. If the group or person is gone, or the group has moved to a state where the read no longer applies, the sheet says so and offers to close; if the server cannot be reached each part of a sheet says so with its own "Try again" and keeps what it already showed. The sheets are part of the group's screen, not screens of their own, so the web address stays on the group while one is open.

Back works the platform's way: the system back button or gesture on Android (from the Groups list it leaves the app; from Profile it goes back to Groups first), the edge swipe on iOS and the browser's back button on the web, where the address bar also follows the screens. After a page refresh the web app keeps you signed in and opens the screen named in the address; a new tab or window, or a refresh once the server no longer accepts the session, starts at the sign-in screen. A bottom sheet on a group closes first: the system back button on Android closes it and a second back leaves the group. On the web the browser's back button does not close a sheet but leaves the group, because the browser history belongs to the screens and a sheet is not one; the Escape key closes a sheet there. The iOS edge swipe on a sheet has not been tried.

Run it with `./kotlin run -m androidApp`, `./kotlin run -m iosApp --platform=iosSimulatorArm64 -d <simulator id>` or `./kotlin run -m webApp` (the web dev server listens on `http://localhost:8080`).

| Platform | Session is kept | Server address |
|----------|-----------------|----------------|
| Android  | Yes, encrypted with a key in the Android Keystore (excluded from backups) | `http://10.0.2.2:8080` (the emulator's alias for this machine), set in `OpenSecretSantaApplication.kt` |
| iOS      | Yes, in the Keychain (this device only)                                   | `http://localhost:8080`, set in `ViewController.kt` |
| Web      | For the tab: kept in `sessionStorage`, so a refresh keeps you signed in and closing the tab signs you out | `baseUrl` in `config.json` next to the app, else the page's own origin |

On the web the token is in the tab's `sessionStorage`, never in `localStorage` or a cookie. A script running on the page can read it, which is why it is kept for the tab only; the app draws to a canvas and builds no HTML from what people type, which keeps that exposure small. If the web app is served to people you do not trust to keep scripts off the page, serve it with a Content-Security-Policy; an httpOnly cookie (which the server would have to issue and read) would take the exposure away and is the stronger design for later.

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

A physical device needs your machine's LAN address instead of `10.0.2.2`.

### Testing

Run the project's tests with `./kotlin test`, or `./kotlin test -m <module>` for a single module.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Kotlin Toolchain](https://kotlin-toolchain.org/dev/), [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/), [Kotlin/Wasm](https://kotl.in/wasm/), [Ktor](https://ktor.io/).

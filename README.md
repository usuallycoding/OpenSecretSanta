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

One Compose Multiplatform UI (`app/shared`) runs on Android, iOS and the web. It signs in, makes a person replace a temporary password, keeps the session, and shows a participant view for everyone plus an admin view (with a switch) for administrators. Both views are placeholders until the participant and admin screens arrive in later changes.

Run it with `./kotlin run -m androidApp`, `./kotlin run -m iosApp --platform=iosSimulatorArm64 -d <simulator id>` or `./kotlin run -m webApp` (the web dev server listens on `http://localhost:8080`).

| Platform | Session is kept | Server address |
|----------|-----------------|----------------|
| Android  | Yes, encrypted with a key in the Android Keystore (excluded from backups) | `http://10.0.2.2:8080` (the emulator's alias for this machine), set in `OpenSecretSantaApplication.kt` |
| iOS      | Yes, in the Keychain (this device only)                                   | `http://localhost:8080`, set in `ViewController.kt` |
| Web      | No: the token lives in memory, so a refresh asks for the password again   | `baseUrl` in `config.json` next to the app, else the page's own origin |

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

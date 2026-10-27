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

Questions are numbered from 1 within each Santa's own thread, so the numbers say nothing about other threads. A question cannot be edited or deleted once asked, an answer can be changed as often as you like while the group is Active, and once the group is Archived both threads stay readable but nothing changes. Admins, including the group's owner, have no way to read the questions or answers. Asking in a group that is not Active, answering in one, or asking the 31st question answers `409` with the code `invalid_state`.

A group needs at least 4 participants to be activated. With only 3, everyone could work out who their Santa is from who their receiver is; with 4 a participant's Santa is one of two people.

### Server address used by the apps

Each app host passes the server's base URL to `initKoin` at launch:

| Platform          | Default base URL           | Why                                            |
|-------------------|----------------------------|------------------------------------------------|
| Android emulator  | `http://10.0.2.2:8080`     | The emulator's alias for the host's `localhost` |
| iOS simulator     | `http://localhost:8080`    | The simulator shares the host's network        |
| Web               | the page's own origin      | Expects the server (or a proxy) to serve the app |

Change them in `OpenSecretSantaApplication.kt` (Android), `ViewController.kt` (iOS) and `main.kt` (web). A physical device needs your machine's LAN address instead.

### Testing

Run the project's tests with `./kotlin test`, or `./kotlin test -m <module>` for a single module.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Kotlin Toolchain](https://kotlin-toolchain.org/dev/), [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/), [Kotlin/Wasm](https://kotl.in/wasm/), [Ktor](https://ktor.io/).
